package de.jollybox.vinylscrobbler;

import org.json.JSONArray;
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
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Gallery;
import android.widget.ListView;
import android.widget.TextView;

public class ReleaseInfoTab extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.info);
		
		Intent intent = getIntent();
		String query_string = intent.getData().getEncodedPath();
		
		DiscogsQuery query = new DiscogsQuery.WithAlertDialog(this) {
			@Override
			protected void onResult(JSONObject result) {
				JSONObject release;
				TextView title = (TextView) findViewById(R.id.title);
				StringBuilder ht;
				int i;
				
				try {
					JSONObject resp = result.getJSONObject("resp");
					if (resp.has("master")) {
						release = resp.getJSONObject("master");
						title.setText(release.getJSONArray("versions").getJSONObject(0).getString("title"));
					} else {
						release = resp.getJSONObject("release");
						title.setText(release.getString("title"));
					}
					
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
					
					if (release.has("artists")) {
						ht = new StringBuilder();
						ht.append(res.getString(R.string.info_artist));
						JSONArray artists = release.getJSONArray("artists");
						for (i = 0; i < artists.length(); ++i) {
							if (i != 0) ht.append(",");
							String artist = artists.getJSONObject(i).getString("name");
							ht.append(String.format(" <a href=\"%s://%s/artist/%s\">%s</a>",
									res.getString(R.string.uri_scheme),
									res.getString(R.string.authority_discogs),
									Uri.encode(artist),
									artist));
						}
						items.add(Html.fromHtml(ht.toString()));
					}
					
					if (release.has("genres")) {
						JSONArray genres = release.getJSONArray("genres");
						ht = new StringBuilder();
						ht.append(res.getString(R.string.info_genres) + " ");
						for (i = 0; i < genres.length(); ++i) {
							if (i != 0) ht.append(", ");
							String genre = genres.getString(i);
							ht.append(genre);
						}
						items.add(ht.toString());
					}
					
					if (release.has("styles")) {
						JSONArray styles = release.getJSONArray("styles");
						ht = new StringBuilder();
						ht.append(res.getString(R.string.info_styles) + " ");
						for (i = 0; i < styles.length(); ++i) {
							if (i != 0) ht.append(", ");
							String style = styles.getString(i);
							ht.append(style);
						}
						items.add(ht.toString());
					}
					
					
					if (release.has("released_formatted")) {
						items.add(res.getString(R.string.info_released) + " "
										+ release.getString("released_formatted"));
					} else if (release.has("year")) {
						items.add(res.getString(R.string.info_released) + " " 
										+ (new Integer(release.getInt("year"))).toString());
					}
					
					if (release.has("labels")) {
						JSONArray labels = release.getJSONArray("labels");
						
						ht = new StringBuilder();
						ht.append(res.getString(R.string.info_label) + " ");
						for (i = 0; i < labels.length(); ++i) {
							if (i != 0) ht.append(", ");
							String label = labels.getJSONObject(i).getString("name");
							ht.append(label);
						}
						items.add(ht.toString());
						
						ht = new StringBuilder();
						ht.append(res.getString(R.string.info_catno) + " ");
						for (i = 0; i < labels.length(); ++i) {
							if (i != 0) ht.append(", ");
							String catno = labels.getJSONObject(i).getString("catno");
							ht.append(catno);
						}
						items.add(ht.toString());
					}
					
					if (release.has("country")) {
						items.add(res.getString(R.string.info_country) + " " + release.getString("country"));
					}
					
					if (release.has("extraartists")) {
						// TODO!
					}
					
					if (release.has("notes")) {
						items.add(release.getString("notes"));
					}
					
					if (release.has("master_id")) {
						items.add(Html.fromHtml(String.format("<a href=\"%s://%s/master/%s\">%s</a>",
								res.getString(R.string.uri_scheme),
								res.getString(R.string.authority_discogs),
								release.getString("master_id"),
								res.getString(R.string.show_master_release)
								)));
					}
					
					ListView list = (ListView)findViewById(R.id.infos);
					list.setAdapter(items);
					list.setItemsCanFocus(true);
					
					if (release.has("images")) {
						DiscogsImageAdapter gallery = new DiscogsImageAdapter(mContext,
															release.getJSONArray("images"));
						Gallery gallery_widget = (Gallery)findViewById(R.id.images);
						gallery_widget.setAdapter(gallery);
					}
					
				} catch (JSONException json_exc) {
					title.setText("Error");
					errorMessage("Cannot comprehend data");
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
