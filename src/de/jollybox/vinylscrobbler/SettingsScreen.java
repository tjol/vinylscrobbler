/**
 * Vinyl Scrobbler app by JollyBOX.de
 *
 * Copyright (c) 2011	Thomas Jollans
 * 
 * Refer to the file COPYING for copying permissions.
 */

package de.jollybox.vinylscrobbler;

import java.util.Map;

import de.jollybox.vinylscrobbler.util.Discogs;
import de.jollybox.vinylscrobbler.util.Helper;
import de.jollybox.vinylscrobbler.util.Lastfm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

public class SettingsScreen extends Activity implements Lastfm.ErrorHandler {
	public static final String PREFS_FILE_NAME = "de.jollybox.vinylscrobbler.settings";
	public static final String PREF_N_RECENT = "nRecent";
	public static final int DEFAULT_N_RECENT = 10;
	private SharedPreferences mPrefs;

	private static final int DIALOG_LOGIN = 1;
	private static final int DISCOGS_LOGIN_REQUEST_CODE = 0;

	private Lastfm mLastfm;
	private Discogs mDiscogs;

	private EditText mNoOfRecent;
	private TextView mLoggedIn;
	private Button mLoginNow;
	private Button mLogout;
	private TextView mDiscogsLoggedIn;
	private Button mDiscogsLogin;
	private Button mDiscogsLogout;
	private CheckBox mDiscogsAutoadd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.settings);

		mLastfm = new Lastfm(this);
		mDiscogs = new Discogs(this);
		mPrefs = getPrefs(this);
		int nRecent = mPrefs.getInt(PREF_N_RECENT, 0);
		
		mNoOfRecent = (EditText) findViewById(R.id.no_of_recent);
		mLoggedIn = (TextView) findViewById(R.id.logged_in);
		mLoginNow = (Button) findViewById(R.id.login_now);
		mLogout = (Button) findViewById(R.id.logout);
		mDiscogsLoggedIn = (TextView) findViewById(R.id.Discogs_Logged_in);
		mDiscogsAutoadd = (CheckBox) findViewById(R.id.Discogs_Autoadd);
		mDiscogsLogin = (Button) findViewById(R.id.Discogs_Login_now);
		mDiscogsLogout = (Button) findViewById(R.id.Discogs_Logout);
		mLoggedIn.setVisibility(View.GONE);
		mLoginNow.setVisibility(View.GONE);
		mLogout.setVisibility(View.GONE);
		mDiscogsLoggedIn.setVisibility(View.GONE);
		mDiscogsAutoadd.setVisibility(View.GONE);
		mDiscogsLogin.setVisibility(View.GONE);
		mDiscogsLogout.setVisibility(View.GONE);
		
		mDiscogsAutoadd.setChecked(mDiscogs.isAutoadd());
		mNoOfRecent.setText(Integer.toString(nRecent));
		mNoOfRecent.addTextChangedListener(mNoOfRecentWatcher);

		showCorrectControl();
	}

	public static SharedPreferences getPrefs(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE_NAME,
				MODE_PRIVATE);
		int nRecent = prefs.getInt(PREF_N_RECENT, -1);
		if (nRecent < 0) {
			prefs.edit().putInt(PREF_N_RECENT, DEFAULT_N_RECENT).commit();
		}
		return prefs;
	}

	public void errorMessage(String message) {
		(new AlertDialog.Builder(this)).setMessage(message)
				.setNeutralButton("OK", null).show();
	}

	private void showCorrectControl() {
		String lastfmUser;
		if ((lastfmUser = mLastfm.getUser()) != null) {
			mLoggedIn.setText(Html.fromHtml(String.format(getResources()
					.getString(R.string.loggedin_as), lastfmUser)));
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
		String discogsUser;
		if ((discogsUser = mDiscogs.getUser()) != null) {
			mDiscogsLoggedIn.setText(Html.fromHtml(String.format(getResources()
					.getString(R.string.loggedin_as), discogsUser)));
			mDiscogsLoggedIn.setVisibility(View.VISIBLE);
			mDiscogsLogin.setVisibility(View.GONE);
			mDiscogsAutoadd.setVisibility(View.VISIBLE);
			mDiscogsLogout.setVisibility(View.VISIBLE);
			mDiscogsLogout.setOnClickListener(mOnDiscogsLogoutClickListener);
			mDiscogsAutoadd.setOnCheckedChangeListener(mDiscogsAutoaddListener);
		} else {
			mDiscogsLogin.setOnClickListener(mOnDiscogsLoginClickListener);
			mDiscogsLoggedIn.setVisibility(View.GONE);
			mDiscogsAutoadd.setVisibility(View.GONE);
			mDiscogsLogin.setVisibility(View.VISIBLE);
			mDiscogsLogout.setVisibility(View.GONE);
		}
	}

	private TextWatcher mNoOfRecentWatcher = new TextWatcher() {
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}

		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		public void afterTextChanged(Editable s) {
			if (s.length() == 0)
				return;
			int newNRecent = Integer.parseInt(s.toString());
			int oldNRecent = mPrefs.getInt(PREF_N_RECENT, DEFAULT_N_RECENT);
			if (newNRecent != oldNRecent) {
				mPrefs.edit().putInt(PREF_N_RECENT, newNRecent).commit();
			}
		}
	};
	
	private OnCheckedChangeListener mDiscogsAutoaddListener = new OnCheckedChangeListener() {
		
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if(isChecked) {
				mDiscogs.setAutoadd(true);
			} else {
				mDiscogs.setAutoadd(false);
			}
		}
	};

	private OnClickListener mOnLoginClickListener = new OnClickListener() {

		public void onClick(View v) {
			showDialog(DIALOG_LOGIN);
		}
	};

	private OnClickListener mOnLogoutClickListener = new OnClickListener() {

		public void onClick(View v) {
			mLastfm.forgetSession();
			showCorrectControl();
		}
	};

	private OnClickListener mOnDiscogsLoginClickListener = new OnClickListener() {

		public void onClick(View v) {
			startActivityForResult(new Intent(SettingsScreen.this,
					DiscogsOauth.class), DISCOGS_LOGIN_REQUEST_CODE);
		}
	};

	private OnClickListener mOnDiscogsLogoutClickListener = new OnClickListener() {

		public void onClick(View v) {
			mDiscogs.forgetSession();
			showCorrectControl();
		}
	};

	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_LOGIN:
			final View login_wd = getLayoutInflater().inflate(
					R.layout.lastfm_login_dialog, null);
			return (new AlertDialog.Builder(this))
					.setView(login_wd)
					.setPositiveButton(R.string.login,
							new Dialog.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									EditText username = (EditText) login_wd
											.findViewById(R.id.login_username);
									EditText password = (EditText) login_wd
											.findViewById(R.id.login_password);

									String sPassword = password.getText()
											.toString();
									String sUsername = username.getText()
											.toString();

									String authToken = Helper.hexMD5(sUsername
											+ Helper.hexMD5(sPassword));

									mLastfm.doGetMobileSession(sUsername,
											authToken,
											new Lastfm.ResultWaiter() {
												public void onResult(
														Map<String, String> result) {
													showCorrectControl();
												}
											}, SettingsScreen.this);

								}
							}).setNegativeButton(android.R.string.cancel, null)
					.create();
		default:
			return null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		menu.findItem(R.id.item_settings).setVisible(false);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return MainScreen.handleMenuEvent(this, item)
				|| super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK
				&& requestCode == DISCOGS_LOGIN_REQUEST_CODE) {
			String accessToken = data.getStringExtra("access_token");
			String accessSecret = data.getStringExtra("access_secret");
			mDiscogs.setWaiter(new Discogs.ResultWaiter() {
				public void onResult(Map<String, String> result) {
					showCorrectControl();
				}
			});
			mDiscogs.setSession(accessToken, accessSecret);
		}
	}

}
