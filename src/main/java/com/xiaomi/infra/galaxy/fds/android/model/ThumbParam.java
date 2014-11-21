package com.xiaomi.infra.galaxy.fds.android.model;

/**
 * User defined parameters for get thumbnail
 */
public class ThumbParam extends UserParam {
  /**
   * @param width The width of thumbnail
   * @param height The height of thumbnail
   */
  public ThumbParam(int width, int height) {
    params.put("thumb", "1");
    params.put("w", Integer.toString(width));
    params.put("h", Integer.toString(height));
  }
}
