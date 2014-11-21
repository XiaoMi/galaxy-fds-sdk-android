package com.xiaomi.infra.galaxy.fds.android.auth;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;

import com.xiaomi.infra.galaxy.fds.android.exception.GalaxyFDSClientException;
import com.xiaomi.infra.galaxy.fds.android.model.HttpHeaders;
import com.xiaomi.infra.galaxy.fds.android.model.StorageAccessToken;

public class OAuthCredential implements GalaxyFDSCredential {
  private static final String STORAGE_ACCESS_TOKEN = "storageAccessToken";
  private static final String APP_ID = "appId";
  private static final String OAUTH_APPID = "oauthAppId";
  private static final String OAUTH_ACCESS_TOKEN = "oauthAccessToken";
  private static final String OAUTH_PROVIDER = "oauthProvider";
  private static final String OAUTH_MAC_KEY = "oauthMacKey";
  private static final String OAUTH_MAC_ALGORITHM = "oauthMacAlgorithm";
  private final String HEADER_VALUE = "OAuth";

  private final String appId;
  private final StorageAccessToken storageAccessToken;

  public OAuthCredential(String fdsServiceBaseUri, String appId,
      String oauthAppId, String oauthAccessToken, String oauthProvider,
      String macKey, String macAlgorithm) throws GalaxyFDSClientException {
    this.appId = appId;
    this.storageAccessToken = getStorageAccessToken(fdsServiceBaseUri, appId,
        oauthAppId, oauthAccessToken, oauthProvider, macKey, macAlgorithm);
  }

  private StorageAccessToken getStorageAccessToken(String serviceBaseUri,
      String appId, String oauthAppId, String oauthAccessToken,
      String oauthProvider, String macKey, String macAlgorithm)
      throws GalaxyFDSClientException {
    try {
      HttpClient httpClient = new DefaultHttpClient();
      HttpGet get = new HttpGet(serviceBaseUri + "/?" + STORAGE_ACCESS_TOKEN
          + "&" + APP_ID + "=" + appId
          + "&" + OAUTH_APPID + "=" + oauthAppId
          + "&" + OAUTH_ACCESS_TOKEN + "=" + oauthAccessToken
          + "&" + OAUTH_PROVIDER + "=" + oauthProvider
          + "&" + OAUTH_MAC_ALGORITHM + "=" + macAlgorithm
          + "&" + OAUTH_MAC_KEY + "=" + macKey);
      get.setHeader(HttpHeaders.AUTHORIZATION, HEADER_VALUE);
      HttpResponse response = httpClient.execute(get);
      if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
        throw new GalaxyFDSClientException("Failed to get the storage access "
            + "token from FDS server. URI:" + get.getURI().toString()
            + ".Reason:" + response.getStatusLine().toString());
      }

      InputStream in = response.getEntity().getContent();
      Reader reader = new InputStreamReader(in);
      StorageAccessToken token = new Gson().fromJson(reader,
          StorageAccessToken.class);
      in.close();
      return token;
    } catch (IOException e) {
      throw new GalaxyFDSClientException("Failed to get the storage access "
          + "token", e);
    }
  }

  @Override
  public void addHeader(HttpRequestBase request) {
    request.setHeader(HttpHeaders.AUTHORIZATION, HEADER_VALUE);
  }

  @Override
  public String addParam(String uri) {
    StringBuilder builder = new StringBuilder(uri);
    if (uri.indexOf('?') == -1) {
      builder.append('?');
    } else {
      builder.append('&');
    }
    builder.append(APP_ID);
    builder.append('=');
    builder.append(this.appId);
    builder.append('&');
    builder.append(STORAGE_ACCESS_TOKEN);
    builder.append('=');
    builder.append(this.storageAccessToken.getToken());

    return builder.toString();
  }
}
