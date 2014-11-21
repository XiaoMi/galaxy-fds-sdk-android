package com.xiaomi.infra.galaxy.fds.android.model;

/**
 * Listener interface for transfer progress events. The user of Galaxy FDS
 * client should implement the abstract method by himself.
 */
public abstract class ProgressListener {

  /**
   * Called when some bytes have been transferred since the last time it was
   * called and the progress interval has passed
   *
   * @param transferred
   *            The number of bytes transferred.
   * @param total
   *            The size of the object in bytes
   *
   */
  public abstract void onProgress(long transferred, long total);

  /**
   * Should return how often transferred bytes should be reported to this
   * listener, in milliseconds. It is not guaranteed that updates will happen
   * at this exact interval, but that at least this amount of time will pass
   * between updates. The default implementation always returns 500 milliseconds.
   */
  public long progressInterval() {
    return 500;
  }
}
