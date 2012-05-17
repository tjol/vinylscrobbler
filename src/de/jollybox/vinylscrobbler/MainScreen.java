/**
 * Vinyl Scrobbler app by JollyBOX.de
 *
 * Copyright (c) 2011	Thomas Jollans
 * 
 * Refer to the file COPYING for copying permissions.
 */

package de.jollybox.vinylscrobbler;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.client.methods.HttpGet;

import de.jollybox.vinylscrobbler.util.Helper;
import de.jollybox.vinylscrobbler.util.HistoryDatabase;
import de.jollybox.vinylscrobbler.util.ReleaseInfo.ReleaseSummary;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainScreen extends ListActivity {
	
	private static final int DIALOG_FIRSTRUN = 1;
	private static final int DIALOG_UPDATE = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		populateList(true);
		
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		boolean isFirstRun = prefs.getBoolean("firstrun", true);
		if (isFirstRun) {
			showDialog(DIALOG_FIRSTRUN);
			prefs.edit().putBoolean("firstrun", false).commit();
		}
		
		(new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {
				return checkForUpdate();
			}
			@Override
			protected void onPostExecute(Boolean result) {
				super.onPostExecute(result);
				if (result) {
					showDialog(DIALOG_UPDATE);
				}
			}
		}).execute();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_FIRSTRUN:
			return (new AlertDialog.Builder(this))
						.setMessage(R.string.firstrun_settings_info)
						.setPositiveButton(R.string.go_to_settings, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startActivity(new Intent(MainScreen.this, SettingsScreen.class));						
							}
						})
						.setNegativeButton(android.R.string.cancel, null)
						.create();
		case DIALOG_UPDATE:
			return (new AlertDialog.Builder(this))
						.setMessage(R.string.new_update)
						.setPositiveButton(R.string.download_update, new DialogInterface.OnClickListener() {							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
											  "http://code.jollybox.de/pub/vinylscrobbler/VinylScrobbler-latest.apk")));
							}
						})
						.setNegativeButton(R.string.ignore_update, null)
						.create();
		default:
			return super.onCreateDialog(id);
		}
	}
	
	@Override
	protected void onResume() {
		populateList(false);
		super.onResume();
	}
	
	protected void populateList(boolean create) {
		HistoryDatabase history = new HistoryDatabase(this);
		List<ReleaseSummary> recentReleases = history.getRecentReleases();
		ListAdapter releaseAdapter = new ReleasesAdapter(this, recentReleases);
		
		ListView list = getListView();
		
		if (create) {
			final View vBarcode = getLayoutInflater().inflate(R.layout.list_command, list, false);
			((TextView)vBarcode.findViewById(R.id.text)).setText(R.string.main_barcode);
			((ImageView)vBarcode.findViewById(R.id.icon)).setImageResource(R.drawable.ic_menu_barcode);
			vBarcode.setClickable(false);
			
			final View vSearch = getLayoutInflater().inflate(R.layout.list_command, list, false);
			((TextView)vSearch.findViewById(R.id.text)).setText(R.string.main_search);
			((ImageView)vSearch.findViewById(R.id.icon)).setImageResource(R.drawable.ic_menu_search);
			vSearch.setClickable(false);
			
			final View vSettings = getLayoutInflater().inflate(R.layout.list_command, list, false);
			((TextView)vSettings.findViewById(R.id.text)).setText(R.string.main_settings);
			((ImageView)vSettings.findViewById(R.id.icon)).setImageResource(R.drawable.ic_menu_preferences);
			vSettings.setClickable(false);
			
			list.setDividerHeight(1);
			list.setVerticalFadingEdgeEnabled(true);
			
			list.addHeaderView(vBarcode);
			list.addHeaderView(vSearch);
			list.addHeaderView(vSettings);
			
			final OnItemClickListener releaseClickListener = new ReleasesAdapter.ReleaseOpener(this);
			
			list.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(android.widget.AdapterView<?> av, View v, int pos, long id) {
					if (v == vBarcode) {
						doBarcodeScan(MainScreen.this);
					} else if (v == vSearch) {
						onSearchRequested();
					} else if (v == vSettings) {
						startActivity(new Intent(MainScreen.this, SettingsScreen.class));
					} else {
						releaseClickListener.onItemClick(av, v, pos, id);
					}
				};
			});
		}
		
		setListAdapter(releaseAdapter);
	}
	
	public boolean onSearchRequested() {
		
		startActivity(new Intent(MainScreen.this, SearchScreen.class));
		return true;
	};
	
	public static boolean handleMenuEvent (Activity a, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_search:
			a.onSearchRequested();
			return true;
		case R.id.item_scan:
			doBarcodeScan(a);
			return true;
		case R.id.item_settings:
			a.startActivity(new Intent(a, SettingsScreen.class));
			return true;
		case R.id.item_recent:
			a.startActivity(new Intent(a, MainScreen.class));
			return true;
		default:
			return false;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		menu.findItem(R.id.item_recent).setVisible(false);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return  handleMenuEvent(this, item) || super.onOptionsItemSelected(item);
	}
	
	private static void doBarcodeScan (final Activity a) {
		a.startActivity(new Intent(a, BarcodeActivity.class));
	}
	
	private boolean checkForUpdate() {
		try {
			PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			int myVersionCode = pinfo.versionCode;
			
			
			InputStream versionInfoStream =
				Helper.doRequest(this, new HttpGet("http://code.jollybox.de/pub/vinylscrobbler/version"));
			
			String currentVersionString = new BufferedReader(
					new InputStreamReader(versionInfoStream)).readLine();
			
			int currentVersionCode = Integer.parseInt(currentVersionString);
			
			if (currentVersionCode > myVersionCode)
				return true;
			else
				return false;
			
		} catch (Exception e) {
			return false;
		}
	}
}
