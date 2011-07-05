/**
 * Vinyl Scrobbler app by JollyBOX.de
 *
 * Copyright (c) 2011	Thomas Jollans
 * 
 * Refer to the file COPYING for copying permissions.
 */

package de.jollybox.vinylscrobbler;

import java.util.Map;

import de.jollybox.vinylscrobbler.util.Helper;
import de.jollybox.vinylscrobbler.util.Lastfm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SettingsScreen extends Activity
							implements Lastfm.ErrorHandler {
	private final int DIALOG_LOGIN = 1;
	
	private Lastfm mLastfm;
	
	private TextView mLoggedIn;
	private Button mLoginNow;
	private Button mLogout;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.settings);
		
		mLastfm = new Lastfm(this);
		
		mLoggedIn = (TextView) findViewById(R.id.logged_in);
		mLoginNow = (Button) findViewById(R.id.login_now);
		mLogout = (Button) findViewById(R.id.logout);
		mLoggedIn.setVisibility(View.GONE);
		mLoginNow.setVisibility(View.GONE);
		mLogout.setVisibility(View.GONE);
		
		showCorrectControl();
	}
	
	public void errorMessage(String message) {
		(new AlertDialog.Builder(this))
			.setMessage(message)
			.setNeutralButton("OK", null).show();
	}
	
	private void showCorrectControl() {
		String user;
		if ((user = mLastfm.getUser()) != null) {
			mLoggedIn.setText(Html.fromHtml(String.format(
					getResources().getString(R.string.loggedin_as),
					user)));
			mLoggedIn.setVisibility(View.VISIBLE);
			mLoginNow.setVisibility(View.GONE);
			mLogout.setVisibility(View.VISIBLE);
			mLogout.setOnClickListener(mOnLogoutClickListener);
		} else {
			mLoginNow.setOnClickListener(mOnLoginClickListener);
			mLogout.setVisibility(View.GONE);
			mLoggedIn.setVisibility(View.GONE);
			mLoginNow.setVisibility(View.VISIBLE);
		}
	}
	
	private OnClickListener mOnLoginClickListener = new OnClickListener() {
		
		public void onClick(View v) {
			showDialog(DIALOG_LOGIN);
		}
	};
	
	private OnClickListener mOnLogoutClickListener = new OnClickListener() {
		
		public void onClick(View v) {
			mLastfm.forgetSession();
		}
	};
	
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_LOGIN:
			final View login_wd = getLayoutInflater().inflate(R.layout.lastfm_login_dialog, null);
			return (new AlertDialog.Builder(this))
						.setView(login_wd)
						.setPositiveButton(R.string.login, new Dialog.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								EditText username = (EditText)login_wd.findViewById(R.id.login_username);
								EditText password = (EditText)login_wd.findViewById(R.id.login_password);
								
								String sPassword = password.getText().toString();
								String sUsername = username.getText().toString();
								
								String authToken = Helper.hexMD5(sUsername + Helper.hexMD5(sPassword));
								
								mLastfm.doGetMobileSession(sUsername, authToken, new Lastfm.ResultWaiter() {
									public void onResult(Map<String, String> result) {
										showCorrectControl();
									}
								}, SettingsScreen.this);
								
							}
						})
						.setNegativeButton(android.R.string.cancel, null)
						.create();
		default:
			return null;
		}
	}
}
