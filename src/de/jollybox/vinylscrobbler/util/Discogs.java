/**
 * Vinyl Scrobbler app by JollyBOX.de
 *
 * Copyright (c) 2011	Thomas Jollans
 * 
 * Refer to the file COPYING for copying permissions.
 */

package de.jollybox.vinylscrobbler.util;

import java.util.Map;

import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Resources;
import de.jollybox.vinylscrobbler.R;

public class Discogs extends ContextWrapper {
	private final String API_KEY;
	private final String API_SECRET;

	private SharedPreferences mPrefs;
	private final static String PREFS_FILE_NAME = "de.jollybox.vinylscrobbler.Discogs";

	private OAuthService mOAuthService;
	private String mAccessToken;
	private String mAccessSecret;
	private String mUserName;
	
	private ResultWaiter mWaiter;

	public Discogs(Context context) {
		super(context);
		Resources res = context.getResources();

		API_KEY = res.getString(R.string.discogs_api_key);
		API_SECRET = res.getString(R.string.discogs_api_secret);

		mPrefs = getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE);
		mAccessToken = mPrefs.getString("access_token", null);
		mAccessSecret = mPrefs.getString("access_secret", null);
		mUserName = mPrefs.getString("user_name", null);
		
		mOAuthService = new ServiceBuilder().provider(DiscogsApi.class)
				.apiKey(API_KEY).apiSecret(API_SECRET)
				.callback("oauth://discogs").build();

	}
	
	public String getUser() {
		return mUserName;
	}

	public OAuthService getOAuthService() {
		return mOAuthService;
	}
	
	public Token getOAuthToken() {
		if (mAccessSecret != null && mAccessToken != null) {
			return new Token(mAccessToken,mAccessSecret);
		}
		return null;
	}
	
	public OAuthRequest signRequest(OAuthRequest request) {
		Token authToken = getOAuthToken();
		if(authToken != null) {
			mOAuthService.signRequest(getOAuthToken(), request);
		}
		return request;
	}
	
	public void setWaiter(ResultWaiter waiter) {
		mWaiter = waiter;
	}

	public void setSession(String accessToken, String accessSecret) {
		mAccessToken = accessToken;
		mAccessSecret = accessSecret;
		String query_string = "/oauth/identity";
		DiscogsQuery q = new DiscogsQuery.WithAlertDialog(this, false, this) {
			@Override
			protected void onResult(JSONObject result) {
				String username;
				try {
					username = result.getString("username");
				} catch (Exception exc) {
					errorMessage(res.getString(R.string.error_invalid_data));
					return;
				}
				if (username != null) {
					//only save the credentials when a username has been correctly parsed
					mUserName = username;
					saveSession();
					if(mWaiter != null) {
						mWaiter.onResult(null);
					}
				}
			}
		};
		
		q.execute(query_string);
	}

	public void forgetSession() {
		mAccessToken = null;
		mAccessSecret = null;
		mUserName = null;
		saveSession();
	}

	private void saveSession() {
		SharedPreferences.Editor prefEdit = mPrefs.edit();

		if (mAccessToken != null) {
			prefEdit.putString("access_token", mAccessToken);
		} else {
			prefEdit.remove("access_token");
		}

		if (mAccessSecret != null) {
			prefEdit.putString("access_secret", mAccessSecret);
		} else {
			prefEdit.remove("access_secret");
		}
		
		if (mUserName != null) {
			prefEdit.putString("user_name", mUserName);
		} else {
			prefEdit.remove("user_name");
		}

		prefEdit.commit();
	}
	
	public interface ResultWaiter {
		public void onResult(Map<String,String> result);
	}
}
