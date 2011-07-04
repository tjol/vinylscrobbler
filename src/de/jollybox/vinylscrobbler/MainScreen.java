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

import org.apache.http.client.methods.HttpGet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainScreen extends Activity {
	
	private static final int DIALOG_FIRSTRUN = 1;
	private static final int DIALOG_UPDATE = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		Button search = (Button)findViewById(R.id.main_search);
		search.setOnClickListener(mSearchClickListener);
		
		Button barcode = (Button)findViewById(R.id.main_barcode);		
		barcode.setOnClickListener(mBarcodeClickListener);
		
		Button settings = (Button)findViewById(R.id.main_settings);
		settings.setOnClickListener(mSettingsClickListener);
		
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		boolean isFirstRun = prefs.getBoolean("firstrun", true);
		if (isFirstRun) {
			showDialog(DIALOG_FIRSTRUN);
			prefs.edit().putBoolean("firstrun", false).commit();
		}
		
		if (checkForUpdate()) {
			showDialog(DIALOG_UPDATE);
		}
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
	
	private OnClickListener mSearchClickListener = new OnClickListener() {
		public void onClick(View v) {
			onSearchRequested();
		}
	};
	
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
		default:
			return false;
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
		return  handleMenuEvent(this, item) || super.onOptionsItemSelected(item);
	}
	
	private static void doBarcodeScan (final Activity a) {
		a.startActivity(new Intent(a, BarcodeActivity.class));
	}
	
	private OnClickListener mBarcodeClickListener = new OnClickListener() {
		public void onClick(View v) {
			doBarcodeScan(MainScreen.this);
		}
	};
	
	private OnClickListener mSettingsClickListener = new OnClickListener() {
		
		public void onClick(View v) {
			startActivity(new Intent(MainScreen.this, SettingsScreen.class));
		}
	};
	
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
