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
import android.widget.ListView;
import de.jollybox.vinylscrobbler.util.Discogs;
import de.jollybox.vinylscrobbler.util.DiscogsQuery;
import de.jollybox.vinylscrobbler.util.ReleaseInfo.ReleaseSummary;
import de.jollybox.vinylscrobbler.util.VinylDatabase;

public class CollectionScreen extends Activity {
	private ListView mList;
	private EditText mQuery;
	private Discogs mDiscogs;
	private ReleasesAdapter mReleases;
	private VinylDatabase mCollection;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.collection);

		mList = (ListView) findViewById(R.id.results_list);
		mList.setVerticalFadingEdgeEnabled(true);

		mDiscogs = new Discogs(this);
		mCollection = new VinylDatabase(this);
		mQuery = (EditText) findViewById(R.id.search_query);
		mQuery.setOnClickListener(mSearchClickListener);
		mQuery.setFocusable(false);
		mQuery.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				mReleases.ApplyFilter(mQuery.getText().toString());
			}
		});
		List<ReleaseSummary> releases = mCollection.getDiscogsCollection();
		if (releases.size() != 0) {
			mReleases = new ReleasesAdapter(CollectionScreen.this, releases);
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
		final String query_string = "/users/" + mDiscogs.getUser()
				+ "/collection/folders/0/releases?per_page=100&page=" + page;
		DiscogsQuery query = new DiscogsQuery.WithAlertDialog(this, false,
				mDiscogs) {
			@Override
			protected void onResult(JSONObject result) {
				try {
					// check if we get an authentication error (key remotely
					// revoked)
					if (result.has("message")) {
						if (result.getString("message")
								.contains("authenticate")) {
							// the current discogs token is invalid, clear
							// discogs session and remove the query from the
							// cache so after login we get the correct results
							mDiscogs.forgetSession();
							removeFromCache(query_string);
							errorMessage(res
									.getString(R.string.discogs_nologin));
							startActivity(new Intent(CollectionScreen.this,
									SettingsScreen.class));
							return;
						}
					}
					JSONArray releases = concatArray(currResults,
							result.getJSONArray("releases"));
					// check for pagination
					int totalPages = result.getJSONObject("pagination").getInt(
							"pages");
					if (page < totalPages) {
						getCollection(releases, page + 1);
					} else {
						// finalise the collection list if the last page has
						// been read and update the db
						mCollection.updateDiscogsCollection(ReleaseSummary
								.fromJSONArray(releases));
						// notify the discogs manager that the local db is synced with discogs
						mDiscogs.saveDiscogsState();
						// reread the db to present the correct collection incl
						// already stored thumbs TODO: could be more efficient,
						// no?
						mReleases = new ReleasesAdapter(CollectionScreen.this,
								mCollection.getDiscogsCollection());
						mList.setAdapter(mReleases);
						mList.setOnItemClickListener(new ReleasesAdapter.ReleaseOpener(CollectionScreen.this));
					}
				} catch (JSONException json_exc) {
					errorMessage("Cannot comprehend data");
					// remove the current query from the cache since it has
					// (temporary?) faulty results
					removeFromCache(query_string);
				}
			}
		};
		query.execute(query_string);
	}

	// so we can concatenate the paginated collection results
	private JSONArray concatArray(JSONArray arr1, JSONArray arr2)
			throws JSONException {
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
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return MainScreen.handleMenuEvent(this, item)
				|| super.onOptionsItemSelected(item);
	}

}
