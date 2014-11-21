package com.xiaomi.infra.galaxy.fds.android.model;

public class StorageAccessToken {

  private String token;
  private long expireTime; // ms

  public StorageAccessToken() {}

  public StorageAccessToken(String token, long expireTime) {
    this.token = token;
    this.expireTime = expireTime;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public long getExpireTime() {
    return expireTime;
  }

  public void setExpireTime(long expireTime) {
    this.expireTime = expireTime;
  }
}
