/**
 * Vinyl Scrobbler app by JollyBOX.de
 *
 * Copyright (c) 2011	Thomas Jollans
 * 
 * Refer to the file COPYING for copying permissions.
 */

package de.jollybox.vinylscrobbler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class BarcodeActivity extends Activity {
	private boolean mRequestedBarcode = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		try {
			Intent intent = new Intent("com.google.zxing.client.android.SCAN");
	        intent.setPackage("com.google.zxing.client.android");
	        intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
	        startActivityForResult(intent, 0);
	        mRequestedBarcode = true;
		} catch (ActivityNotFoundException exc) {
			showDialog(DIALOG_REQUIRES_ZXING);
		}
	}
	
	private final static int DIALOG_REQUIRES_ZXING = 1;
	private final static int DIALOG_MARKET_ERROR = 2;
	
	@Override
	protected Dialog onCreateDialog(int id) {
		DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		};
		
		switch (id) {
		case DIALOG_REQUIRES_ZXING:
			return (new AlertDialog.Builder(this))
				.setMessage(R.string.requires_zxing)
				.setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					try {
						startActivity(new Intent(Intent.ACTION_VIEW,
							Uri.parse("market://search?q=pname:com.google.zxing.client.android")));
						finish();
					} catch (Exception exc) {
						showDialog(DIALOG_MARKET_ERROR);
					}
				} })
				.setNegativeButton("Cancel", cancelListener)
				.create();
		case DIALOG_MARKET_ERROR:
			return (new AlertDialog.Builder(this))
				.setMessage(R.string.error_market)
				.setNeutralButton(R.string.ok, cancelListener)
				.create();
		default:
			return null;
		}
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data != null && requestCode == 0) {
			if (resultCode == RESULT_OK) {
				String contents = data.getStringExtra("SCAN_RESULT");
				Intent searchIntent = new Intent(this, SearchScreen.class);
				searchIntent.putExtra(SearchManager.QUERY, contents);
				searchIntent.putExtra("REDIRECT", true);
				startActivity(searchIntent);
			}
			finish();
		} else if (mRequestedBarcode) {
			finish();
		}
		super.onActivityResult(requestCode, resultCode, data);
	};
}
