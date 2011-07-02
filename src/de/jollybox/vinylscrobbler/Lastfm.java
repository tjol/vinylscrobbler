package de.jollybox.vinylscrobbler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;

@SuppressWarnings("unchecked")
public class Lastfm extends ContextWrapper {
	
	private final String API_ROOT;
	private final String API_KEY;
	private final String API_SECRET;
	
	private final static String PREFS_FILE_NAME = "de.jollybox.vinylscrobbler.Lastfm";
	private SharedPreferences mPrefs;
	
	private String mSessionKey;
	private String mSessionUser;
			
	public Lastfm (Context context) {
		super(context);
		Resources res = context.getResources();
		
		API_ROOT = res.getString(R.string.lastfm_api_root);
		API_KEY = res.getString(R.string.lastfm_api_key);
		API_SECRET = res.getString(R.string.lastfm_api_secret);
	
		mPrefs = getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE);
		mSessionKey = mPrefs.getString("session_key", null);
		mSessionUser = mPrefs.getString("session_user", null);
	
	}
	
	public void doGetMobileSession (String username, String authToken,
									final ResultWaiter waiter, final ErrorHandler errh) {

		TreeMap<String, String> args = new TreeMap<String, String>();
		args.put("method", "auth.getMobileSession");
		args.put("username", username);
		args.put("authToken", authToken);
		args.put("api_key", API_KEY);
		addSignature(args);
		
		AsyncTask<Map<String,String>,Void,Map<String,String>> task = 
		new AsyncTask<Map<String,String>, Void, Map<String,String>>() {
			private String errorMsg = null;
			
			@Override
			protected Map<String, String> doInBackground(Map<String,String>... params) {
				try {
					List<Map<String,String>> results = queryLastfm(params[0], false);
					if (results.size() == 1)
						return results.get(0);
				} catch (LastfmException lfmex) {
					errorMsg = String.format(getResources().getString(R.string.error_lastfm),
											 lfmex.toString());
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Map<String, String> result) {
				super.onPostExecute(result);
				
				if (errorMsg == null) {
					mSessionKey = result.get("key");
					mSessionUser = result.get("name");
					saveSession();
					if (waiter != null)
						waiter.onResult(result);
				} else if (errh != null) {
					errh.errorMessage(errorMsg);
				}
			}
		};
		task.execute(args);
	}
	
	public void scrobbleTracks (List<TrackList.Track> tracks, long[] times, String releaseTitle, String releaseArtist,
			final ResultWaiter waiter, final ErrorHandler errh) {
		
		TreeMap<String, String> args = new TreeMap<String, String>();
		
		int i = 0;
		for (TrackList.Track t : tracks) {
			args.put("track["+i+"]", t.getTitle());
			args.put("timestamp["+i+"]", Long.toString(times[i]));
			if (t.getArtists().length > 0) {
				args.put("artist["+i+"]", t.getArtistString());
				args.put("albumArtist["+i+"]", releaseArtist);
			} else {
				args.put("artist["+i+"]", releaseArtist);
			}
			args.put("album["+i+"]", releaseTitle);
			args.put("duration["+i+"]", Integer.toString(t.getDurationInSeconds()));
			
			++i;
		}
		
		args.put("api_key", API_KEY);
		args.put("sk", mSessionKey);
		args.put("method", "track.scrobble");
		addSignature(args);
		
		AsyncTask<Map<String,String>, Void, Object> task = new AsyncTask<Map<String,String>, Void, Object> () {
			private String errorMsg = null;

			@Override
			protected Object doInBackground(Map<String, String>... params) {
				try {
					queryLastfm(params[0], true);
				} catch (LastfmException lfmex) {
					errorMsg = String.format(getResources().getString(R.string.error_lastfm),
											 lfmex.toString());
				}
				return null;
			}

			@Override
			protected void onPostExecute(Object result) {
				if (errorMsg == null) {
					if (waiter != null) {
						waiter.onResult(null);
					}
				} else if (errh != null) {
					errh.errorMessage(errorMsg);
				}
			}
		};
		task.execute(args);
	}
	
	public String getUser () {
		return mSessionUser;
	}
	
	public void forgetSession () {
		mSessionKey = null;
		mSessionUser = null;
		saveSession();
	}
	
	private void saveSession () {
		SharedPreferences.Editor prefEdit = mPrefs.edit();
		
		if (mSessionKey != null) {
			prefEdit.putString("session_key", mSessionKey);
		} else {
			prefEdit.remove("session_key");
		}
		
		if (mSessionUser != null) {
			prefEdit.putString("session_user", mSessionUser);
		} else {
			prefEdit.remove("session_user");
		}
		
		prefEdit.commit();
	}
	
	private void addSignature (TreeMap<String, String> args) {
		String rawsig = API_SECRET;
		
		String key;
		SortedMap<String, String> head = args;
		while (!head.isEmpty()) {
			key = head.lastKey();
			rawsig = key + head.get(key) + rawsig;
			
			head = head.headMap(key);
		}
		
		args.put("api_sig", Helper.hexMD5(rawsig));
	}
	
	private List<Map<String, String>> queryLastfm (Map<String, String> args, boolean isWrite)
											throws LastfmException {
		HttpUriRequest request;
		if (isWrite) {
			List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
			for (Map.Entry<String,String> param : args.entrySet()) {
				params.add(new BasicNameValuePair(param.getKey(), param.getValue()));
			}
			HttpPost postrequest = new HttpPost(API_ROOT);
			try {
				postrequest.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// meh.
			}
			request = postrequest;
		} else {
			Uri.Builder uri_builder = Uri.parse(API_ROOT).buildUpon();
			for (Map.Entry<String,String> param : args.entrySet()) {
				uri_builder.appendQueryParameter(param.getKey(), param.getValue());
			}
			request = new HttpGet(uri_builder.build().toString());
		}
		InputStream response;
		try {
			response = Helper.doRequest(getBaseContext(), request);
		} catch (IOException io_exc) {
			throw new LastfmException("Cannot communicate with Last.fm", io_exc);
		}
		
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		
		class LastfmResponseHandler extends DefaultHandler {
			public boolean isOk;
			public int errorCode;
			public String errorDescr;
			public ArrayList<Map<String,String>> dataSets = new ArrayList<Map<String,String>>();
			private StringBuilder mChars;
			private Map<String,String> mCurrentDataSet = null;

			@Override
			public void characters(char[] ch, int start, int length)
					throws SAXException {
				super.characters(ch, start, length);
				if (mChars != null) {
					mChars.append(new String(ch, start, length));
				}
			}

			@Override
			public void endElement(String uri, String localName, String qName)
					throws SAXException {
				super.endElement(uri, localName, qName);
				
				if (localName.equals("session")) {
					dataSets.add(mCurrentDataSet);
					mCurrentDataSet = null;
				} else if (localName.equals("error")) {
					errorDescr = mChars.toString();
				} else if (localName.equals("name")			||
						   localName.equals("key")			||
						   localName.equals("subscriber")) {
					if (mCurrentDataSet != null) {
						mCurrentDataSet.put(localName, mChars.toString());
					}
				}
			}

			@Override
			public void startElement(String uri, String localName,
					String qName, Attributes attributes) throws SAXException {
				super.startElement(uri, localName, qName, attributes);
				
				if (localName.equals("lfm")) {
					String status = attributes.getValue(uri, "status");
					if (status.equals("ok")) {
						this.isOk = true;
					} else {
						this.isOk = false;
					}
				} else if (localName.equals("session")) {
					mCurrentDataSet = new HashMap<String, String>();
				} else if (localName.equals("error")) {
					try {
						errorCode = Integer.parseInt(attributes.getValue(uri, "code"));
					} catch (NumberFormatException nf_e) {
						throw new SAXException(nf_e);
					}
					mChars = new StringBuilder();
				} else if (localName.equals("name")			||
						   localName.equals("key")			||
						   localName.equals("subscriber")) {
					mChars = new StringBuilder();
				}
			}
			
		}

		LastfmResponseHandler handler = new LastfmResponseHandler();
		try {
			SAXParser parser = parserFactory.newSAXParser();
			InputSource xmlInput = new InputSource(new InputStreamReader(response, "UTF-8"));
			parser.parse(xmlInput, handler);
		} catch (Exception e) {
			throw new LastfmException(e);
		}
		
		if (handler.isOk) {
			return handler.dataSets;
		} else {
			throw new LastfmException(handler.errorCode, handler.errorDescr);
		}
	}
	
	@SuppressWarnings("serial")
	public class LastfmException extends Exception {
		private Integer mErrorCode = null;
		
		public LastfmException() { super(); }
		public LastfmException(String description) { super(description); }
		public LastfmException(Throwable cause) { super(cause); }
		public LastfmException(String description, Throwable cause) { super(description, cause); }
		public LastfmException(int errorCode, String description) {
			super(description);
			mErrorCode = new Integer(errorCode);
		}
		
		/**
		 * @return Last.fm error code. May be null.
		 */
		public Integer getErrorCode () {
			return mErrorCode;
		}
		
		// TODO: nice toString.
	}
	
	public interface ResultWaiter {
		public void onResult(Map<String,String> result);
	}
	
	public interface ErrorHandler {
		public void errorMessage(String message);
	}
}
