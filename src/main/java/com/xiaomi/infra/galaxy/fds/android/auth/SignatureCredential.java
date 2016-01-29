package com.xiaomi.infra.galaxy.fds.android.auth;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpRequestBase;

import com.xiaomi.infra.galaxy.fds.android.exception.GalaxyFDSClientException;
import com.xiaomi.infra.galaxy.fds.android.model.HttpHeaders;
import com.xiaomi.infra.galaxy.fds.signer.Common;
import com.xiaomi.infra.galaxy.fds.signer.HttpMethod;
import com.xiaomi.infra.galaxy.fds.signer.MultivaluedMap;
import com.xiaomi.infra.galaxy.fds.signer.SignAlgorithm;
import com.xiaomi.infra.galaxy.fds.signer.Signer;

public class SignatureCredential implements GalaxyFDSCredential {
  private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
      new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
          SimpleDateFormat format = new SimpleDateFormat(
              "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
          format.setTimeZone(TimeZone.getTimeZone("GMT"));
          return format;
        }
      };

  private final String accessKeyId;
  private final String secretAccessKeyId;

  public SignatureCredential(String accessKeyId, String secretAccessKeyId) {
    this.accessKeyId = accessKeyId;
    this.secretAccessKeyId = secretAccessKeyId;
  }

  @Override
  public void addHeader(HttpRequestBase request) throws GalaxyFDSClientException {
    request.setHeader(Common.DATE, DATE_FORMAT.get().format(new Date()));

    try {
      URI uri = request.getURI();
      MultivaluedMap httpHeaders = new MultivaluedMap();
      for (Header httpHeader : request.getAllHeaders()) {
        httpHeaders.add(httpHeader.getName(), httpHeader.getValue());
      }
      HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod());
      request.setHeader(HttpHeaders.AUTHORIZATION, Signer.getAuthorizationHeader(
          httpMethod, uri, httpHeaders, accessKeyId, secretAccessKeyId,
          SignAlgorithm.HmacSHA1));
    } catch (NoSuchAlgorithmException e) {
      throw new GalaxyFDSClientException("Fail to get signature for request:"
          + request, e);
    } catch (InvalidKeyException e) {
      throw new GalaxyFDSClientException("Fail to get signature for request:"
          + request, e);
    }
  }

  @Override
  public String addParam(String uri) {
    return uri;
  }
}
