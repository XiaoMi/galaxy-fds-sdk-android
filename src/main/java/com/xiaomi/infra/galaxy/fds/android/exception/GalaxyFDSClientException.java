package com.xiaomi.infra.galaxy.fds.android.exception;

/**
 * Base exception class for any errors that occur while attempting to use Galaxy
 * FDS client to make service calls to Galaxy FDS service.
 */
public class GalaxyFDSClientException extends Exception {

  public GalaxyFDSClientException() {}

  public GalaxyFDSClientException(String message) {
    super(message);
  }

  public GalaxyFDSClientException(Throwable cause) {
    super(cause);
  }

  public GalaxyFDSClientException(String message, Throwable cause) {
    super(message, cause);
  }
}
