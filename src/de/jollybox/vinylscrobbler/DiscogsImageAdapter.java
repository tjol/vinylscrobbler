package de.jollybox.vinylscrobbler;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class DiscogsImageAdapter extends BaseAdapter {
	
	protected final JSONArray mArr;
	protected final Context mContext;
	protected final ImageDownloader mDownloader;
	
	public DiscogsImageAdapter(Context context, JSONArray array) {
		mContext = context;
		mArr = array;
		mDownloader = new ImageDownloader(context);
	}
	
	public int getCount() {
		return mArr.length();
	}

	public Object getItem(int position) {
		try {
			return mArr.get(position);
		} catch (JSONException json_exc) {
			return null;
		}
	}

	public long getItemId(int position) {
		try {
			return mArr.getJSONObject(position).getString("uri").hashCode();
		} catch (JSONException json_exc) {
			return -1;
		}
	}

	public View getView(int position, View view, ViewGroup parent) {
		ImageView iv;
		if (view == null) {
			view = new ImageView(mContext);
		}
		iv = (ImageView) view;
		
		try {
			mDownloader.getBitmap(mArr.getJSONObject(position).getString("uri150"), iv);
		} catch (JSONException json_exc) {
			// Nothing I can do.
		}
		
		return view;
	}

}
