package com.xiaomi.infra.galaxy.fds.android.auth;

import org.apache.http.client.methods.HttpRequestBase;

import com.xiaomi.infra.galaxy.fds.android.model.HttpHeaders;
import com.xiaomi.infra.galaxy.fds.android.util.Args;

public class SSOCredential implements GalaxyFDSCredential {
  private final String HEADER_VALUE = "SSO";
  private final String SERVICE_TOKEN_PARAM = "serviceToken";
  private final String APP_ID = "appId";

  private final String serviceToken;
  private final String appId;

  @Deprecated
  public SSOCredential(String serviceToken) {
    Args.notNull(serviceToken, "Service token");
    Args.notEmpty(serviceToken, "Service token");
    this.serviceToken = serviceToken;
    this.appId = null;
  }

  public SSOCredential(String serviceToken, String appId) {
    Args.notNull(serviceToken, "Service token");
    Args.notEmpty(serviceToken, "Service token");
    Args.notNull(appId, "App id");
    Args.notEmpty(appId, "App id");
    this.serviceToken = serviceToken;
    this.appId = appId;
  }

  @Override
  public void addHeader(HttpRequestBase request) {
    request.addHeader(HttpHeaders.AUTHORIZATION, HEADER_VALUE);
  }

  @Override
  public String addParam(String uri) {
    StringBuilder builder = new StringBuilder(uri);
    if (uri.indexOf('?') == -1) {
      builder.append('?');
    } else {
      builder.append('&');
    }
    builder.append(SERVICE_TOKEN_PARAM);
    builder.append('=');
    builder.append(serviceToken);

    if (appId != null) {
      builder.append('&');
      builder.append(APP_ID);
      builder.append('=');
      builder.append(appId);
    }

    return builder.toString();
  }
}
