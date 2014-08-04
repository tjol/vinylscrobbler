package de.jollybox.vinylscrobbler;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import de.jollybox.vinylscrobbler.util.ImageDownloader;
import de.jollybox.vinylscrobbler.util.ReleaseInfo.ReleaseSummary;

public class ReleasesAdapter extends BaseAdapter {
	private final List<ReleaseSummary> mReleases;
	private List<ReleaseSummary> mFiltered;
	private final Context mContext;
	private final ImageDownloader mDownloader;

	public ReleasesAdapter(Context context, List<ReleaseSummary> releases) {
		mReleases = releases;
		mFiltered = new ArrayList<ReleaseSummary>(releases);
		mContext = context;
		mDownloader = new ImageDownloader(context);
	}

	public int getCount() {
		return mFiltered.size();
	}

	public Object getItem(int position) {
		return mFiltered.get(position);
	}

	public void ApplyFilter(String filter) {
		mFiltered.clear();
		for (ReleaseSummary r : mReleases) {
			String check = r.getTitle().toLowerCase();
			if (r.getArtist() != null)
				check += r.getArtist().toLowerCase();
			if (check.contains(filter.toLowerCase())) {
				mFiltered.add(r);
			}
		}
		notifyDataSetChanged();
	}

	public long getItemId(int position) {
		return getItem(position).hashCode();
	}

	public View getView(int position, View view, ViewGroup parent) {
		//if the parent is a grid, only present the thumbnail
		if (parent instanceof GridView) {
			SquareView img;
			if (view == null) {
				img = new SquareView(mContext);
				img.setScaleType(ImageView.ScaleType.CENTER_CROP);
			} else {
				img = (SquareView) view;
			}
			ReleaseSummary release = mFiltered.get(position);
			img.setImageBitmap(null);
			String thumbURI = release.getThumbURI();
			if (thumbURI != null) {
				// if it's part of the cached discogs collection, also search the db for the thumb
				if (release.isCached()) {
					mDownloader.getBitmap(thumbURI, img, true);
				} else {
					mDownloader.getBitmap(thumbURI, img);
				}
			}
			return img;
		} else {

			if (view == null) {
				LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.discogs_item, parent, false);
			}
			TextView title = (TextView) view.findViewById(R.id.item_title);
			TextView info1 = (TextView) view.findViewById(R.id.item_info1);
			TextView info2 = (TextView) view.findViewById(R.id.item_info2);
			ImageView img = (ImageView) view.findViewById(R.id.item_image);

			ReleaseSummary release = mFiltered.get(position);
			title.setText(release.getTitle());
			info2.setText("");
			if (release.isMaster()) {
				info1.setText(R.string.master_release);
			} else {
				String artist = release.getArtist();
				if (artist != null) {
					info1.setText(artist);
				} else {
					info1.setText(release.getFormat());
				}
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
				// if it's part of the cached discogs collection, also search the db for the thumb
				if (release.isCached()) {
					mDownloader.getBitmap(thumbURI, img, true);
				} else {
					mDownloader.getBitmap(thumbURI, img);
				}
			}

			return view;
		}
	}

	public static class ReleaseOpener implements OnItemClickListener {
		private final Context mContext;

		public ReleaseOpener(Context c) {
			mContext = c;
		}

		public void onItemClick(AdapterView<?> adapter_view, View item_view, int position, long hash) {
			ReleaseSummary release = (ReleaseSummary) adapter_view.getItemAtPosition(position);

			String type = "releases";
			if (release.isMaster()) {
				type = "masters";
			}
			int id = release.getId();
			Uri uri = (new Uri.Builder())
						.scheme("de.jollybox.vinylscrobbler")
						.authority("discogs")
						.appendPath(type)
						.appendPath(Integer.toString(id))
						.build();

			mContext.startActivity(new Intent(Intent.ACTION_VIEW, uri));
		}

	}

	//presents a square image, based on the given width
	private class SquareView extends ImageView {

		public SquareView(Context context) {
			super(context);
		}

		@Override
		public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			super.onMeasure(widthMeasureSpec, widthMeasureSpec);
		}

	}

}
