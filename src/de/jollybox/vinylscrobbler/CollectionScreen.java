/**
 * Vinyl Scrobbler app by JollyBOX.de
 *
 * Copyright (c) 2011	Thomas Jollans
 * 
 * Refer to the file COPYING for copying permissions.
 */

package de.jollybox.vinylscrobbler;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import de.jollybox.vinylscrobbler.util.Discogs;
import de.jollybox.vinylscrobbler.util.DiscogsQuery;
import de.jollybox.vinylscrobbler.util.ReleaseInfo.ReleaseSummary;
import de.jollybox.vinylscrobbler.util.VinylDatabase;

public class CollectionScreen extends Activity {
	private final static int GRID_MENU_ITEM = 1;
	private final static int LIST_MENU_ITEM = 2;
	
	private ListView mList;
	private GridView mGrid;
	private EditText mQuery;
	private Discogs mDiscogs;
	private ReleasesAdapter mReleases;
	
	private boolean showGridView;

	// private VinylDatabase mCollection;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		showGridView = getIntent().getBooleanExtra("GRIDVIEW", false);
		
		if(showGridView) {
			setContentView(R.layout.collection_grid);
			mGrid = (GridView) findViewById(R.id.results_grid);
			mGrid.setVerticalFadingEdgeEnabled(true);
		} else {
			setContentView(R.layout.collection);
			mList = (ListView) findViewById(R.id.results_list);
			mList.setVerticalFadingEdgeEnabled(true);
		}

		
		mDiscogs = new Discogs(this);
		mQuery = (EditText) findViewById(R.id.search_query);
		mQuery.setOnClickListener(mSearchClickListener);
		mQuery.setFocusable(false);
		mQuery.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before, int count) {
				mReleases.ApplyFilter(mQuery.getText().toString());
			}
		});

		try {
			// check if the discogs collection is cached
			if (mDiscogs.isCacheCollection()) {
				List<ReleaseSummary> releases = VinylDatabase.getInstance(this).getDiscogsCollection();
				if (releases.size() != 0) {
					//show the local collection
					mReleases = new ReleasesAdapter(CollectionScreen.this, releases);
					//switch between grid and list
					if(showGridView) {
						mGrid.setAdapter(mReleases);
						mGrid.setOnItemClickListener(new ReleasesAdapter.ReleaseOpener(CollectionScreen.this));
					} else {
						mList.setAdapter(mReleases);
						mList.setOnItemClickListener(new ReleasesAdapter.ReleaseOpener(CollectionScreen.this));
					}

					// check if the local collection is still valid
					mDiscogs.onCollectionChanged(new Discogs.ResultWaiter() {
						@Override
						public void onResult(Boolean result) {
							if (result) {
								// the discogs collection has changed, fetch it
								getCollection(new JSONArray(), 1);
							}
						}
					});
				} else {
					//no local releases available, directly go to discogs
					getCollection(new JSONArray(), 1);
				}
			} else {
				// no caching, just fetch the collection
				getCollection(new JSONArray(), 1);
			}
		} catch (Exception e) {
			//catch exceptions since this part is heavily threaded, could fail if thread pool is full
		}
	}

	private OnClickListener mSearchClickListener = new OnClickListener() {

		public void onClick(View v) {
			mQuery.setFocusableInTouchMode(true);
			mQuery.requestFocus();
			InputMethodManager m = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			if (m != null) {
				m.toggleSoftInput(0, InputMethodManager.SHOW_IMPLICIT);
			}
		}
	};

	private void getCollection(final JSONArray currResults, final int page) {
		final String query_string = "/users/" + mDiscogs.getUser() + "/collection/folders/0/releases?sort=artist&per_page=100&page=" + page;
		// cache this query only if we don't have local caching enabled, else this is only called when there are updates, and we might miss them
		DiscogsQuery query = new DiscogsQuery.WithAlertDialog(this, !mDiscogs.isCacheCollection(), mDiscogs) {
			@Override
			protected void onResult(JSONObject result) {
				try {
					// check if we get an authentication error (key remotely revoked)
					if (result.has("message")) {
						if (result.getString("message").contains("authenticate")) {
							// the current discogs token is invalid, clear discogs session, remove the query from cache and go to settings
							mDiscogs.forgetSession();
							errorMessage(res.getString(R.string.discogs_nologin));
							removeFromCache(query_string);
							startActivity(new Intent(CollectionScreen.this, SettingsScreen.class));
							finish();
							return;
						}
					}
					JSONArray releases = concatArray(currResults, result.getJSONArray("releases"));
					// check for pagination
					int totalPages = result.getJSONObject("pagination").getInt("pages");
					if (page < totalPages) {
						getCollection(releases, page + 1);
					} else {
						List<ReleaseSummary> collection = null;
						if (mDiscogs.isCacheCollection()) {
							// finalise the collection list if the last page has been read and update the db
							VinylDatabase.getInstance(CollectionScreen.this).updateDiscogsCollection(ReleaseSummary.fromCollectionJSONArray(releases));
							// notify the discogs manager that the local db is synced with discogs
							mDiscogs.saveCollectionState();
							// reread the db to present the correct collection
							collection = VinylDatabase.getInstance(CollectionScreen.this).getDiscogsCollection();
						} else {
							collection = ReleaseSummary.fromCollectionJSONArray(releases);
						}
						if (collection != null) {
							mReleases = new ReleasesAdapter(CollectionScreen.this, collection);
							if(showGridView) {
								mGrid.setAdapter(mReleases);
								mGrid.setOnItemClickListener(new ReleasesAdapter.ReleaseOpener(CollectionScreen.this));
							} else {
								mList.setAdapter(mReleases);
								mList.setOnItemClickListener(new ReleasesAdapter.ReleaseOpener(CollectionScreen.this));
							}
						}
					}
				} catch (JSONException json_exc) {
					removeFromCache(query_string);
					errorMessage("Cannot comprehend data");
				}
			}
		};
		query.execute(query_string);
	}

	// so we can concatenate the paginated collection results
	private JSONArray concatArray(JSONArray arr1, JSONArray arr2) throws JSONException {
		JSONArray result = new JSONArray();
		for (int i = 0; i < arr1.length(); i++) {
			result.put(arr1.get(i));
		}
		for (int i = 0; i < arr2.length(); i++) {
			result.put(arr2.get(i));
		}
		return result;
	}

	public boolean onSearchRequested() {
		startActivity(new Intent(this, SearchScreen.class));
		return super.onSearchRequested();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		if(showGridView) {
			menu.add(Menu.NONE,LIST_MENU_ITEM,Menu.NONE,R.string.collection_list);
		} else {
			menu.add(Menu.NONE,GRID_MENU_ITEM,Menu.NONE,R.string.collection_grid);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//logic for switching between grid and list view
		if(item.getItemId() == GRID_MENU_ITEM) {
			rememberView(GRID_MENU_ITEM);
			Intent collectionIntent = new Intent(this, CollectionScreen.class);
			collectionIntent.putExtra("GRIDVIEW", true);
			startActivity(collectionIntent);
			finish();
			return true;
		} else if (item.getItemId() == LIST_MENU_ITEM) {
			rememberView(LIST_MENU_ITEM);
			Intent collectionIntent = new Intent(this, CollectionScreen.class);
			collectionIntent.putExtra("GRIDVIEW", false);
			startActivity(collectionIntent);
			finish();
			return true;
		}
		return MainScreen.handleMenuEvent(this, item) || super.onOptionsItemSelected(item);
	}
	
	private void rememberView(int viewtype) {
		getSharedPreferences("de.jollybox.vinylscrobbler.settings",MODE_PRIVATE).edit().putBoolean("collection_gridview", viewtype == GRID_MENU_ITEM).commit();
	}

}
