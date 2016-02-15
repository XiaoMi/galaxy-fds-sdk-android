package com.xiaomi.infra.galaxy.fds.android.util;

import java.util.Date;
import java.util.Map;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;

import com.xiaomi.infra.galaxy.fds.android.auth.GalaxyFDSCredential;
import com.xiaomi.infra.galaxy.fds.android.exception.GalaxyFDSClientException;
import com.xiaomi.infra.galaxy.fds.android.model.HttpHeaders;
import com.xiaomi.infra.galaxy.fds.android.model.HttpMethod;

public class RequestFactory {
  public static HttpUriRequest createRequest(String uri,
      GalaxyFDSCredential credential, HttpMethod method,
      Map<String, String> headers) throws GalaxyFDSClientException {
    uri = credential.addParam(uri);
    HttpRequestBase request;
    switch(method) {
      case GET:
        request = new HttpGet(uri);
        break;
      case PUT:
        request = new HttpPut(uri);
        break;
      case POST:
        request = new HttpPost(uri);
        break;
      case DELETE:
        request = new HttpDelete(uri);
        break;
      case HEAD:
        request = new HttpHead(uri);
        break;
      default:
        request = null;
        break;
    }

    if (request != null) {
      if (headers != null) {
        // Should not set content length here, otherwise the fucking apache
        // library will throw an exception
        headers.remove(HttpHeaders.CONTENT_LENGTH);
        headers.remove(HttpHeaders.CONTENT_LENGTH.toLowerCase());
        for (Map.Entry<String, String> header : headers.entrySet()) {
          request.addHeader(header.getKey(), header.getValue());
        }
      }
      // Add date header
      request.addHeader(HttpHeaders.DATE, Util.formatDateString(new Date()));
      credential.addHeader(request);
    }
    return request;
  }
}
