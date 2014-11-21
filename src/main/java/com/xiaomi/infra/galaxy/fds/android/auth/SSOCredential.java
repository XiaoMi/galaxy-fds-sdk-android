package com.xiaomi.infra.galaxy.fds.android.auth;

import org.apache.http.client.methods.HttpRequestBase;

import com.xiaomi.infra.galaxy.fds.android.model.HttpHeaders;
import com.xiaomi.infra.galaxy.fds.android.util.Args;

public class SSOCredential implements GalaxyFDSCredential {
  private final String HEADER_VALUE = "SSO";
  private final String SERVICE_TOKEN_PARAM = "serviceToken";

  private final String serviceToken;

  public SSOCredential(String serviceToken) {
    Args.notNull(serviceToken, "Service token");
    Args.notEmpty(serviceToken, "Service token");
    this.serviceToken = serviceToken;
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

    return builder.toString();
  }
}
