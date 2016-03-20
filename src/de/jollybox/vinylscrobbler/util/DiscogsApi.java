package de.jollybox.vinylscrobbler.util;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;

public class DiscogsApi extends DefaultApi10a {

	private static final String AUTHORIZATION_URL = "https://www.discogs.com/oauth/authorize?oauth_token=%s";
	private static final String BASE_URL = "https://api.discogs.com/oauth/";

	@Override
	public String getRequestTokenEndpoint() {
		return BASE_URL + "request_token";
	}

	@Override
	public String getAccessTokenEndpoint() {
		return BASE_URL + "access_token";
	}

	@Override
	public String getAuthorizationUrl(Token requestToken) {
		return String.format(AUTHORIZATION_URL, requestToken.getToken());
	}

}
