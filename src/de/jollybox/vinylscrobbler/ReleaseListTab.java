/**
 * Vinyl Scrobbler app by JollyBOX.de
 *
 * Copyright (c) 2011	Thomas Jollans
 * 
 * Refer to the file COPYING for copying permissions.
 */

package de.jollybox.vinylscrobbler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ReleaseListTab extends Activity 
							implements OnItemClickListener {
	
	protected ListView mList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mList = new ListView(this);
		setContentView(mList);
		
		Intent intent = getIntent();
		Uri target = intent.getData();
		String query_string;
		
		if (target.getPathSegments().get(0).equals("artist")) {
			query_string = target.getEncodedPath() + "?releases=1";
		} else { // master release
			query_string = target.getEncodedPath();
		}
		
		DiscogsQuery query = new DiscogsQuery.WithAlertDialog(this) {
			@Override
			protected void onResult(JSONObject result) {
				try {
					JSONArray releases;
					JSONObject resp = result.getJSONObject("resp");
					if (resp.has("artist")) {
						releases = resp.getJSONObject("artist")
									   .getJSONArray("releases");
					} else {
						releases = resp.getJSONObject("master")
						   			   .getJSONArray("versions");
					}
					
					ReleasesAdapter adapter = new ReleasesAdapter(releases);
					mList.setAdapter(adapter);
					mList.setOnItemClickListener(ReleaseListTab.this);
				} catch (JSONException json_exc) {
					errorMessage("Cannot comprehend data");
				}
			}
		};
		
		query.execute(query_string);
	}
	
	public void onItemClick(AdapterView<?> adapter_view, View item_view, int position, long hash) {
		JSONObject release = (JSONObject) adapter_view.getItemAtPosition(position);
		
		try {
			String type = release.optString("type", "release");
			String id = release.getString("id"); // It's an int, but org.json can cope.
			Uri uri = (new Uri.Builder()).scheme("de.jollybox.vinylscrobbler")
										 .authority("discogs")
										 .appendPath(type)
										 .appendPath(id)
										 .build();
			startActivity(new Intent(Intent.ACTION_VIEW, uri));
			
		} catch (JSONException json_exc) {
			// TODO: error message.
		}
	}
	
	protected class ReleasesAdapter extends BaseAdapter {
		
		private final JSONArray mReleases;
		private final ImageDownloader mDownloader;
		
		public ReleasesAdapter(JSONArray releases) {
			mReleases = releases;
			mDownloader = new ImageDownloader(ReleaseListTab.this);
		}

		public int getCount() {
			return mReleases.length();
		}

		public Object getItem(int position) {
			return mReleases.opt(position);
		}

		public long getItemId(int position) {
			return mReleases.opt(position).hashCode();
		}

		public View getView(int position, View view, ViewGroup parent) {
			if (view == null) {
				view = getLayoutInflater().inflate(R.layout.discogs_item, parent, false);
			}
			TextView title = (TextView) view.findViewById(R.id.item_title);
			TextView info1 = (TextView) view.findViewById(R.id.item_info1);
			TextView info2 = (TextView) view.findViewById(R.id.item_info2);
			ImageView img = (ImageView) view.findViewById(R.id.item_image);
			try {
				JSONObject release = mReleases.getJSONObject(position);
				title.setText(release.getString("title"));
				if (release.optString("type").equals("master")) {
					info1.setText("Master release");
					info2.setText("");
				} else {
					if (release.has("format")) info1.setText(release.getString("format"));
					if (release.has("label") && release.has("country"))
						info2.setText(release.getString("label") + " — " + release.getString("country"));
					else if (release.has("label"))
						info2.setText(release.getString("label"));
					else if (release.has("country"))
						info2.setText(release.getString("country"));
				}
				img.setImageBitmap(null);
				if (release.has("thumb")) {
					mDownloader.getBitmap(release.getString("thumb"), img);
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
