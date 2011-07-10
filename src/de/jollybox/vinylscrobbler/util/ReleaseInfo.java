/**
 * Vinyl Scrobbler app by JollyBOX.de
 *
 * Copyright (c) 2011	Thomas Jollans
 * 
 * Refer to the file COPYING for copying permissions.
 */

package de.jollybox.vinylscrobbler.util;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import de.jollybox.vinylscrobbler.DiscogsImageAdapter;

public class ReleaseInfo implements Cloneable {
	private String mTitle;
	private TrackList mTracks;
	private boolean mIsMaster;
	private int mMasterId;
	private int mMainVerionId;
	private List<Integer> mVersionIds;
	private List<Credit> mArtists;
	private List<String> mGenres;
	private List<String> mStyles;
	private String mDateString;
	private List<CatalogEntry> mCatalogEntries; // labels
	private List<String> mFormatStrings;
	private String mCountry;
	private List<Credit> mExtraArtists;
	private String mNotes;
	private JSONArray mImages; // TODO: JSONArray may not be ideal here.
	
	private Context mContext;
	
	private static WeakHashMap<JSONObject, SoftReference<ReleaseInfo>> cCache = 
			new WeakHashMap<JSONObject, SoftReference<ReleaseInfo>>();
	
	public static ReleaseInfo fromJSON(Context context, JSONObject discogsResp) throws JSONException {	
		if (discogsResp.has("resp")) {
			discogsResp = discogsResp.getJSONObject("resp");
		}
		
		ReleaseInfo release;
		synchronized(cCache) {
			if (cCache.containsKey(discogsResp)) {
				if ((release = cCache.get(discogsResp).get()) != null) {
					try {
						release = (ReleaseInfo) release.clone();
						release.mContext = context;
						// cache the new object: it's younger, so it may stay around longer.
						cCache.put(discogsResp, new SoftReference<ReleaseInfo>(release));
						return release;
					} catch (CloneNotSupportedException e) {
						throw new RuntimeException(e);
					}
				} else {
					cCache.remove(discogsResp);
				}
			}
		}
		release = new ReleaseInfo(context, discogsResp);
		cCache.put(discogsResp, new SoftReference<ReleaseInfo>(release));
		return release;
	}
	
