package com.xiaomi.infra.galaxy.fds.android.util;

public final class Consts {
  public static final String XIAOMI_HEADER_PREFIX = "x-xiaomi-";
  public static final String XIAOMI_META_HEADER_PREFIX = XIAOMI_HEADER_PREFIX
      + "meta-";
  public static final String ESTIMATED_OBJECT_SIZE = XIAOMI_HEADER_PREFIX
      + "estimated-object-size";
  public static final String GALAXY_ACCESS_KEY_ID = "GalaxyAccessKeyId";
  public static final String SIGNATURE = "Signature";
  public static final String EXPIRES = "Expires";
  public static String APPLICATION_OCTET_STREAM = "application/octet-stream";
}
