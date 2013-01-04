package de.jollybox.vinylscrobbler.util;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import de.jollybox.vinylscrobbler.SettingsScreen;
import de.jollybox.vinylscrobbler.util.ReleaseInfo.ReleaseSummary;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.util.SparseArray;
import android.util.SparseIntArray;

public class VinylDatabase extends SQLiteOpenHelper {

	private static final int DB_VERSION = 2;
	private static final String DB_NAME = "VinylScrobbler";
	private static VinylDatabase instance;
	private final Context mContext;
	private boolean mCacheCollection;

	public VinylDatabase(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		mContext = context;
		//check if we need to cache the discogs collection
		setCacheCollection(new Discogs(context).isCacheCollection());
	}

	public static synchronized VinylDatabase getInstance(Context context) {
		if (instance == null)
			instance = new VinylDatabase(context);
		return instance;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE recent_releases (" + "release_spec TEXT UNIQUE NOT NULL," + "id INT," + "is_master INT NOT NULL," + "title TEXT NOT NULL," + "format TEXT,"
				+ "label TEXT," + "country TEXT," + "thumb_uri TEXT," + "time INTEGER NOT NULL);");
		db.execSQL("CREATE TABLE discogs_collection (" + "release_spec TEXT UNIQUE NOT NULL," + "id INT," + "is_master INT NOT NULL," + "title TEXT NOT NULL," + "artist TEXT," + "thumb_uri TEXT);");
		db.execSQL("CREATE TABLE thumbs (" + "thumb_uri TEXT PRIMARY KEY," + "thumb BLOB," + "time INTEGER NOT NULL);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldversion, int newversion) {
		if (oldversion == 1 && newversion == 2) {
			// add support for discogs collection caching
			db.execSQL("CREATE TABLE discogs_collection (" + "release_spec TEXT UNIQUE NOT NULL," + "id INT," + "is_master INT NOT NULL," + "title TEXT NOT NULL," + "artist TEXT," + "thumb_uri TEXT);");
			db.execSQL("CREATE TABLE thumbs (" + "thumb_uri TEXT PRIMARY KEY," + "thumb BLOB," + "time INTEGER NOT NULL);");
		}
	}

	public void rememberRelease(ReleaseSummary release) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues(7);
		values.put("release_spec", String.format("/%s/%d", (release.isMaster() ? "master" : "release"), release.getId()));
		values.put("id", release.getId());
		values.put("is_master", release.isMaster());
		values.put("title", release.getTitle());
		if (!release.isMaster()) {
			values.put("format", release.getFormat());
			values.put("label", release.getLabel());
			values.put("country", release.getCountry());
		}
		values.put("thumb_uri", release.getThumbURI());
		values.put("time", System.currentTimeMillis());
		db.replace("recent_releases", null, values);

		SharedPreferences prefs = SettingsScreen.getPrefs(mContext);
		int nReleases = prefs.getInt(SettingsScreen.PREF_N_RECENT, SettingsScreen.DEFAULT_N_RECENT);
		// get releases to be forgotten.
		String[] arg = { Integer.toString(nReleases) };
		Cursor del_curs = db.rawQuery("SELECT release_spec FROM recent_releases " + "ORDER BY time DESC LIMIT -1 OFFSET ?", arg);
		if (del_curs.moveToFirst()) {
			do {
				arg[0] = del_curs.getString(0);
				db.delete("recent_releases", "release_spec=?", arg);
			} while (del_curs.moveToNext());
		}
		del_curs.close();
	}

	public List<ReleaseSummary> getRecentReleases() {
		SQLiteDatabase db = getWritableDatabase();

		String[] columns = { "id", "is_master", "title", "format", "label", "country", "thumb_uri" };
		Cursor curs = db.query("recent_releases", columns, null, null, null, null, "time DESC");
		List<ReleaseSummary> releases = new ArrayList<ReleaseSummary>(curs.getCount());
		if (curs.moveToFirst()) {
			do {
				releases.add(new ReleaseSummary(curs.getInt(0), curs.getInt(1) != 0, curs.getString(2), curs.getString(3), curs.getString(4), curs.getString(5), curs.getString(6)));
			} while (curs.moveToNext());
		}
		curs.close();
		return releases;
	}
	
	public long getCollectionSize() {
		if(!mCacheCollection) return 0;
		SQLiteDatabase db = getWritableDatabase();
		return DatabaseUtils.queryNumEntries(db,"discogs_collection");
	}

