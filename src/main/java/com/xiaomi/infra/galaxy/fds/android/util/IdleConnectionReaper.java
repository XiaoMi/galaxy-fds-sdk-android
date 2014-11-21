package com.xiaomi.infra.galaxy.fds.android.util;

import java.util.concurrent.TimeUnit;

import org.apache.http.conn.ClientConnectionManager;

/**
 * Daemon thread to periodically check connection pools for idle connections.
 */
public final class IdleConnectionReaper extends Thread {

  private static final int PERIOD_MILLISECONDS = 1000 * 60 * 1;

  private final ClientConnectionManager connectionManager;

  public IdleConnectionReaper(ClientConnectionManager connectionManager) {
    super("java-sdk-http-connection-reaper");
    this.connectionManager = connectionManager;
    setDaemon(true);
  }

  @Override
  public void run() {
    while (true) {
      try {
        Thread.sleep(PERIOD_MILLISECONDS);
        connectionManager.closeIdleConnections(60, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }
}
