/**
 * Vinyl Scrobbler app by JollyBOX.de
 *
 * Copyright (c) 2011	Thomas Jollans
 * 
 * Refer to the file COPYING for copying permissions.
 */

package de.jollybox.vinylscrobbler;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ListView;
import android.widget.TextView;
import de.jollybox.vinylscrobbler.util.Discogs;
import de.jollybox.vinylscrobbler.util.DiscogsQuery;
import de.jollybox.vinylscrobbler.util.ReleaseInfo;

public class ReleaseInfoTab extends Activity {
	private Discogs mDiscogs;
	private int mReleaseId = -1;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.info);
		
		//when logged in to discogs -> add option to add this release to the collection
		mDiscogs = new Discogs(this);
		if(mDiscogs.getUser() != null) {
			Button discogsColl = (Button) findViewById(R.id.discogs_add);
			discogsColl.setVisibility(View.VISIBLE);
			discogsColl.setOnClickListener(discogsAddListener);
		}
		
		
		Intent intent = getIntent();
		String query_string = intent.getData().getEncodedPath();
		
		DiscogsQuery query = new DiscogsQuery.WithAlertDialog(this) {
			@Override
			protected void onResult(JSONObject result) {
				//JSONObject release;
				ReleaseInfo release;
				TextView title = (TextView) findViewById(R.id.title);
				StringBuilder ht;
				
				try {
					release = ReleaseInfo.fromJSON(mContext, result);
				} catch (JSONException json_exc) {
					title.setText(R.string.title_error);
					errorMessage(res.getString(R.string.error_invalid_data));
					return;
				}
				
				title.setText(release.getTitle());
				//check for main version id, defaults to release id if it is not a master release
				mReleaseId = release.getMainVersionId();
				
				ArrayAdapter<CharSequence> items = new ArrayAdapter<CharSequence>(mContext, R.layout.textlistitem) {
					@Override
					public View getView(int position, View v, ViewGroup parent) {
						if (v == null) {
							v = getLayoutInflater().inflate(R.layout.textlistitem, parent, false);
						}
						TextView tv = (TextView)v;
						tv.setMovementMethod(LinkMovementMethod.getInstance());
						tv.setText(getItem(position));
						return v;
					}
				};
				
				List<ReleaseInfo.Credit> artists = release.getArtists();
				if (artists.size() > 0) {
					ht = new StringBuilder();
					ht.append(res.getString(R.string.info_artist) + " ");
					boolean first = true;
					for (ReleaseInfo.Credit artist : artists) {
						if (first) first = false;
						else ht.append(", ");

						ht.append(String.format(" <a href=\"%s://%s/artists/%d\">%s</a>",
								res.getString(R.string.uri_scheme),
								res.getString(R.string.authority_discogs),
								artist.getId(),
								artist.getArtist()));
					}
					items.add(Html.fromHtml(ht.toString()));
				}
				
				if (!release.isMaster()) {
					items.add(String.format("%s %s", res.getString(R.string.info_format),
													 release.getFormatString()));
				}
				
				List<String> genres = release.getGenres();
				if (genres.size() > 0) {
					ht = new StringBuilder();
					ht.append(res.getString(R.string.info_genres) + " ");
					boolean first = true;
					for (String genre : genres) {
						if (first) first = false;
						else ht.append(", ");
						
						ht.append(genre);
					}
					items.add(ht.toString());
				}
				
				List<String> styles = release.getStyles();
				if (styles.size() > 0) {
					ht = new StringBuilder();
					ht.append(res.getString(R.string.info_styles) + " ");
					boolean first = true;
					for (String style : styles) {
						if (first) first = false;
						else ht.append(", ");
						
						ht.append(style);
					}
					items.add(ht.toString());
				}
				
				String when = release.getDateString();
				if (when != null) {
					items.add(res.getString(R.string.info_released) + " " + when);
				}
				
				List<ReleaseInfo.CatalogEntry> labels = release.getCatalogEntries();
				if (labels.size() > 0) {
					StringBuilder ht_l = new StringBuilder();
					ht_l.append(res.getString(R.string.info_label) + " ");
					StringBuilder ht_n = new StringBuilder();
					ht_n.append(res.getString(R.string.info_catno) + " ");
					
					boolean first = true;
					for (ReleaseInfo.CatalogEntry e : labels) {
						if (first) first = false;
						else {
							ht_l.append(", ");
							ht_n.append(", ");
						}
						
						ht_l.append(e.getLabel());
						ht_n.append(e.getCatalogNumber());
					}
					
					items.add(ht_l.toString());
					items.add(ht_n.toString());
				}
				
				String country, notes;
				if ((country = release.getCountry()) != null) {
					items.add(res.getString(R.string.info_country) + " " + country);
				}
				
				List<ReleaseInfo.Credit> extraArtists = release.getExtraArtists();
				for (ReleaseInfo.Credit credit : extraArtists) {
					String creditHtml = String.format("%s: <a href=\"%s://%s/artists/%s\">%s</a>",
											credit.getRole(),
											res.getString(R.string.uri_scheme),
											res.getString(R.string.authority_discogs),
											Uri.encode(credit.getCanonicalArtistName()),
											credit.getArtist());
					items.add(Html.fromHtml(creditHtml));
				}
				
				if ((notes = release.getNotes()) != null) {
					items.add(notes);
				}
				
				if (!release.isMaster() && release.getMasterId() != -1) {
					items.add(Html.fromHtml(String.format("<a href=\"%s://%s/masters/%s\">%s</a>",
							res.getString(R.string.uri_scheme),
							res.getString(R.string.authority_discogs),
							release.getMasterId(),
							res.getString(R.string.show_master_release)
							)));
				}
				
				ListView list = (ListView)findViewById(R.id.infos);
				list.setAdapter(items);
				list.setItemsCanFocus(true);
				
				DiscogsImageAdapter gallery = release.getGallery();
				if (gallery != null) {
					Gallery gallery_widget = (Gallery)findViewById(R.id.images);
					gallery_widget.setAdapter(gallery);
				}
			}
		};
		
		query.execute(query_string);
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
	
	private OnClickListener discogsAddListener = new OnClickListener() {
		
		public void onClick(View v) {
			if(mReleaseId != -1) {
				mDiscogs.addRelease(mReleaseId);
			}
		}
	};
}
