package com.xiaomi.infra.galaxy.fds.android.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.xiaomi.infra.galaxy.fds.android.model.ObjectMetadata;
import com.xiaomi.infra.galaxy.fds.android.model.ProgressListener;

public class ObjectInputStream extends FilterInputStream {
  private final ProgressListener listener;
  private final ObjectMetadata metadata;
  private long lastNotifyTime;
  private long totalBytesRead;

  public ObjectInputStream(InputStream in, ObjectMetadata metadata,
      ProgressListener listener) {
    super(in);
    this.metadata = metadata;
    this.listener = listener;
  }

  private void notifyListener(boolean needsCheckTime) {
    if (listener != null) {
      long now = System.currentTimeMillis();
      if (!needsCheckTime || now - lastNotifyTime >= listener.progressInterval()) {
        lastNotifyTime = now;
        listener.onProgress(totalBytesRead, metadata.getContentLength());
      }
    }
  }

  @Override
  public int read(byte[] buffer, int byteOffset, int byteCount)
      throws IOException {
    int bytesRead = super.read(buffer, byteOffset, byteCount);
    if (bytesRead != -1) {
      totalBytesRead += bytesRead;
      notifyListener(true);
    }
    return bytesRead;
  }


  @Override
  public int read() throws IOException {
    int data = super.read();
    if (data != -1) {
      totalBytesRead++;
      notifyListener(true);
    }
    return data;
  }

  @Override
  public synchronized void reset() throws IOException {
    super.reset();
    totalBytesRead = 0;
    notifyListener(true);
  }

  @Override
  public void close() throws IOException {
    super.close();
    notifyListener(false);
  }
}
