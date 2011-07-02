package de.jollybox.vinylscrobbler;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

public final class ImageDownloader {
	/*
	 * Static in order to have a global cache that can be used in
	 * different activities
	 */
	private static Map<ImageView, DownloadTask> cDownloads = new WeakHashMap<ImageView, DownloadTask>();
	private static Map<String, DownloadTask> cUrlDownloads = new ConcurrentHashMap<String, DownloadTask>();
	// TODO: implement specialised cache class
	private static Map<String, SoftReference<Bitmap>> cCache = new ConcurrentHashMap<String, SoftReference<Bitmap>>();
	
	private Context mContext;
	
	public ImageDownloader(Context context) {
		mContext = context;
	}
	
	public void getBitmap(String url, ImageView img) {
		SoftReference<Bitmap> bmref;
		Bitmap bmp;
		if ((bmref = cCache.get(url)) != null) {
			if ((bmp = bmref.get()) != null) {
				cDownloads.remove(img);
				cancelDownload(cUrlDownloads.remove(url));
				img.setImageBitmap(bmp);
				return;
			} else {
				// has been garbage collected. Remove mapping.
				cCache.remove(url);
			}
		}
		
		DownloadTask dl = cUrlDownloads.get(url);
		if (dl == null) {
			dl = new DownloadTask();
			dl.mImg = img;
			dl.execute(url);
		} else {
			dl.mImg = img;
		}
		cDownloads.put(img, dl);
	}
	
	private static void cancelDownload (DownloadTask dl) {
		
	}
	
	private class DownloadTask extends AsyncTask<String, Void, Bitmap> {
		ImageView mImg;

		@Override
		protected Bitmap doInBackground(String... params) {
			String url = params[0];
			InputStream dataStream;
			Bitmap result;
			
			HttpUriRequest request = new HttpGet(url);
			try {
				dataStream = Helper.doRequest(mContext, request);
			} catch (IOException io_exc) {
				// TODO: handle.
				return null;
			}
			result = BitmapFactory.decodeStream(dataStream);
			// yay. Cache this.
			if (result != null) {
				cCache.put(url, new SoftReference<Bitmap>(result));
			}
			
			return result;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (cDownloads.get(mImg) == this) {
				// Okay! We haven't been replaced!
				mImg.setImageBitmap(result);
			}	
			
		}
		
	}
}