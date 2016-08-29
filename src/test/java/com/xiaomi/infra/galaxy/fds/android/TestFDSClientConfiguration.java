package com.xiaomi.infra.galaxy.fds.android;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by zhangjunbin on 12/29/14.
 */
public class TestFDSClientConfiguration {
  private static final String URI_SUFFIX = "fds.api.xiaomi.com";
  private static final String URI_CDN_SUFFIX = "fds.api.mi-img.com";

  @Test
  public void testDefaultConfigurationValue() {
    FDSClientConfiguration conf = new FDSClientConfiguration();
    Assert.assertEquals("cnbj0", conf.getRegionName());
    Assert.assertEquals(true, conf.isHttpsEnabled());
    Assert.assertEquals(false, conf.isCdnEnabledForUpload());
    Assert.assertEquals(true, conf.isCdnEnabledForDownload());
    Assert.assertEquals(false, conf.isEnabledUnitTestMode());
  }

  @Test
  public void testCdnChosen() {
    FDSClientConfiguration fdsConfig = new FDSClientConfiguration();
    fdsConfig.setRegionName("regionName");
    fdsConfig.enableHttps(true);

    // Test flag enableCdnForUpload.
    fdsConfig.enableCdnForUpload(false);
    Assert.assertEquals(fdsConfig.getUploadBaseUri(),
        "https://regionName."  + URI_SUFFIX);
    fdsConfig.enableCdnForUpload(true);
    Assert.assertEquals(fdsConfig.getUploadBaseUri(),
        "https://cdn.regionName." + URI_CDN_SUFFIX);
    fdsConfig.enableHttps(false);
    Assert.assertEquals(fdsConfig.getUploadBaseUri(),
        "http://cdn.regionName." + URI_CDN_SUFFIX);

    // Test flag enableCdnForDownload.
    fdsConfig.enableCdnForDownload(false);
    Assert.assertEquals(fdsConfig.getDownloadBaseUri(),
        "http://regionName." + URI_SUFFIX);
    fdsConfig.enableCdnForDownload(true);
    Assert.assertEquals(fdsConfig.getDownloadBaseUri(),
        "http://cdn.regionName." + URI_CDN_SUFFIX);
    fdsConfig.enableHttps(true);
    Assert.assertEquals(fdsConfig.getDownloadBaseUri(),
        "https://cdn.regionName." + URI_CDN_SUFFIX);
  }

  @Test
  public void testBuildBaseUri() {
    final String regionNameA = "regionNameA";
    final String regionNameB = "regionNameB";
    FDSClientConfiguration fdsConfig = new FDSClientConfiguration();

    // Test against flag enable https.
    fdsConfig.setRegionName(regionNameA);
    fdsConfig.enableHttps(true);
    Assert.assertEquals("https://" + regionNameA + "." + URI_SUFFIX,
        fdsConfig.buildBaseUri(false));
    fdsConfig.enableHttps(false);
    Assert.assertEquals("http://" + regionNameA + "." +  URI_SUFFIX,
        fdsConfig.buildBaseUri(false));

    // Test against region name.
    fdsConfig.setRegionName(regionNameB);
    fdsConfig.enableHttps(true);
    Assert.assertEquals("https://" + regionNameB + "." +
        URI_SUFFIX, fdsConfig.buildBaseUri(false));

    // Test setting endpoint
    String endpoint = "cnbj0.fds.api.xiaomi.net";
    fdsConfig.setEndpoint(endpoint);
    fdsConfig.enableHttps(false);
    Assert.assertEquals("http://" + endpoint, fdsConfig.buildBaseUri(false));
    Assert.assertEquals("http://" + endpoint, fdsConfig.buildBaseUri(true));
    fdsConfig.enableHttps(true);
    Assert.assertEquals("https://" + endpoint, fdsConfig.buildBaseUri(false));
    Assert.assertEquals("https://" + endpoint, fdsConfig.buildBaseUri(true));
  }
}
