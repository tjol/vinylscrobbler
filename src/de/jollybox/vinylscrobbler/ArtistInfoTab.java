/**
 * Vinyl Scrobbler app by JollyBOX.de
 *
 * Copyright (c) 2011	Thomas Jollans
 * 
 * Refer to the file COPYING for copying permissions.
 */

package de.jollybox.vinylscrobbler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.jollybox.vinylscrobbler.R;
import de.jollybox.vinylscrobbler.util.DiscogsQuery;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Gallery;
import android.widget.ListView;
import android.widget.TextView;

public class ArtistInfoTab extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.info);
		
		Intent intent = getIntent();
		String query_string = intent.getData().getEncodedPath();
		
		DiscogsQuery query = new DiscogsQuery.WithAlertDialog(this) {
			@Override
			protected void onResult(JSONObject result) {
				JSONObject artist;
				TextView title = (TextView) findViewById(R.id.title);
				Resources res = getResources();
				StringBuilder ht;
				int i;
				
				try {
					//artist = result.getJSONObject("resp").getJSONObject("artist");
					artist = result;
					
					title.setText(artist.getString("name"));
					
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
					
					if (artist.has("aliases")) {
						ht = new StringBuilder();
						ht.append(res.getString(R.string.info_aliases));
						JSONArray aliases = artist.getJSONArray("aliases");
						for (i = 0; i < aliases.length(); ++i) {
							if (i != 0) ht.append(",");
							String alias = aliases.getString(i);
							ht.append(String.format(" <a href=\"%s://%s/artist/%s\">%s</a>",
											res.getString(R.string.uri_scheme),
											res.getString(R.string.authority_discogs),
											Uri.encode(alias),
											alias));
						}
						items.add(Html.fromHtml(ht.toString()));
					}
					
					if (artist.has("members")) {
						ht = new StringBuilder();
						ht.append(res.getString(R.string.info_members));
						JSONArray members = artist.getJSONArray("members");
						for (i = 0; i < members.length(); ++i) {
							if (i != 0) ht.append(",");
							JSONObject member = members.getJSONObject(i);
							Uri member_uri = Uri.parse(member.getString("resource_url"));
							ht.append(String.format(" <a href=\"%s://%s/%s\">%s</a>",
									res.getString(R.string.uri_scheme),
									res.getString(R.string.authority_discogs),
									Uri.encode(member_uri.getEncodedPath()),
									member.getString("name")));
						}
						items.add(Html.fromHtml(ht.toString()));
					}
					
					if (artist.has("urls")) {
						ht = new StringBuilder();
						ht.append(res.getString(R.string.info_urls));
						JSONArray urls = artist.getJSONArray("urls");
						for (i = 0; i < urls.length(); ++i) {
							if (i != 0) ht.append(",");
							String url = urls.getString(i);
							ht.append(String.format(" <a href=\"%s\">%s</a>", url, url));
						}
						items.add(Html.fromHtml(ht.toString()));
					}
					
					if (artist.has("groups")) {
						ht = new StringBuilder();
						ht.append(res.getString(R.string.info_groups));
						JSONArray groups = artist.getJSONArray("groups");
						for (i = 0; i < groups.length(); ++i) {
							if (i != 0) ht.append(",");
							String group = groups.getString(i);
							ht.append(String.format(" <a href=\"%s://%s/artist/%s\">%s</a>",
									res.getString(R.string.uri_scheme),
									res.getString(R.string.authority_discogs),
									Uri.encode(group),
									group));
						}
						items.add(Html.fromHtml(ht.toString()));
					}
					
					ListView list = (ListView)findViewById(R.id.infos);
					list.setAdapter(items);
					list.setItemsCanFocus(true);
					
					if (artist.has("images")) {
						DiscogsImageAdapter gallery = new DiscogsImageAdapter(mContext,
															artist.getJSONArray("images"));
						Gallery gallery_widget = (Gallery)findViewById(R.id.images);
						gallery_widget.setAdapter(gallery);
					}
					
				} catch (JSONException json_exc) {
					title.setText(R.string.title_error);
					errorMessage(res.getString(R.string.error_invalid_data));
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
}
