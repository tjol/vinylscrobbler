/**
 * Vinyl Scrobbler app by JollyBOX.de
 *
 * Copyright (c) 2011	Thomas Jollans
 * 
 * Refer to the file COPYING for copying permissions.
 */

package de.jollybox.vinylscrobbler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import de.jollybox.vinylscrobbler.util.Discogs;
import de.jollybox.vinylscrobbler.util.DiscogsQuery;
import de.jollybox.vinylscrobbler.util.VinylDatabase;
import de.jollybox.vinylscrobbler.util.Lastfm;
import de.jollybox.vinylscrobbler.util.ReleaseInfo;
import de.jollybox.vinylscrobbler.util.TrackList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

public class TracksTab extends ListActivity
					   implements OnItemClickListener, Lastfm.ResultWaiter, Lastfm.ErrorHandler {
	private TrackList mTracks = null;
	private Lastfm mLastfm;
	private Button mScrobbleBtn;
	private Resources res;
	private ReleaseInfo mRelease;
	private Discogs mDiscogs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		res = getResources();
		
		setContentView(R.layout.tracks);
		
		mScrobbleBtn = (Button) findViewById(R.id.scrobble);
		registerForContextMenu(mScrobbleBtn);
		mScrobbleBtn.setOnClickListener(mOnScrobbleClickListener);
		
		initLastfm();
		
		mDiscogs = new Discogs(this);
		
		Intent intent = getIntent();
		String query_string = intent.getData().getEncodedPath();
		
		DiscogsQuery query = new DiscogsQuery.WithAlertDialog(this) {
			@Override
			protected void onResult(JSONObject result) {
				try {
					mRelease = ReleaseInfo.fromJSON(mContext, result);
				} catch (JSONException json_exc) {
					errorMessage(res.getString(R.string.error_invalid_data));
					return;
				}
				mTracks = mRelease.getTracks();
				ListView list = getListView();
				setListAdapter(mTracks);
				list.setOnItemClickListener(TracksTab.this);
				list.setVerticalFadingEdgeEnabled(true);
			}
		};
		
		query.execute(query_string);
					
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		initLastfm(); // User could have visited the settings in the mean time.
	}
	
	private void initLastfm () {
		mLastfm = new Lastfm(this);
		
		if (mLastfm.getUser() == null) {
			mScrobbleBtn.setVisibility(View.GONE);
		} else {
			mScrobbleBtn.setVisibility(View.VISIBLE);
		}
	}
	
	public void onItemClick(AdapterView<?> av, View item, int idx, long hash) {
		TrackList.Track track = ((TrackList.Track)av.getItemAtPosition(idx));
		if (track.getPosition().length() != 0) {
			track.toggleSelected();
			CheckBox cb = (CheckBox) item.findViewById(R.id.track_selected);
			cb.setChecked(track.isSelected());
		}
	}
	
	private MenuItem mMenuScrobbleSelected;
	private final int SC_ALL = 0x1;
	private final int SC_SELECTED = 0x2;
	private final int SC_PART = 0x3;
	
	private final int SC_T_PAST = 0x0;
	private final int SC_T_FUTURE = 0x4;
	private final int SC_T_MASK = SC_T_PAST | SC_T_FUTURE;
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		if (mTracks == null) return;
		
		// Add items to the menu
		Menu futureMenu = menu.addSubMenu(R.string.scrobble_future);
		costructScobbleSubMenu(futureMenu, SC_T_FUTURE);
		Menu pastMenu = menu.addSubMenu(R.string.scrobble_past);
		costructScobbleSubMenu(pastMenu, SC_T_PAST);
		
	}
		
	private void costructScobbleSubMenu (Menu menu, final int id_flag) {
		
		// Create a sub-menu listing the different ways to scrobble a track.
		
		int nSelected = mTracks.getSelected().size();
		
		menu.add(id_flag | SC_ALL, Menu.NONE, Menu.NONE, R.string.scrobble_all);
		mMenuScrobbleSelected = menu.add(id_flag | SC_SELECTED, Menu.NONE, Menu.NONE,
										 res.getQuantityString(R.plurals.scrobble_selected, nSelected));
		if (nSelected == 0) {
			mMenuScrobbleSelected.setVisible(false);
		}
		int i = 0;
		List<TrackList.Part> parts = mTracks.getParts();
		if (parts != null) {
			for (TrackList.Part p : parts) {
				String text;
				if (p.getName().length() > 15) {
					text = "%s";
				} else if (p.getType() == TrackList.Part.Type.SIDE) {
					text = res.getString(R.string.scrobble_side);
				} else {
					text = res.getString(R.string.scrobble_disc);
				}
				menu.add(id_flag | SC_PART, i, Menu.NONE,
						 String.format(text, p.getName()));
				++i;
			}
		}
	}
	
	private OnClickListener mOnScrobbleClickListener = new OnClickListener() {
		
		public void onClick(View v) {
			if (mMenuScrobbleSelected != null) {
				int nSelected = mTracks.getSelected().size();
				mMenuScrobbleSelected.setTitle(res.getQuantityString(R.plurals.scrobble_selected, nSelected));
				mMenuScrobbleSelected.setVisible((nSelected != 0));
			}
			v.showContextMenu();
		}
	};
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// Handle a context menu item
		
		ArrayList<TrackList.Track> scrobble_these;
		
		// What should I scrobble?
		int groupId = item.getGroupId();
		switch (groupId & ~SC_T_MASK) {
		case SC_ALL:
			scrobble_these = mTracks.getTracks();
			break;
		case SC_SELECTED:
			scrobble_these = mTracks.getSelected();
			break;
		case SC_PART:
			scrobble_these = mTracks.getParts().get(item.getItemId()).getTracks();
			break;
		default:
			return false;
		}
		
		// When should I scrobble?
		switch (item.getGroupId() & SC_T_MASK) {
		case SC_T_FUTURE:
			scrobbleInTheFuture(scrobble_these);
			break;
		default: // Past
			scrobbleInThePast(scrobble_these);
			break;
		}
		
		//if we want to auto-add scrobbled releases to the discogs collection, do so now
		if(mDiscogs.getUser() != null && mDiscogs.isAutoadd()) {
			//check for main version id, defaults to release id if it is not a master release
			mDiscogs.addRelease(mRelease.getMainVersionId());
		}
		
		// Remember this scrobble and show it on the home screen next time.
		VinylDatabase history = new VinylDatabase(this);
		history.rememberRelease(mRelease.getSummary());
		
		return true;
	}
	
	private void scrobbleInThePast (List<TrackList.Track> scrobble_these) {
		
		long[] times = new long[scrobble_these.size()];
		long current_time = System.currentTimeMillis() /1000;
		
		for (int i = scrobble_these.size()-1; i >= 0; --i) {
			int length = scrobble_these.get(i).getDurationInSeconds();
			if (length == 0) length = 60;
			current_time -= length;
			times[i] = current_time;
		}
		
		mLastfm.scrobbleTracks(scrobble_these, times,
							   mRelease.getTitle(), mRelease.getArtistString(),
							   this, this);
	}
	
	private void scrobbleInTheFuture (ArrayList<TrackList.Track> scrobble_these) {
		Intent futureScrobbleIntent = new Intent(Intent.ACTION_DEFAULT,
												 new Uri.Builder()
												 .scheme("de.jollybox.vinylscrobbler")
												 .authority("FutureScrobbler")
												 .appendPath("queueTracks")
												 .build());
		futureScrobbleIntent.putExtra("releaseTitle", mRelease.getTitle());
		futureScrobbleIntent.putExtra("releaseArtist", mRelease.getArtistString());
		futureScrobbleIntent.putParcelableArrayListExtra("tracks", scrobble_these);
		futureScrobbleIntent.putExtra("startTime", System.currentTimeMillis());
		
		startService(futureScrobbleIntent);
	}

	public void onResult(Map<String, String> result) {
		Toast.makeText(this, R.string.scrobble_success, Toast.LENGTH_SHORT).show();
	}

	public void errorMessage(String message) {
		(new AlertDialog.Builder(this))
			.setMessage(message)
			.setNeutralButton("OK", null).show();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return  MainScreen.handleMenuEvent(this, item) || super.onOptionsItemSelected(item);
	}
}

