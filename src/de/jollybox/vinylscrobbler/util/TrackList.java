/**
 * Vinyl Scrobbler app by JollyBOX.de
 *
 * Copyright (c) 2011	Thomas Jollans
 * 
 * Refer to the file COPYING for copying permissions.
 */

package de.jollybox.vinylscrobbler.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.jollybox.vinylscrobbler.R;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class TrackList extends BaseAdapter {
	
	private final Track[] mTracks;
	private final Context mContext;
	private final LayoutInflater mInflater;
	
	private List<Part> mParts;
	
	public TrackList(Context context, JSONArray tracks) throws JSONException {
		mContext = context;
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		int len = tracks.length();
		mTracks = new Track[len];
		
		for (int i = 0; i < len; ++i) {
			JSONObject track = tracks.getJSONObject(i);
			String position = track.getString("position");
			String title = track.getString("title");
			String duration = track.getString("duration");
			JSONArray artists_j;
			List<ReleaseInfo.Credit> artists = null;
			if ((artists_j = track.optJSONArray("artists")) != null) {
				int nArtists = artists_j.length();
				artists = new ArrayList<ReleaseInfo.Credit>(nArtists);
				for (int j = 0; j < nArtists; ++j) {
					artists.add(new ReleaseInfo.Credit(artists_j.getJSONObject(j)));
				}
			}
			
			mTracks[i] = new Track(position, title, duration, artists);
		}
		
		findSidesAndDiscs();
	}
	
	private void findSidesAndDiscs() {
		Pattern sidePattern = Pattern.compile("^([A-Z])[0-9]*$");
		Pattern discPattern = Pattern.compile("^(.+)-[A-Z0-9]+$");
		
		List<Part> parts = new ArrayList<TrackList.Part>();
		String currentPartLongName = null;
		String currentPartName = null;
		String fullPartName;
		Part.Type currentPartType = null;
		ArrayList<Track> currentPart = new ArrayList<Track>();
		
		Matcher m;
		
		for (Track t : mTracks) {
			String pos = t.getPosition();
			if (pos.length() == 0) {
				// It's a title pseudo track
				if (currentPartName != null || currentPartLongName != null) {
					if (currentPartName != null) {
						fullPartName = currentPartName;
						if (currentPartLongName != null) {
							fullPartName = currentPartLongName + " (" + currentPartName + ")";
						}
					} else {
						fullPartName = currentPartLongName;
					}
					parts.add(new Part(currentPartType, fullPartName, currentPart));
					currentPart = new ArrayList<TrackList.Track>();
					currentPartName = null;
				}
				currentPartLongName = t.getTitle();
				continue;
			}
			if ((m = sidePattern.matcher(pos)).matches()) {
				String thisSide = m.group(1);
				if (currentPartName != null && !thisSide.equals(currentPartName)) {
					fullPartName = currentPartName;
					if (currentPartLongName != null) {
						fullPartName = currentPartLongName + " (" + currentPartName + ")";
					}
					parts.add(new Part(currentPartType, fullPartName, currentPart));
					currentPart = new ArrayList<TrackList.Track>();
				}
				currentPartName = thisSide;
				currentPartType = Part.Type.SIDE;
			} else if ((m = discPattern.matcher(pos)).matches()) {
				String thisDisc = m.group(1);
				if (currentPartName != null && !thisDisc.equals(currentPartName)) {
					fullPartName = currentPartName;
					if (currentPartLongName != null) {
						fullPartName = currentPartLongName + " (" + currentPartName + ")";
					}
					parts.add(new Part(currentPartType, fullPartName, currentPart));
					currentPart = new ArrayList<TrackList.Track>();
				}
				currentPartName = thisDisc;
				currentPartType = Part.Type.DISC;
			} else if (currentPartLongName == null) {
				// No regular sides or discs. No section titles. Enough of this.
				return;
			}
			currentPart.add(t);
		}
		fullPartName = currentPartName;
		if (currentPartLongName != null) {
			fullPartName = currentPartLongName + " (" + currentPartName + ")";
		}
		parts.add(new Part(currentPartType, fullPartName, currentPart));
		
		mParts = parts;
	}

	public int getCount() {
		return mTracks.length;
	}

	public Object getItem(int idx) {
		return mTracks[idx];
	}

	public long getItemId(int idx) {
		return mTracks[idx].hashCode();
	}
	
	public List<Part> getParts () {
		return mParts;
	}
	
	public List<Track> getSelected () {
		ArrayList<Track> retval = new ArrayList<TrackList.Track>();
		for (Track t : mTracks) {
			if (t.isSelected()) {
				retval.add(t);
			}
		}
		return retval;
	}
	
	public List<Track> getTracks () {
		return new ArrayList<TrackList.Track>(Arrays.asList(mTracks));
	}

	public View getView(int idx, View view, ViewGroup parent) {
		if (view == null) {
			view = mInflater.inflate(R.layout.single_track, parent, false);
		}
		Track track = mTracks[idx];
		TextView position = (TextView) view.findViewById(R.id.track_no);
		TextView name = (TextView) view.findViewById(R.id.track_name);
		TextView duration = (TextView) view.findViewById(R.id.track_duration);
		CheckBox selected = (CheckBox) view.findViewById(R.id.track_selected);
		position.setText(track.getPosition());
		if (track.getPosition().length() == 0) {
			name.setTypeface(Typeface.DEFAULT_BOLD);
			selected.setVisibility(View.INVISIBLE);
		} else {
			name.setTypeface(Typeface.DEFAULT);
			selected.setVisibility(View.VISIBLE);
		}
		name.setText(track.getTitleWithArtists());
		duration.setText(track.getDuration());
		selected.setChecked(track.isSelected());
		return view;
	}
	
	public static class Track {
		private final String mDuration;
		private final String mTitle;
		private final String mPosition;
		private final List<ReleaseInfo.Credit> mArtists;
		private boolean mSelected;
		
		protected Track (String position, String title, String duration, List<ReleaseInfo.Credit> artists) {
			if (title == null) {
				throw new IllegalArgumentException("Track title must not be null");
			}
			mPosition = position;
			mTitle = title;
			mDuration = duration;
			if (artists != null) { 
				mArtists = artists;
			} else {
				mArtists = new ArrayList<ReleaseInfo.Credit>();
			}
			
			mSelected = false;
		}
		
		public String   getDuration () { return mDuration; }
		public String   getTitle () { return mTitle; }
		public List<ReleaseInfo.Credit> getArtists () { return new ArrayList<ReleaseInfo.Credit>(mArtists); }
		public String   getPosition () { return mPosition; }
		public String	getArtistString () {
			return ReleaseInfo.Credit.artistsString(mArtists);
		}
		public String   getTitleWithArtists () {
			if (mArtists.size() == 0) {
				return mTitle;
			} else {
				StringBuilder sb = new StringBuilder();
				sb.append(getArtistString());
				sb.append(" - ");
				sb.append(mTitle);
				return sb.toString();
			}
		}
		public int getDurationInSeconds () {
			Pattern p = Pattern.compile("([0-9]+):([0-9]+)");
			Matcher m = p.matcher(mDuration);
			if (m.matches()) {
				return Integer.parseInt(m.group(1)) * 60
						+ Integer.parseInt(m.group(2));
			} else {
				return 0;
			}
		}
		
		public void select () {
			select(true);
		}
		public void select (boolean select) {
			mSelected = select;
		}
		public boolean isSelected () {
			return mSelected;
		}
		public void toggleSelected () {
			mSelected = !mSelected;
		}
		
		public String toString () {
			if (mPosition != null) {
				return mPosition + ". " + mTitle;
			} else {
				return mTitle;
			}
		}
	}
	
	public static class Part {
		private final String mName;
		private final List<Track> mTracks;
		private final Part.Type mType;
		
		protected Part (Part.Type type, String name, List<Track> tracks) {
			mName = name;
			mTracks = tracks;
			mType = type;
		}
		
		public String getName () { return mName; }
		public List<Track> getTracks () { return mTracks; }
		public Part.Type getType () { return mType; }
		
		public String toString () {
			StringBuilder sb = new StringBuilder();
			switch (mType) {
			case SIDE:
				sb.append("Side ");
				break;
			case DISC:
				sb.append("Disc ");
				break;
			default:
				sb.append("Part ");
			}
			sb.append(mName);
			sb.append(": ");
			sb.append(mTracks.toString());
			
			return sb.toString();
		}
		
		public static enum Type { SIDE, DISC }
	}

}
