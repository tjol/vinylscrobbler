/**
 * Vinyl Scrobbler app by JollyBOX.de
 *
 * Copyright (c) 2011	Thomas Jollans
 * 
 * Refer to the file COPYING for copying permissions.
 */

package de.jollybox.vinylscrobbler;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.res.Resources;

public class Helper {
	public static InputStream doRequest (Context context, HttpUriRequest request)
										throws IOException {
		Resources res = context.getResources();
		request.addHeader("User-Agent", res.getString(R.string.user_agent));
		request.addHeader("Accept-Encoding", "gzip");
		HttpClient http = new DefaultHttpClient(request.getParams());
		HttpResponse resp = http.execute(request);
		HttpEntity entity = resp.getEntity();
		InputStream stream = entity.getContent();
		Header encoding_header;
		// TODO: check all headers and header elements
		if ((encoding_header = resp.getFirstHeader("Content-Encoding")) != null &&
			 encoding_header.getValue().equalsIgnoreCase("gzip")) {
			stream = new GZIPInputStream(stream);
		}
		return stream;
	}
	
	public static String hexMD5 (String input) {
		MessageDigest digester;
		byte[] bInput;
		try {
			digester = MessageDigest.getInstance("MD5");
			bInput = input.getBytes("UTF-8");
		} catch (Exception notgonnahappen) { // NoSuchAlgorithmException|UnsupportedEncodingException
			throw new RuntimeException(notgonnahappen);
		}
		
		byte[] bOutput = digester.digest(bInput);
		StringBuilder hexOutputBuilder = new StringBuilder(bOutput.length * 2);
		for (byte b : bOutput) {
			hexOutputBuilder.append(Integer.toHexString((0xFF & b)|0x100).substring(1, 3));
		}
		
		return hexOutputBuilder.toString();
	}
	
	public static String removeNumberFromArtist (String artist) {
		Pattern numberExp = Pattern.compile("^(.*?)( \\([0-9]+\\))?$");
		Matcher m = numberExp.matcher(artist);
		if (m.matches()) {
			return m.group(1);
		} else {
			return artist;
		}
	}
}
