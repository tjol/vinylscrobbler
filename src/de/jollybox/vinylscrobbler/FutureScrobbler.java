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

import de.jollybox.vinylscrobbler.util.Lastfm;
import de.jollybox.vinylscrobbler.util.TrackList;
import de.jollybox.vinylscrobbler.util.TrackList.Track;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;

public class FutureScrobbler extends IntentService
							  implements Lastfm.ResultWaiter, Lastfm.ErrorHandler {
	
	public FutureScrobbler() {
		super ("FutureScrobbler");
		
		//mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotification = null;
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extra_data = intent.getExtras();
		
		String releaseArtist = extra_data.getString("releaseArtist");
		String releaseTitle = extra_data.getString("releaseTitle");
		
		updateNotification (releaseTitle, releaseArtist);
		
		List<String> pathSegments = intent.getData().getPathSegments();
		
		if (pathSegments.get(0).equals("scrobbleTrack")) {
			
			Track scrobbleThis = extra_data.getParcelable("track");
			long when = extra_data.getLong("timeStamp");
			
			// Wait until the right time for scrobble.
			while (when > System.currentTimeMillis()) {
				synchronized (this) {
					try {
						wait (when - System.currentTimeMillis());
					} catch (Exception e) {
					}
				}
			}
			
			// Scrobble now.
			Lastfm lastfm = new Lastfm(this);
			List<Track> tracks = new ArrayList<TrackList.Track>(1);
			tracks.add(scrobbleThis);
			long[] times = { when / 1000 };
			lastfm.scrobbleTracks(tracks, times, releaseTitle,
					releaseArtist, this, this);
			
		} else if (pathSegments.get(0).equals("queueTracks")) {
			
			ArrayList<Track> tracks = extra_data.getParcelableArrayList("tracks");
			long timePointer = extra_data.getLong("startTime");
			
			Uri scrobbleIntentUri = new Uri.Builder()
							.scheme("de.jollybox.vinylscrobbler")
						    .authority("FutureScrobbler")
						    .appendPath("scrobbleTrack")
						    .build();
			
			// Go through all the tracks and queue their scrobbling
			for ( Track track : tracks ) {
				int length = track.getDurationInSeconds();
				if (length == 0) length = 60;
				timePointer += length * 1000;
				
				Intent scrobbleIntent = new Intent(Intent.ACTION_DEFAULT, scrobbleIntentUri);
				scrobbleIntent.putExtra("releaseArtist", releaseArtist);
				scrobbleIntent.putExtra("releaseTitle", releaseTitle);
				scrobbleIntent.putExtra("timeStamp", timePointer);
				scrobbleIntent.putExtra("track", track);
				startService(scrobbleIntent);
			}
		}
	}
	
	private void updateNotification (final String releaseTitle, final String releaseArtist) {
		Resources res = getResources();
		boolean notificationIsNew = false;
		
		if (mNotification == null) {
			notificationIsNew = true;
			mNotification = new Notification(R.drawable.ic_tab_tracks, 
									res.getString(R.string.now_scrobbling),
									System.currentTimeMillis());
		}
		
		PendingIntent contentIntent = PendingIntent.getActivity(this, 
																0, 
																new Intent(),
																PendingIntent.FLAG_UPDATE_CURRENT);
		
		mNotification.setLatestEventInfo(this, res.getString(R.string.now_scrobbling),
										 res.getString(R.string.album_by_artist, releaseTitle, releaseArtist),
										 contentIntent);
		mNotification.contentIntent = contentIntent;
		
		if (notificationIsNew) {
			startForeground(1, mNotification);
		}
	}

	public void errorMessage(String message) {
		// TODO Auto-generated method stub
		
	}

	public void onResult(Map<String, String> result) {
		// Cool. There's a result. I don't care (not in this class)
	}
	
	private Notification mNotification;
	//private NotificationManager mNotificationManager;
}
