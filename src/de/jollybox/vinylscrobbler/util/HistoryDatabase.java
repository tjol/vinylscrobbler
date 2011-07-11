package de.jollybox.vinylscrobbler.util;

import java.util.ArrayList;
import java.util.List;

import de.jollybox.vinylscrobbler.util.ReleaseInfo.ReleaseSummary;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HistoryDatabase extends SQLiteOpenHelper {
	
	private static final int DB_VERSION = 1;
	private static final String DB_NAME = "VinylScrobbler";
	
	public HistoryDatabase(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE recent_releases ("
					+ "release_spec TEXT UNIQUE NOT NULL,"
					+ "id INT,"
					+ "is_master INT NOT NULL,"
					+ "title TEXT NOT NULL,"
					+ "format TEXT,"
					+ "label TEXT,"
					+ "country TEXT,"
					+ "thumb_uri TEXT,"
					+ "time INTEGER NOT NULL);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldversion, int newversion) {
		// pass
	}
	
	public void rememberRelease(ReleaseSummary release) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues(7);
		values.put("release_spec", String.format("/%s/%d",
								   (release.isMaster() ? "master" : "release"),
								   release.getId()));
		values.put("id", release.getId());
		values.put("is_master", release.isMaster());
		values.put("title", release.getTitle());
		values.put("format", release.getFormat());
		values.put("label", release.getLabel());
		values.put("country", release.getCountry());
		values.put("thumb_uri", release.getThumbURI());
		values.put("time", System.currentTimeMillis());
		db.replace("recent_releases", null, values);
	}
	
	public List<ReleaseSummary> getRecentReleases() {
		SQLiteDatabase db = getWritableDatabase();
		
		String[] columns = {"id", "is_master", "title", "format",
							"label", "country", "thumb_uri"};
		Cursor curs = db.query("recent_releases", columns, null, null, null, null, "time DESC");
		List<ReleaseSummary> releases = new ArrayList<ReleaseSummary>(curs.getCount());
		curs.moveToFirst(); // Is this necessary?
		do {
			releases.add(new ReleaseSummary(curs.getInt(0),
											curs.getInt(1) != 0, 
											curs.getString(2),
											curs.getString(3), 
											curs.getString(4),
											curs.getString(5), 
											curs.getString(6)));
		} while (curs.moveToNext());
		return releases;
	}
}