	private ReleaseInfo (Context context, JSONObject discogsResp) throws JSONException {
		int i;
		
		mContext = context;
		
		mIsMaster = discogsResp.has("master");
		
		JSONObject r;
		if (mIsMaster) {
			r = discogsResp.getJSONObject("master");
			
			mMainVerionId = r.getInt("main_release");
			
			JSONArray versions = r.getJSONArray("versions");
			mVersionIds = new ArrayList<Integer>(versions.length());
			for (i = 0; i < versions.length(); ++i) {
				JSONObject version = versions.getJSONObject(i);
				int id = version.getInt("id");
				if (id == mMainVerionId) {
					mTitle = version.getString("title");
				}
				mVersionIds.add(id);
			}
		} else {
			r = discogsResp.getJSONObject("release");
			mTitle = r.getString("title");
			mMasterId = r.optInt("master_id", -1);
		}
		
		if (r.has("formats")) {
			JSONArray formats = r.getJSONArray("formats");
			mFormatStrings = new ArrayList<String>(formats.length());
			for (i = 0; i < formats.length(); ++i) {
				JSONObject jfmt = formats.getJSONObject(i);
				StringBuilder fmt = new StringBuilder();
				fmt.append(String.format("%s× %s",
										 jfmt.getString("qty"),
										 jfmt.getString("name")));
				if (jfmt.has("descriptions")) {
					JSONArray descriptions = jfmt.getJSONArray("descriptions");
					if (descriptions.length() > 0) {
						fmt.append(" (");
						for (int j = 0; j < descriptions.length(); ++j) {
							if (j != 0) fmt.append(", ");
							fmt.append(descriptions.getString(j));
						}
						fmt.append(")");
					}
				}
				mFormatStrings.add(fmt.toString());
			}
		} else {
			mFormatStrings = new ArrayList<String>();
		}
		
		if (r.has("artists")) {
			JSONArray artists = r.getJSONArray("artists");
			mArtists = new ArrayList<Credit>(artists.length());
			for (i = 0; i < artists.length(); ++i) {
				mArtists.add(new Credit(artists.getJSONObject(i)));
			}
		} else {
			mArtists = new ArrayList<Credit>();
		}
		
		if (r.has("extra_artists")) {
			JSONArray extra_artists = r.getJSONArray("extra_artists");
			mExtraArtists = new ArrayList<Credit>(extra_artists.length());
			for (i = 0; i < extra_artists.length(); ++i) {
				mExtraArtists.add(new Credit(extra_artists.getJSONObject(i)));
			}
		} else {
			mExtraArtists = new ArrayList<Credit>();
		}
		
		if (r.has("genres")) {
			JSONArray genres = r.getJSONArray("genres");
			mGenres = new ArrayList<String>(genres.length());
			for (i = 0; i < genres.length(); ++i) {
				mGenres.add(genres.getString(i));
			}
		} else {
			mGenres = new ArrayList<String>();
		}
		
		if (r.has("styles")) {
			JSONArray styles = r.getJSONArray("styles");
			mStyles = new ArrayList<String>(styles.length());
			for (i = 0; i < styles.length(); ++i) {
				mStyles.add(styles.getString(i));
			}
		} else {
			mStyles = new ArrayList<String>();
		}
		
		if (r.has("released_formatted")) {
			mDateString = r.getString("released_formatted");
		} else if (r.has("year")) {
			mDateString = r.getString("year");
		} else {
			mDateString = null;
		}
		
		if (r.has("labels")) {
			JSONArray labels = r.getJSONArray("labels");
			mCatalogEntries = new ArrayList<CatalogEntry>(labels.length());
			for (i = 0; i < labels.length(); ++i) {
				mCatalogEntries.add(new CatalogEntry(labels.getJSONObject(i)));
			}
		} else {
			mCatalogEntries = new ArrayList<CatalogEntry>();
		}
		
		mCountry = r.optString("country", null);
		mNotes = r.optString("notes", null);
		
		mImages = r.optJSONArray("images");
		
		mTracks = new TrackList(mContext, r.getJSONArray("tracklist"));
	}
	

	public String getTitle() {
		return mTitle;
	}

	public TrackList getTracks() {
		return mTracks;
	}

	public boolean isMaster() {
		return mIsMaster;
	}

	public int getMasterId() {
		if (! mIsMaster) {
			return mMasterId;
		} else {
			throw new RuntimeException("This is a master release.");
		}
	}
	
	public List<Integer> getVersionIds() {
		if (mIsMaster) {
			return new ArrayList<Integer>(mVersionIds);
		} else {
			throw new RuntimeException("This is not a master release.");
		}
	}
	
	public List<Credit> getArtists() {
		return new ArrayList<Credit>(mArtists);
	}
	
	public String getArtistString() {
		return Credit.artistsString(mArtists);
	}

	public List<String> getGenres() {
		return new ArrayList<String>(mGenres);
	}

	public List<String> getStyles() {
		return new ArrayList<String>(mStyles);
	}
	
	public List<String> getFormatStrings() {
		if (mIsMaster) {
			throw new RuntimeException("This is a master release.");
		}
		return new ArrayList<String>(mFormatStrings);
	}
	
	public String getFormatString() {
		if (mIsMaster) {
			throw new RuntimeException("This is a master release.");
		}
		StringBuilder retv = new StringBuilder();
		boolean first = true;
		for (String f : mFormatStrings) {
			if (!first) retv.append(", ");
			else		first = false;
			retv.append(f);
		}
		return retv.toString();
	}

	public String getDateString() {
		return mDateString;
	}

