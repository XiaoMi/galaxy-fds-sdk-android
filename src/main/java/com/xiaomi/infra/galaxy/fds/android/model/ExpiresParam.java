package com.xiaomi.infra.galaxy.fds.android.model;

public class ExpiresParam extends UserParam {
  private static final String EXPIRES = "expires";

  /**
   * @param expiresMs Expire timestamp in milliseconds
   */
  public ExpiresParam(long expiresMs) {
    params.put(EXPIRES, Long.toString(expiresMs));
  }
}
