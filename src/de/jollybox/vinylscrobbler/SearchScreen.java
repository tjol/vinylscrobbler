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

import de.jollybox.vinylscrobbler.util.DiscogsQuery;
import de.jollybox.vinylscrobbler.util.ImageDownloader;

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchManager.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class SearchScreen extends Activity
						  implements OnItemClickListener, OnDismissListener {
	protected SearchResultsAdapter mResultsAdapter;
	protected ListView mList;
	protected EditText mQuery;
	protected ImageDownloader mDownloader;
	protected boolean mRedirect = false;
	protected boolean mHaveSearched = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		
		Intent intent = getIntent();
		mRedirect = intent.getBooleanExtra("REDIRECT", false);

		mList = (ListView) findViewById(R.id.results_list);
		mList.setVerticalFadingEdgeEnabled(true);
		
		mDownloader = new ImageDownloader(this);
		mQuery = (EditText) findViewById(R.id.search_query); 
		mQuery.setOnClickListener(mSearchClickListener);
		mQuery.setFocusable(false);
		
		SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
		searchManager.setOnDismissListener(this);
	
		String query = intent.getStringExtra(SearchManager.QUERY);
		if (query != null) {
			mQuery.setText(query);
			search(query);
		} else {
			onSearchRequested();
		}
	}
	
	private OnClickListener mSearchClickListener = new OnClickListener() {
		
		public void onClick(View v) {
			onSearchRequested();
		}
	};
	
	public void onDismiss() { // search dialog.
		if (!mHaveSearched) finish();
		else mQuery.setVisibility(View.VISIBLE);
	};
	
	public boolean onSearchRequested() {
		mQuery.setVisibility(View.INVISIBLE);
		
		return super.onSearchRequested();
	}
	
	public void search (String quest) {
		mHaveSearched = true;
		String query_string = "/search?q=" + Uri.encode(quest);
		
		DiscogsQuery q = new DiscogsQuery.WithAlertDialog(SearchScreen.this, false) {
			@Override
			protected void onResult(JSONObject result) {
				JSONArray result_array, result_array_2;
				try {
					JSONObject resp = result.getJSONObject("resp");
					if (resp.getBoolean("status") != true) {
						errorMessage(res.getString(R.string.no_search_results));
						return;
					}
					JSONObject search = resp.getJSONObject("search");
					if (search.has("exactresults")) {
						result_array = search.getJSONArray("exactresults");
						result_array_2 = search.getJSONObject("searchresults")
										       .getJSONArray("results");
						
						int extralen = result_array_2.length();
						
						for (int i = 0; i < extralen; ++i) {
							result_array.put(result_array_2.get(i));
						}
					} else {
						result_array = search.getJSONObject("searchresults").getJSONArray("results");
					}
					
				} catch (Exception exc) {
					errorMessage(res.getString(R.string.error_invalid_data));
					return;
				}
				
				if (result_array.length() == 1 && mRedirect) {
					goToResult(result_array.optJSONObject(0));
					finish();
				}

				mResultsAdapter = new SearchResultsAdapter(result_array);
				mList.setAdapter(mResultsAdapter);
				mList.setOnItemClickListener(SearchScreen.this);
				
			}
		};
		
		q.execute(query_string);
		
		/*
		InputMethodManager imm = (InputMethodManager) getSystemService(
			    INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(findViewById(R.id.search_query).getWindowToken(), 0);
		*/		
	}

	public void onItemClick(android.widget.AdapterView<?> av, View view, int pos, long id) {
		goToResult((JSONObject) av.getItemAtPosition(pos));
	}
	
	public void goToResult (JSONObject result) {
		try {
			String type = result.getString("type");
			Uri http_url = Uri.parse(result.getString("uri"));
			Uri.Builder internal_uri_builder = new Uri.Builder();
			internal_uri_builder.scheme("de.jollybox.vinylscrobbler");
			internal_uri_builder.authority("discogs");
			
			if (type.equals("artist")) {
				internal_uri_builder.encodedPath(http_url.getEncodedPath());
			} else if (type.equals("master") || type.equals("release")) {
				List<String> pSegsIn = http_url.getPathSegments();
				internal_uri_builder.appendPath(pSegsIn.get(1)); // master | release
				internal_uri_builder.appendPath(pSegsIn.get(2)); // ID
			} else {
				return; // Not implemented. [possibly TODO]
			}
			
			Intent intent = new Intent(Intent.ACTION_VIEW, internal_uri_builder.build());
			startActivity(intent);
		} catch (JSONException json_exc) {
			// nothing happens.
		}
	}
	
	class SearchResultsAdapter extends BaseAdapter {
		private final JSONArray mResults;

		public SearchResultsAdapter(JSONArray results) {
			mResults = results;
		}

		public int getCount() {
			return mResults.length();
		}

		public Object getItem(int pos) {
			try {
				return mResults.getJSONObject(pos);
			} catch (JSONException exc) {
				return null;
			}
		}

		public long getItemId(int pos) {
			try {
				return mResults.getJSONObject(pos).getString("uri").hashCode();
			} catch (JSONException exc) {
				return -1;
			}
		}

		public View getView(int pos, View view, ViewGroup parent) {
			if (view == null || view.getId() != R.layout.discogs_item) {
				LayoutInflater inflater = getLayoutInflater();
				view = inflater.inflate(R.layout.discogs_item, parent, false);
			}
			TextView title = (TextView) view.findViewById(R.id.item_title);
			TextView info1 = (TextView) view.findViewById(R.id.item_info1);
			TextView info2 = (TextView) view.findViewById(R.id.item_info2);
			ImageView img = (ImageView) view.findViewById(R.id.item_image);
			try {
				JSONObject result = mResults.getJSONObject(pos);
				title.setText(result.getString("title"));
				String type = result.getString("type");
				if (type.equals("artist")) {
					info1.setText("Artist");
				} else if (type.equals("master")) {
					info1.setText("Master release");
				} else if (type.equals("release")) {
					info1.setText("Release");
				} else if (type.equals("label")) {
					info1.setText("Record label");
				}
				if (result.has("summary")) {
					info2.setText(result.getString("summary"));
				} else {
					info2.setText("");
				}
				img.setImageBitmap(null);
				if (result.has("thumb")) {
					mDownloader.getBitmap(result.getString("thumb"), img);
				}
			} catch (JSONException exc) {
				title.setText("Error");
			}
			return view;
		}

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return  MainScreen.handleMenuEvent(this, item) || super.onOptionsItemSelected(item);
	}
	
}
