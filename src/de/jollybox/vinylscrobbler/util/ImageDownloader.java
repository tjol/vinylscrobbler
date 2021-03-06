/**
 * Vinyl Scrobbler app by JollyBOX.de
 *
 * Copyright (c) 2011	Thomas Jollans
 * 
 * Refer to the file COPYING for copying permissions.
 */

package de.jollybox.vinylscrobbler.util;

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
	 * Static in order to have a global cache that can be used in different
	 * activities
	 */
	private static Map<ImageView, DownloadTask> cDownloads = new WeakHashMap<ImageView, DownloadTask>();
	private static Map<String, DownloadTask> cUrlDownloads = new ConcurrentHashMap<String, DownloadTask>();
	private static Map<String, SoftReference<Bitmap>> cCache = new ConcurrentHashMap<String, SoftReference<Bitmap>>();

	private Context mContext;

	public ImageDownloader(Context context) {
		mContext = context;
	}

	public void getBitmap(String url, ImageView img) {
		getBitmap(url, img, false);
	}

	public void getBitmap(String url, ImageView img, boolean localThumb) {
		SoftReference<Bitmap> bmref;
		Bitmap bmp;
		
		//check for cached thumb
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
		
		//check for thumb in database
		if(localThumb) {
			bmp = VinylDatabase.getInstance(mContext).getThumb(url);
			if (bmp != null) {
				// thumb was found in db, use this one and cache it
				cCache.put(url, new SoftReference<Bitmap>(bmp));
				img.setImageBitmap(bmp);
				return;
			}
		}
		
		//not found in local resources, to the internets!
		DownloadTask dl = cUrlDownloads.get(url);
		if (dl == null) {
			dl = new DownloadTask();
			dl.mImg = img;
			dl.mStoreInDb = localThumb;
			try{
				dl.execute(url);
			} catch(Exception e) {
				//could happen if there are too many threads open
			}
		} else {
			dl.mImg = img;
			dl.mStoreInDb = localThumb;
		}
		cDownloads.put(img, dl);
	}

	private static void cancelDownload(DownloadTask dl) {

	}

	private class DownloadTask extends AsyncTask<String, Void, Bitmap> {
		ImageView mImg;
		String mThumbUri;
		boolean mStoreInDb = false;

		@Override
		protected Bitmap doInBackground(String... params) {
			String url = params[0];
			mThumbUri = url;
			InputStream dataStream;
			Bitmap result;

			HttpUriRequest request = new HttpGet(url);
			try {
				dataStream = Helper.doRequest(mContext, request);
			} catch (Exception io_exc) {
				//catches broken network and missing thumbnails
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
				// check if we need to store this result in the db
				if (mStoreInDb && result != null) {
					VinylDatabase.getInstance(mContext).storeThumb(mThumbUri, result);
				}
			}

		}

	}
}