	public List<CatalogEntry> getCatalogEntries() {
		return new ArrayList<CatalogEntry>(mCatalogEntries);
	}

	public String getCountry() {
		return mCountry;
	}

	public List<Credit> getExtraArtists() {
		return new ArrayList<Credit>(mExtraArtists);
	}

	public String getNotes() {
		return mNotes;
	}

	public DiscogsImageAdapter getGallery () {
		if (mImages != null) {
			return new DiscogsImageAdapter(mContext, mImages);
		} else {
			return null;
		}
	}
	
	public static class CatalogEntry {
		private String mLabel;
		private String mCatalogNumber;
		
		public CatalogEntry (String label, String catno) {
			mLabel = label;
			mCatalogNumber = catno;
		}
		
		public CatalogEntry (JSONObject json) throws JSONException {
			mLabel = json.getString("name");
			mCatalogNumber = json.getString("catno");
		}

		public String getLabel() {
			return mLabel;
		}

		public String getCatalogNumber() {
			return mCatalogNumber;
		}
	}
	
	public static class Credit {
		private String mArtist;
		private String mArtistNameVar;
		private String mRole;
		private String mJoin; // what is this?
		private String mTracks; //
		
		public Credit (JSONObject source) throws JSONException {
			mArtist = source.getString("name");
			mArtistNameVar = source.getString("anv");
			mRole = source.getString("role");
			mJoin = source.getString("join");
			mTracks = source.getString("tracks");
		}
		
		public String getCanonicalArtistName() {
			return mArtist;
		}
		
		public String getArtist() {
			if (mArtistNameVar.equals("")) {
				return mArtist;
			} else {
				return mArtistNameVar;
			}
		}
		
		public String getRole() {
			return mRole;
		}
		
		public String getJoin() {
			return mJoin;
		}
		
		public String getTracks() {
			return mTracks;
		}
		
		public static String artistsString(Iterable<Credit> artists) {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (Credit c : artists) {
				if (!first) sb.append(" / ");
				else first = false;
				
				sb.append(c.getArtist());
			}
			return sb.toString();
		}
	}
	
	public static class ReleaseSummary {
		protected int mId = -1;
		protected String mThumbURI;
		protected String mFormat = null;
		protected String mTitle = null;
		protected boolean mIsMaster;
		protected String mCountry = null;
		protected String mLabel = null;
		
		protected ReleaseSummary () { super(); }
		
		public String getThumbURI() {
			return mThumbURI;
		}
		public String getFormat() {
			if (! mIsMaster) {
				return mFormat;
			} else {
				throw new RuntimeException("This is a master release.");
			}
		}
		public String getTitle() {
			return mTitle;
		}
		public boolean isMaster() {
			return mIsMaster;
		}
		public String getCountry() {
			if (! mIsMaster) {
				return mCountry;
			} else {
				throw new RuntimeException("This is a master release.");
			}
		}
		public String getLabel() {
			if (! mIsMaster) {
				return mLabel;
			} else {
				throw new RuntimeException("This is a master release.");
			}
		}
		public int getId() {
			return mId;
		}
		
		public static List<ReleaseSummary> fromJSONArray (final JSONArray arr) throws JSONException {
			List<ReleaseSummary> rv = new ArrayList<ReleaseSummary>(arr.length());
			for (int i = 0; i < arr.length(); ++i) {
				JSONObject desc = arr.getJSONObject(i);
				ReleaseSummary s = new ReleaseSummary();
				s.mIsMaster = desc.has("type") && desc.getString("type").equals("master");
				s.mId = desc.getInt("id");
				s.mTitle = desc.getString("title");
				s.mCountry = desc.optString("country", null);
				s.mFormat = desc.optString("format");
				s.mLabel = desc.optString("label", null);
				s.mThumbURI = desc.optString("thumb", null);
				rv.add(s);
			}
			return rv;
		}
	}
}