	/*
	 * diffs the local discogs collection with the remote discogs collection
	 * check each id of the local data to a map (toAdd) of the discogs release
	 * id mapped to the array index: -> local id found : release still in remote
	 * collection, remove from toAdd map -> local id not found : add to toRemove
	 * list toRemove contains all deleted releases from discogs toAdd contains
	 * only the new releases added to discogs
	 */
	public void updateDiscogsCollection(List<ReleaseSummary> discogs) {
		if(!mCacheCollection) return;
		// a map of ids to remove, with their corresponding thumb_uri, to clear
		// the stored thumb
		SparseArray<String> toRemove = new SparseArray<String>();
		// a map of release id keys and index values of the input list for
		// efficient lookups
		SparseIntArray toAdd = new SparseIntArray();
		for (int i = 0; i < discogs.size(); i++) {
			toAdd.put(discogs.get(i).getId(), i);
		}
		SQLiteDatabase db = getWritableDatabase();
		String[] columns = { "id", "thumb_uri" };
		Cursor curs = db.query("discogs_collection", columns, null, null, null, null, "id ASC");
		if (curs.moveToFirst()) {
			do {
				int id = curs.getInt(0);
				if (toAdd.get(id, -1) != -1) {
					toAdd.delete(id);
				} else {
					toRemove.append(id, curs.getString(1));
				}
			} while (curs.moveToNext());
		}
		curs.close();
		// diff lists have been created, remove local only releases and clear
		// the stored thumb if it exists
		for (int i = 0; i < toRemove.size(); i++) {
			String[] collId = { Integer.toString(toRemove.keyAt(i)) };
			String[] thumbURI = { toRemove.valueAt(i) };
			db.delete("discogs_collection", "id=?", collId);
			if (thumbURI[0] != null)
				db.delete("thumbs", "thumb_uri=?", thumbURI);
		}
		// now add the remote only releases
		for (int i = 0; i < toAdd.size(); i++) {
			ContentValues values = new ContentValues(6);
			ReleaseInfo.ReleaseSummary release = discogs.get(toAdd.valueAt(i));
			values.put("release_spec", String.format("/%s/%d", (release.isMaster() ? "master" : "release"), release.getId()));
			values.put("id", release.getId());
			values.put("is_master", release.isMaster());
			values.put("title", release.getTitle());
			values.put("artist", release.getArtist());
			values.put("thumb_uri", release.getThumbURI());
			db.replace("discogs_collection", null, values);
		}
	}

	public void storeThumb(String thumbURI, Bitmap thumb) {
		if(!mCacheCollection) return;
		if (thumb != null) {
			SQLiteDatabase db = getWritableDatabase();
			ContentValues values = new ContentValues(3);
			values.put("thumb_uri", thumbURI);
			values.put("thumb", getBitmapAsByteArray(thumb));
			values.put("time", System.currentTimeMillis());
			db.replace("thumbs", null, values);
		}
	}
	// clear Discogs collection from db
	public void clearCollection() {
		SQLiteDatabase db = getWritableDatabase();
		db.delete("discogs_collection", null, null);
		db.delete("thumbs",null,null);
	}

	public Bitmap getThumb(String thumbURI) {
		if(!mCacheCollection) return null;
		SQLiteDatabase db = getWritableDatabase();
		Bitmap storedThumb = null;
		String[] columns = { "thumb" };
		String thumbFilter = "thumb_uri=?";
		String[] whereArgs = new String[] { thumbURI };
		Cursor curs = db.query("thumbs", columns, thumbFilter, whereArgs, null, null, null);
		if (curs.moveToFirst()) {
			byte[] bitmapData = curs.getBlob(0);
			if (bitmapData != null) {
				storedThumb = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
			}
		}
		curs.close();
		return storedThumb;
	}

	public List<ReleaseSummary> getDiscogsCollection() {
		if(!mCacheCollection) return new ArrayList<ReleaseInfo.ReleaseSummary>();
		SQLiteDatabase db = getWritableDatabase();

		String[] columns = { "id", "is_master", "title", "artist", "thumb_uri" };
		Cursor curs = db.query("discogs_collection", columns, null, null, null, null, "artist ASC");
		List<ReleaseSummary> releases = new ArrayList<ReleaseSummary>(curs.getCount());
		if (curs.moveToFirst()) {
			do {
				ReleaseSummary release = new ReleaseSummary(curs.getInt(0), curs.getInt(1) != 0, curs.getString(2), curs.getString(3), null, null, null, curs.getString(4));
				release.setCollection(true);
				releases.add(release);
			} while (curs.moveToNext());
		}
		curs.close();
		return releases;
	}

	private byte[] getBitmapAsByteArray(Bitmap bitmap) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		bitmap.compress(CompressFormat.JPEG, 95, outputStream);
		return outputStream.toByteArray();
	}

	public boolean isCacheCollection() {
		return mCacheCollection;
	}

	public void setCacheCollection(boolean cache) {
		this.mCacheCollection = cache;
	}

}
