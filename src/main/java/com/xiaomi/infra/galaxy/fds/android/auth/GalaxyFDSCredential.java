package com.xiaomi.infra.galaxy.fds.android.auth;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;

import com.xiaomi.infra.galaxy.fds.android.exception.GalaxyFDSClientException;

/**
 * Credential information for FDS Client
 */
public interface GalaxyFDSCredential {

  /**
   * Add HTTP header to request
   * @param request
   */
  void addHeader(HttpRequestBase request) throws GalaxyFDSClientException;

  /**
   * Adds parameter to the URL string
   * @param uri
   * @return
   */
  String addParam(String uri);
}
