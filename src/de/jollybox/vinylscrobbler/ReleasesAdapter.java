package de.jollybox.vinylscrobbler;

import java.util.List;

import de.jollybox.vinylscrobbler.util.ImageDownloader;
import de.jollybox.vinylscrobbler.util.ReleaseInfo.ReleaseSummary;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ReleasesAdapter extends BaseAdapter {
	private final List<ReleaseSummary> mReleases;
	private final Context mContext;
	private final ImageDownloader mDownloader;
	
	public ReleasesAdapter(Context context, List<ReleaseSummary> releases) {
		mReleases = releases;
		mContext = context;
		mDownloader = new ImageDownloader(context);
	}

	@Override
	public int getCount() {
		return mReleases.size();
	}

	@Override
	public Object getItem(int position) {
		return mReleases.get(position);
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).hashCode();
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater)
				mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.discogs_item, parent, false);
		}
		TextView title = (TextView) view.findViewById(R.id.item_title);
		TextView info1 = (TextView) view.findViewById(R.id.item_info1);
		TextView info2 = (TextView) view.findViewById(R.id.item_info2);
		ImageView img = (ImageView) view.findViewById(R.id.item_image);

		ReleaseSummary release = mReleases.get(position);
		title.setText(release.getTitle());
		info2.setText("");
		if (release.isMaster()) {
			info1.setText(R.string.master_release);
		} else {
			info1.setText(release.getFormat());
			String label = release.getLabel();
			String country = release.getCountry();
			if (label != null && country != null) {
				info2.setText(country + ": " + label);
			} else if (label != null) {
				info2.setText(label);
			} else if (country != null) {
				info2.setText(country);
			}
		}
		img.setImageBitmap(null);
		String thumbURI = release.getThumbURI();
		if (thumbURI != null) {
			mDownloader.getBitmap(thumbURI, img);
		}

		return view;
	}
	
	public static class ReleaseOpener implements OnItemClickListener {
		private final Context mContext;
		
		public ReleaseOpener(Context c) {
			mContext = c;
		}

		public void onItemClick(AdapterView<?> adapter_view, View item_view, int position, long hash) {
			ReleaseSummary release = (ReleaseSummary) adapter_view.getItemAtPosition(position);
		
			String type = "release";
			if (release.isMaster()) {
				type = "master";
			}
			int id = release.getId();
			Uri uri = (new Uri.Builder()).scheme("de.jollybox.vinylscrobbler")
										 .authority("discogs")
										 .appendPath(type)
										 .appendPath(Integer.toString(id))
										 .build();
			
			mContext.startActivity(new Intent(Intent.ACTION_VIEW, uri));
		}
		
	}

}
