package de.jollybox.vinylscrobbler;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.scribe.exceptions.OAuthException;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings.PluginState;
import de.jollybox.vinylscrobbler.util.Discogs;

public class DiscogsOauth extends Activity {

	private OAuthService mService;
	private Token mRequestToken;
	private WebView mWebView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.discogs_oauth);
		mWebView = (WebView) findViewById(R.id.discogs_webview);
		mService = new Discogs(this).getOAuthService();
		//for pretty formatting of the discogs auth page
		mWebView.getSettings().setJavaScriptEnabled(true);
		System.out.println(mWebView.getSettings().getUserAgentString());
		new DiscogsAuthTask().execute();
	}

	private class DiscogsAuthTask extends AsyncTask<Void, Void, String> {

		@Override
		protected String doInBackground(Void... params) {
			// Temporary URL
			String authURL = "http://api.discogs.com/";

			try {
				mRequestToken = mService.getRequestToken();
				authURL = mService.getAuthorizationUrl(mRequestToken);
			} catch (OAuthException e) {
				e.printStackTrace();
				return null;
			}
			return authURL;
		}

		@Override
		protected void onPostExecute(String authURL) {
			mWebView.setWebViewClient(new WebViewClient() {

				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					super.shouldOverrideUrlLoading(view, url);

					if (url.startsWith("oauth")) {
						mWebView.setVisibility(WebView.GONE);

						final String url1 = url;
						Thread t1 = new Thread() {
							public void run() {
								//decode URL in case it contains + ? = htmlencoded
								String decodedUrl = url1;
								try {
									decodedUrl = URLDecoder.decode(decodedUrl,"UTF-8");
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
								Uri uri = Uri.parse(decodedUrl);

								String verifier = uri.getQueryParameter("oauth_verifier");
								Verifier v = new Verifier(verifier);
								Token accessToken = mService.getAccessToken(mRequestToken, v);
								Intent intent = new Intent();
								intent.putExtra("access_token",accessToken.getToken());
								intent.putExtra("access_secret",accessToken.getSecret());
								setResult(RESULT_OK, intent);
								finish();
							}
						};
						t1.start();
					}

					return false;
				}
			});
			mWebView.loadUrl(authURL);
		}
	}
}
