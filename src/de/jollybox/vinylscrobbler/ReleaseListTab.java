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

import de.jollybox.vinylscrobbler.util.DiscogsQuery;
import de.jollybox.vinylscrobbler.util.ReleaseInfo.ReleaseSummary;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.ListView;

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
					
					//ReleasesAdapter adapter = new ReleasesAdapter(releases);
					ReleasesAdapter adapter = new ReleasesAdapter(mContext, ReleaseSummary.fromJSONArray(releases));
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
		ReleaseSummary release = (ReleaseSummary) adapter_view.getItemAtPosition(position);
	
		String type = "release";
		if (release.isMaster()) {
			type = "master";
		}
		int id = release.getId();
		Uri uri = (new Uri.Builder()).scheme("de.jollybox.vinylscrobbler")
									 .authority("discogs")
									 .appendPath(type)
									 .appendPath(Integer.toString(id))
									 .build();
		startActivity(new Intent(Intent.ACTION_VIEW, uri));
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
