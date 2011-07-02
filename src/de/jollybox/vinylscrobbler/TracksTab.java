package de.jollybox.vinylscrobbler;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
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
	private String mTitle;
	private String mArtist;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		res = getResources();
		
		setContentView(R.layout.tracks);
		
		mScrobbleBtn = (Button) findViewById(R.id.scrobble);
		registerForContextMenu(mScrobbleBtn);
		mScrobbleBtn.setOnClickListener(mOnScrobbleClickListener);
		
		initLastfm();
		
		Intent intent = getIntent();
		String query_string = intent.getData().getEncodedPath();
		
		DiscogsQuery query = new DiscogsQuery.WithAlertDialog(this) {
			@Override
			protected void onResult(JSONObject result) {
				JSONObject release;
				
				try {
					JSONObject resp = result.getJSONObject("resp");
					if (resp.has("master")) {
						release = resp.getJSONObject("master");
						mTitle = release.getJSONArray("versions").getJSONObject(0).getString("title");
					} else {
						release = resp.getJSONObject("release");
						mTitle = release.getString("title");
					}
					
					if (release.has("artists")) {
						StringBuilder artiststring = new StringBuilder();
						JSONArray artists = release.getJSONArray("artists");
						for (int i = 0; i < artists.length(); ++i) {
							if (i != 0) artiststring.append(" / ");
							artiststring.append(artists.getJSONObject(i).getString("name"));
						}
						mArtist = artiststring.toString();
					} else {
						mArtist = "Unknown";
					}
					
					JSONArray tracklist_json = release.getJSONArray("tracklist");
					mTracks = new TrackList(TracksTab.this, tracklist_json);
					ListView list = getListView();
					setListAdapter(mTracks);
					list.setOnItemClickListener(TracksTab.this);
				} catch (JSONException json_exc) {
					errorMessage(res.getString(R.string.error_invalid_data));
				}
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
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		if (mTracks == null) return;
		
		int nSelected = mTracks.getSelected().size();
		
		menu.add(SC_ALL, Menu.NONE, Menu.NONE, R.string.scrobble_all);
		mMenuScrobbleSelected = menu.add(SC_SELECTED, Menu.NONE, Menu.NONE,
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
				menu.add(SC_PART, i, Menu.NONE,
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
		List<TrackList.Track> scrobble_these;
		
		switch (item.getGroupId()) {
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
		
		long[] times = new long[scrobble_these.size()];
		long current_time = System.currentTimeMillis() /1000;
		
		for (int i = scrobble_these.size()-1; i >= 0; --i) {
			int length = scrobble_these.get(i).getDurationInSeconds();
			if (length == 0) length = 60;
			current_time -= length;
			times[i] = current_time;
		}
		
		mLastfm.scrobbleTracks(scrobble_these, times, mTitle, mArtist, this, this);
		
		return true;
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

