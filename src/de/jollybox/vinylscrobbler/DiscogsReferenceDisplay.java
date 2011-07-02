package de.jollybox.vinylscrobbler;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

/**
 * Views a de.jollybox.vinylscrobbler://discogs/[type]/[id] URI
 * 
 */
public class DiscogsReferenceDisplay extends TabActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Resources res = getResources();
		
		setContentView(R.layout.tabbed);
		TabHost tabhost = getTabHost();
		Intent inIntent = getIntent();
		
		String refType = inIntent.getData().getPathSegments().get(0);
		
		// template outgoing intent.
		// each tab processes the same URI, just with a different action.
		Intent outIntent = new Intent().addCategory(Intent.CATEGORY_TAB)
									   .setData(inIntent.getData());
		
		if (refType.equals("artist")) {	
			
			tabhost.addTab(tabhost.newTabSpec("info")
								  .setIndicator(res.getString(R.string.tab_artist_info),
										  	    res.getDrawable(R.drawable.ic_tab_artist))
								  .setContent(new Intent(outIntent).setAction("de.jollybox.vinylscrobbler.VIEW_INFO")));
			tabhost.addTab(tabhost.newTabSpec("releases")
								   .setIndicator(res.getString(R.string.tab_artist_releases),
										   		 res.getDrawable(R.drawable.ic_tab_releases))
					  			  .setContent(new Intent(outIntent).setAction("de.jollybox.vinylscrobbler.LIST_RELEASES")));
			tabhost.setCurrentTab(0);
			
		} else if (refType.equals("master")) {	
			
			tabhost.addTab(tabhost.newTabSpec("info")
					   			  .setIndicator(res.getString(R.string.tab_master_info),
					   					  	    res.getDrawable(R.drawable.ic_tab_record))
								  .setContent(new Intent(outIntent).setAction("de.jollybox.vinylscrobbler.VIEW_INFO")));
			tabhost.addTab(tabhost.newTabSpec("releases")
					   			  .setIndicator(res.getString(R.string.tab_master_versions),
									   		    res.getDrawable(R.drawable.ic_tab_releases))
								  .setContent(new Intent(outIntent).setAction("de.jollybox.vinylscrobbler.LIST_RELEASES")));
			tabhost.addTab(tabhost.newTabSpec("tracks")
								  .setIndicator(res.getString(R.string.tab_tracks),
										  		res.getDrawable(R.drawable.ic_tab_tracks))
								  .setContent(new Intent(outIntent).setAction("de.jollybox.vinylscrobbler.LIST_TRACKS")));
			tabhost.setCurrentTab(2);
			
		} else if (refType.equals("release")) {	
			
			tabhost.addTab(tabhost.newTabSpec("info")
								  .setIndicator(res.getString(R.string.tab_release_info),
										  		res.getDrawable(R.drawable.ic_tab_record))
								  .setContent(new Intent(outIntent).setAction("de.jollybox.vinylscrobbler.VIEW_INFO")));
			tabhost.addTab(tabhost.newTabSpec("tracks")
								  .setIndicator(res.getString(R.string.tab_tracks),
									   			res.getDrawable(R.drawable.ic_tab_tracks))
								  .setContent(new Intent(outIntent).setAction("de.jollybox.vinylscrobbler.LIST_TRACKS")));
			tabhost.setCurrentTab(1);
			
		}
	}

}
