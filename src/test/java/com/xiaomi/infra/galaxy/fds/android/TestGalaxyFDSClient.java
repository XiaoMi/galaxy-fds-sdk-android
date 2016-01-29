package com.xiaomi.infra.galaxy.fds.android;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.google.gson.Gson;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.xiaomi.infra.galaxy.fds.android.auth.OAuthCredential;
import com.xiaomi.infra.galaxy.fds.android.auth.SSOCredential;
import com.xiaomi.infra.galaxy.fds.android.exception.GalaxyFDSClientException;
import com.xiaomi.infra.galaxy.fds.android.model.ExpiresParam;
import com.xiaomi.infra.galaxy.fds.android.model.FDSObject;
import com.xiaomi.infra.galaxy.fds.android.model.InitMultipartUploadResult;
import com.xiaomi.infra.galaxy.fds.android.model.ObjectMetadata;
import com.xiaomi.infra.galaxy.fds.android.model.ProgressListener;
import com.xiaomi.infra.galaxy.fds.android.model.PutObjectResult;
import com.xiaomi.infra.galaxy.fds.android.model.ResponseContentTypeParam;
import com.xiaomi.infra.galaxy.fds.android.model.ResponseExpiresParam;
import com.xiaomi.infra.galaxy.fds.android.model.StorageAccessToken;
import com.xiaomi.infra.galaxy.fds.android.model.ThumbParam;
import com.xiaomi.infra.galaxy.fds.android.model.UploadPartResult;
import com.xiaomi.infra.galaxy.fds.android.model.UploadPartResultList;
import com.xiaomi.infra.galaxy.fds.android.model.UserParam;
import com.xiaomi.infra.galaxy.fds.android.util.Consts;
import com.xiaomi.infra.galaxy.fds.android.util.Util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.headRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;

public class TestGalaxyFDSClient {
  private static final int WIRE_MOCK_BASE_URI_PORT = 8964;
  private static final String SSO_SERVICE_TOKEN = "ossServiceToken";

  private static final String STORAGE_ACCESS_TOKEN = "storageAccessToken";
  private static final String APP_ID = "appId";
  private static final String OAUTH_APPID = "oauthAppId";
  private static final String OAUTH_ACCESS_TOKEN = "oauthAccessToken";
  private static final String OAUTH_PROVIDER = "oauthProvider";
  private static final String OAUTH_MAC_KEY = "oauthMacKey";
  private static final String OAUTH_MAC_ALGORITHM = "oauthMacAlgorithm";

  private final FDSClientConfiguration config;
  private final GalaxyFDSClient client;
  private final int partSize;

  @Rule
  public WireMockRule baseUriMockRule = new WireMockRule(WIRE_MOCK_BASE_URI_PORT);

  @Before
  public void setup() {
    WireMock.reset();
  }

  public TestGalaxyFDSClient() {
    config = new FDSClientConfiguration()
        .withCredential(new SSOCredential(SSO_SERVICE_TOKEN))
        .withUnitTestMode(true)
        .withBaseUriForUnitTest("http://localhost:" + WIRE_MOCK_BASE_URI_PORT + "/");
    client = new GalaxyFDSClientImpl(config);
    partSize = config.DEFAULT_UPLOAD_PART_SIZE;
  }

  @Test(timeout = 120*1000)
  public void testInvalidConfig() {
    try {
      new FDSClientConfiguration().withCredential(new SSOCredential(null));
      fail("null sso token. should fail");
    } catch (IllegalArgumentException e) {
      System.out.print(Util.getStackTrace(e));
    }

    try {
      new FDSClientConfiguration().withCredential(null);
      fail("null credential. should fail");
    } catch (IllegalArgumentException e) {
      System.out.print(Util.getStackTrace(e));
    }
  }

  @Test(timeout = 120*1000)
  public void testPutInvalidParam() {
    String bucketName = "testPutInvalidParam_bucket";
    String objectName = "testPutInvalidParam_object";
    try {
      client.putObject(bucketName, objectName, new File("/notexist/xxx"));
      fail("File not exist, should fail");
    } catch (GalaxyFDSClientException e) {
      System.out.print(Util.getStackTrace(e));
    }

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(-1);
    byte[] data = new byte[FDSClientConfiguration.DEFAULT_UPLOAD_PART_SIZE];
    InputStream in = new ByteArrayInputStream(data);
    try {
      client.putObject(bucketName, objectName, in, metadata);
      fail("Content length is negative, should fail");
    } catch (GalaxyFDSClientException e) {
      fail("Should not raise this exception");
    } catch (IllegalArgumentException e) {
      System.out.print(Util.getStackTrace(e));
    }
  }

  @Test(timeout = 120*1000)
  public void testPutBadResponse() {
    String bucketName = "testPutBadResponse_bucket";
    String objectName = "testPutBadResponse_object";
    String uploadId = "8964";
    int partNumber = 1;
    String initMultipartUrl = "/" + bucketName + "/" + objectName
        + "?uploads&serviceToken=" + SSO_SERVICE_TOKEN;
    String completeMultipartUrl = "/" + bucketName + "/" + objectName
        + "?uploadId=" + uploadId + "&serviceToken=" + SSO_SERVICE_TOKEN;
    String uploadPartUrl = getUploadPartUrl(bucketName, objectName, uploadId, partNumber);
    String abortMultipartUrl = "/" + bucketName + "/" + objectName
        + "?uploadId=" + uploadId + "&serviceToken=" + SSO_SERVICE_TOKEN;

    InitMultipartUploadResult initMultipartUploadResult = new InitMultipartUploadResult();
    initMultipartUploadResult.setBucketName(bucketName);
    initMultipartUploadResult.setObjectName(objectName);
    initMultipartUploadResult.setUploadId(uploadId);
    UploadPartResult uploadPartResult = new UploadPartResult(partNumber, partSize,
        Integer.toString(partNumber));

    baseUriMockRule.stubFor(put(urlEqualTo(initMultipartUrl))
        .inScenario("BadInitMultipartUpload")
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()))
        .willSetStateTo("success"));
    baseUriMockRule.stubFor(put(urlEqualTo(initMultipartUrl))
        .inScenario("BadInitMultipartUpload")
        .whenScenarioStateIs("success")
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
            .withBody(new Gson().toJson(initMultipartUploadResult))));

    baseUriMockRule.stubFor(put(urlEqualTo(uploadPartUrl))
        .inScenario("BadUploadPartResult")
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()))
        .willSetStateTo("second"));
    baseUriMockRule.stubFor(put(urlEqualTo(uploadPartUrl))
        .inScenario("BadUploadPartResult")
        .whenScenarioStateIs("second")
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()))
        .willSetStateTo("third"));
    baseUriMockRule.stubFor(put(urlEqualTo(uploadPartUrl))
        .inScenario("BadUploadPartResult")
        .whenScenarioStateIs("third")
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()))
        .willSetStateTo("success"));
    baseUriMockRule.stubFor(put(urlEqualTo(uploadPartUrl))
        .inScenario("BadUploadPartResult")
        .whenScenarioStateIs("success")
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
            .withBody(new Gson().toJson(uploadPartResult))));

    baseUriMockRule.stubFor(put(urlEqualTo(completeMultipartUrl)).willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())));

    baseUriMockRule.stubFor(delete(urlEqualTo(abortMultipartUrl)).willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)));

    byte[] data = new byte[FDSClientConfiguration.DEFAULT_UPLOAD_PART_SIZE];
    Arrays.fill(data, (byte) 5);
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(data.length);
    InputStream in = null;
    try {
      in = new ByteArrayInputStream(data);
      client.putObject(bucketName, objectName, in, metadata);
      fail("Should fail. Bad response of initMultipartUpload");
    } catch (GalaxyFDSClientException e) {
      System.out.print(Util.getStackTrace(e));
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) { }
      }
    }

    try {
      in = new ByteArrayInputStream(data);
      client.putObject(bucketName, objectName, in, metadata);
      fail("Should fail. Bad response of uploadPart");
    } catch (GalaxyFDSClientException e) {
      System.out.print(Util.getStackTrace(e));
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) { }
      }
    }

    try {
      in = new ByteArrayInputStream(data);
      client.putObject(bucketName, objectName, in, metadata);
      fail("Should fail. Bad response of completeMultipartUpload");
    } catch (GalaxyFDSClientException e) {
      System.out.print(Util.getStackTrace(e));
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) { }
      }
    }
  }

  @Test(timeout = 120*1000)
  public void testPutBadRequest() {
    String bucketName = "testPutBadRequest_bucket";
    String objectName = "testPutBadRequest_object";
    String url = "/" + bucketName + "/" + objectName + "?uploads&serviceToken="
        + SSO_SERVICE_TOKEN;

    baseUriMockRule.stubFor(put(urlEqualTo(url)).willReturn(aResponse()
        .withStatus(HttpStatus.SC_FORBIDDEN)));

    byte[] data = new byte[FDSClientConfiguration.DEFAULT_UPLOAD_PART_SIZE];
    Arrays.fill(data, (byte) 5);
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(data.length);
    InputStream in = new ByteArrayInputStream(data);
    try {
      client.putObject(bucketName, objectName, in, metadata);
      fail("bucket not accessible, should fail");
    } catch (GalaxyFDSClientException e) {
      System.out.print(Util.getStackTrace(e));
    }

    baseUriMockRule.verify(putRequestedFor(urlEqualTo(url))
        .withHeader(Consts.ESTIMATED_OBJECT_SIZE, equalTo(Long.toString(metadata
            .getContentLength())))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("SSO")));
  }
  @Test(timeout = 120*1000)
  public void testPutNormal() {
    int numParts = 3;
    String bucketName = "testPutNormal_bucket";
    String objectName = "testPutNormal_object";
    String uploadId = "8964";
    String signature = "abcdefg";
    String initMultipartUrl = "/" + bucketName + "/" + objectName
        + "?uploads&serviceToken=" + SSO_SERVICE_TOKEN;
    String completeMultipartUrl = "/" + bucketName + "/" + objectName
        + "?uploadId=" + uploadId + "&serviceToken=" + SSO_SERVICE_TOKEN;
    String checkObjectUrl = "/" + bucketName + "/" + objectName
        + "?serviceToken=" + SSO_SERVICE_TOKEN;

    InitMultipartUploadResult initMultipartUploadResult = new InitMultipartUploadResult();
    initMultipartUploadResult.setBucketName(bucketName);
    initMultipartUploadResult.setObjectName(objectName);
    initMultipartUploadResult.setUploadId(uploadId);
    baseUriMockRule.stubFor(put(urlEqualTo(initMultipartUrl)).willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
        .withBody(new Gson().toJson(initMultipartUploadResult))));
    List<UploadPartResult> uploadPartResults = new ArrayList<UploadPartResult>(numParts);
    for (int partNumber = 1; partNumber <= numParts; partNumber++) {
      UploadPartResult uploadPartResult = new UploadPartResult(partNumber, partSize,
          Integer.toString(partNumber));
      baseUriMockRule.stubFor(put(urlEqualTo(
          getUploadPartUrl(bucketName, objectName, uploadId, partNumber))).willReturn(aResponse()
          .withStatus(HttpStatus.SC_OK)
          .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
          .withBody(new Gson().toJson(uploadPartResult))));
      uploadPartResults.add(uploadPartResult);
    }
    UploadPartResultList uploadPartResultList = new UploadPartResultList();
    uploadPartResultList.setUploadPartResultList(uploadPartResults);

    PutObjectResult putObjectResult = new PutObjectResult();
    putObjectResult.setBucketName(bucketName);
    putObjectResult.setObjectName(objectName);
    putObjectResult.setAccessKeyId(SSO_SERVICE_TOKEN);
    putObjectResult.setExpires(Long.MAX_VALUE);
    putObjectResult.setSignature(signature);
    baseUriMockRule.stubFor(put(urlEqualTo(completeMultipartUrl)).willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
        .withBody(new Gson().toJson(putObjectResult))));
    baseUriMockRule.stubFor(head(urlEqualTo(checkObjectUrl)).willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)));

    byte[] data = new byte[(int) (partSize * 2.5)];
    for (int partNumber = 1; partNumber <= numParts; partNumber++) {
      if (partNumber == numParts) {
        Arrays.fill(data, (partNumber - 1) * partSize, (int) (
            (partNumber - 1 + 0.5) * partSize), (byte) partNumber);
      } else {
        Arrays.fill(data, (partNumber - 1) * partSize,
            partNumber * partSize, (byte) partNumber);
      }
    }
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(data.length);
    InputStream in = new ByteArrayInputStream(data);
    try {
      PutObjectResult result = client.putObject(bucketName, objectName, in, metadata);
      assertNotNull(result);
      assertEquals(SSO_SERVICE_TOKEN, result.getAccessKeyId());
      assertEquals(bucketName, result.getBucketName());
      assertEquals(objectName, result.getObjectName());
      assertEquals(Long.MAX_VALUE, result.getExpires());
      assertEquals(signature, result.getSignature());
      assertTrue(client.doesObjectExist(bucketName, objectName));
    } catch (GalaxyFDSClientException e) {
      fail("Should not raise exception");
    }

    baseUriMockRule.verify(putRequestedFor(urlEqualTo(initMultipartUrl))
        .withHeader(Consts.ESTIMATED_OBJECT_SIZE,
            equalTo(Long.toString(metadata.getContentLength())))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("SSO")));
    String dataString = new String(data);
    for (int partNumber = 1; partNumber <= numParts; partNumber++) {
      String subString;
      if (partNumber != numParts) {
        subString = dataString.substring((partNumber - 1) * partSize,
            partNumber * partSize);
      } else {
        subString = dataString.substring((partNumber - 1) * partSize,
            (int) ((partNumber - 1 + 0.5) * partSize));
      }
      baseUriMockRule.verify(putRequestedFor(
          urlEqualTo(getUploadPartUrl(bucketName, objectName, uploadId, partNumber)))
          .withHeader(HttpHeaders.CONTENT_LENGTH,
              equalTo(Long.toString(partNumber == numParts ? (int) (0.5 * partSize) : partSize)))
          .withHeader(HttpHeaders.AUTHORIZATION, equalTo("SSO"))
          .withRequestBody(equalTo(subString)));
    }
    baseUriMockRule.verify(putRequestedFor(urlEqualTo(completeMultipartUrl))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("SSO"))
        .withRequestBody(equalToJson(new Gson().toJson(uploadPartResultList))));
    baseUriMockRule.verify(headRequestedFor(urlEqualTo(checkObjectUrl)));
  }

  @Test(timeout = 120*1000)
  public void testPutRetryFail() {
    int numParts = 3;
    String bucketName = "testPutRetryFail_bucket";
    String objectName = "testPutRetryFail_object";
    String uploadId = "8964";
    String initMultipartUrl = "/" + bucketName + "/" + objectName
        + "?uploads&serviceToken=" + SSO_SERVICE_TOKEN;
    String abortMultipartUrl = "/" + bucketName + "/" + objectName
        + "?uploadId=" + uploadId + "&serviceToken=" + SSO_SERVICE_TOKEN;
    String checkObjectUrl = "/" + bucketName + "/" + objectName
        + "?serviceToken=" + SSO_SERVICE_TOKEN;;

    InitMultipartUploadResult initMultipartUploadResult = new InitMultipartUploadResult();
    initMultipartUploadResult.setBucketName(bucketName);
    initMultipartUploadResult.setObjectName(objectName);
    initMultipartUploadResult.setUploadId(uploadId);
    baseUriMockRule.stubFor(put(urlEqualTo(initMultipartUrl)).willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
        .withBody(new Gson().toJson(initMultipartUploadResult))));
    for (int partNumber = 1; partNumber <= numParts; partNumber++) {
      if (partNumber != numParts) {
        UploadPartResult uploadPartResult = new UploadPartResult(partNumber, partSize,
            Integer.toString(partNumber));
        baseUriMockRule.stubFor(put(urlEqualTo(
            getUploadPartUrl(bucketName, objectName, uploadId, partNumber))).willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
            .withBody(new Gson().toJson(uploadPartResult))));
      } else {
        baseUriMockRule.stubFor(put(urlEqualTo(getUploadPartUrl(bucketName, objectName, uploadId, partNumber)))
            .willReturn(aResponse().withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)));
      }
    }
    baseUriMockRule.stubFor(delete(urlEqualTo(abortMultipartUrl)).willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)));
    baseUriMockRule.stubFor(head(urlEqualTo(checkObjectUrl)).willReturn(aResponse()
        .withStatus(HttpStatus.SC_NOT_FOUND)));

    byte[] data = new byte[(int)
        (FDSClientConfiguration.DEFAULT_UPLOAD_PART_SIZE * 2.5)];
    Arrays.fill(data, (byte) 1);
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(data.length);
    InputStream in = new ByteArrayInputStream(data);
    try {
      client.putObject(bucketName, objectName, in, metadata);
      fail("should fail in uploading the third part");
    } catch (GalaxyFDSClientException e) {
      System.out.print(Util.getStackTrace(e));
    } finally {
      try {
        in.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    try {
      assertFalse(client.doesObjectExist(bucketName, objectName));
    } catch (GalaxyFDSClientException e) {
      fail("should not throw exception");
    }

    baseUriMockRule.verify(putRequestedFor(urlEqualTo(initMultipartUrl)));
    for (int partNumber = 1; partNumber <= numParts; partNumber++) {
      if (partNumber != numParts) {
        baseUriMockRule.verify(putRequestedFor(
            urlEqualTo(getUploadPartUrl(bucketName, objectName, uploadId, partNumber))));
      } else {
        baseUriMockRule.verify(3, putRequestedFor(
            urlEqualTo(getUploadPartUrl(bucketName, objectName, uploadId, partNumber))));
      }
      baseUriMockRule.verify(deleteRequestedFor(urlEqualTo(abortMultipartUrl)));
    }
    baseUriMockRule.verify(headRequestedFor(urlEqualTo(checkObjectUrl)));
  }

  @Test(timeout = 120*1000)
  public void testPutRetrySuccess() {
    int numParts = 3;
    String bucketName = "testPutRetrySuccess_bucket";
    String objectName = "testPutRetrySuccess_object";
    String uploadId = "8964";
    String signature = "abcdefg";
    long expire = 123456;
    String initMultipartUrl = "/" + bucketName + "/" + objectName
        + "?uploads&serviceToken=" + SSO_SERVICE_TOKEN;
    String completeMultipartUrl = "/" + bucketName + "/" + objectName
        + "?uploadId=" + uploadId + "&expires=" + expire + "&serviceToken="
        + SSO_SERVICE_TOKEN;

    InitMultipartUploadResult initMultipartUploadResult = new InitMultipartUploadResult();
    initMultipartUploadResult.setBucketName(bucketName);
    initMultipartUploadResult.setObjectName(objectName);
    initMultipartUploadResult.setUploadId(uploadId);
    baseUriMockRule.stubFor(put(urlEqualTo(initMultipartUrl)).willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withHeader(HttpHeaders.CONTENT_TYPE,
            ContentType.APPLICATION_JSON.toString())
        .withBody(new Gson().toJson(initMultipartUploadResult))));
    List<UploadPartResult> uploadPartResults = new ArrayList<UploadPartResult>(numParts);
    for (int partNumber = 1; partNumber <= numParts; partNumber++) {
      UploadPartResult uploadPartResult = new UploadPartResult(partNumber, partSize,
          Integer.toString(partNumber));
      if (partNumber != 2) {
        baseUriMockRule.stubFor(put(urlEqualTo(getUploadPartUrl(bucketName, objectName, uploadId,
            partNumber))).willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
            .withBody(new Gson().toJson(uploadPartResult))));
      } else {
        baseUriMockRule.stubFor(put(urlEqualTo(getUploadPartUrl(bucketName, objectName, uploadId, partNumber)))
            .inScenario("Retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR))
            .willSetStateTo("second"));
        baseUriMockRule.stubFor(put(urlEqualTo(getUploadPartUrl(bucketName, objectName, uploadId, partNumber)))
            .inScenario("Retry")
            .whenScenarioStateIs("second")
            .willReturn(aResponse().withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR))
            .willSetStateTo("last"));
        baseUriMockRule.stubFor(put(urlEqualTo(getUploadPartUrl(bucketName, objectName, uploadId, partNumber)))
            .inScenario("Retry")
            .whenScenarioStateIs("last")
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                .withBody(new Gson().toJson(uploadPartResult))));
      }
      uploadPartResults.add(uploadPartResult);
    }
    UploadPartResultList uploadPartResultList = new UploadPartResultList();
    uploadPartResultList.setUploadPartResultList(uploadPartResults);

    PutObjectResult putObjectResult = new PutObjectResult();
    putObjectResult.setBucketName(bucketName);
    putObjectResult.setObjectName(objectName);
    putObjectResult.setAccessKeyId(SSO_SERVICE_TOKEN);
    putObjectResult.setExpires(Long.MAX_VALUE);
    putObjectResult.setSignature(signature);
    baseUriMockRule.stubFor(put(urlEqualTo(completeMultipartUrl)).willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
        .withBody(new Gson().toJson(putObjectResult))));

    byte[] data = new byte[(int)
        (FDSClientConfiguration.DEFAULT_UPLOAD_PART_SIZE * 2.5)];
    Arrays.fill(data, (byte) 1);
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(data.length);
    InputStream in = new ByteArrayInputStream(data);
    try {
      List<UserParam> params = new ArrayList<UserParam>(1);
      params.add(new ExpiresParam(expire));
      MyProgressListener listener = new MyProgressListener();
      PutObjectResult result = client.putObject(bucketName, objectName, in,
          metadata, params, listener);
      assertNotNull(result);
      assertEquals(SSO_SERVICE_TOKEN, result.getAccessKeyId());
      assertEquals(bucketName, result.getBucketName());
      assertEquals(objectName, result.getObjectName());
      assertEquals(Long.MAX_VALUE, result.getExpires());
      assertEquals(signature, result.getSignature());
      assertEquals(1.0, listener.getTransferredPercentage());
    } catch (GalaxyFDSClientException e) {
      System.out.println(Util.getStackTrace(e));
      fail("should not throw any exceptions");
    }

    baseUriMockRule.verify(putRequestedFor(urlEqualTo(initMultipartUrl)));
    for (int partNumber = 1; partNumber <= numParts; partNumber++) {
      if (partNumber != 2) {
        baseUriMockRule.verify(putRequestedFor(
            urlEqualTo(getUploadPartUrl(bucketName, objectName, uploadId, partNumber))));
      } else {
        baseUriMockRule.verify(3, putRequestedFor(
            urlEqualTo(getUploadPartUrl(bucketName, objectName, uploadId, partNumber))));
      }
    }
    baseUriMockRule.verify(putRequestedFor(urlEqualTo(completeMultipartUrl))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("SSO"))
        .withRequestBody(equalToJson(new Gson().toJson(uploadPartResultList))));
  }

  @Test(timeout = 120*1000)
  public void testPost() {
    int numParts = 3;
    String bucketName = "testPost_bucket";
    String objectName = "testPost_object";
    String uploadId = "8964";
    String signature = "signature";
    String initMultipartUrl = "/" + bucketName + "/"  + "?uploads&serviceToken="
        + SSO_SERVICE_TOKEN;
    String completeMultipartUrl = "/" + bucketName + "/" + objectName
        + "?uploadId=" + uploadId + "&serviceToken=" + SSO_SERVICE_TOKEN;

    InitMultipartUploadResult initMultipartUploadResult = new InitMultipartUploadResult();
    initMultipartUploadResult.setBucketName(bucketName);
    initMultipartUploadResult.setObjectName(objectName);
    initMultipartUploadResult.setUploadId(uploadId);
    baseUriMockRule.stubFor(post(urlEqualTo(initMultipartUrl)).willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
        .withBody(new Gson().toJson(initMultipartUploadResult))));
    List<UploadPartResult> uploadPartResults = new ArrayList<UploadPartResult>(numParts);
    for (int partNumber = 1; partNumber <= numParts; partNumber++) {
      UploadPartResult uploadPartResult = new UploadPartResult(partNumber, partSize,
          Integer.toString(partNumber));
      baseUriMockRule.stubFor(put(urlEqualTo(
          getUploadPartUrl(bucketName, objectName, uploadId, partNumber))).willReturn(aResponse()
          .withStatus(HttpStatus.SC_OK)
          .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
          .withBody(new Gson().toJson(uploadPartResult))));
      uploadPartResults.add(uploadPartResult);
    }
    UploadPartResultList uploadPartResultList = new UploadPartResultList();
    uploadPartResultList.setUploadPartResultList(uploadPartResults);

    PutObjectResult putObjectResult = new PutObjectResult();
    putObjectResult.setBucketName(bucketName);
    putObjectResult.setObjectName(objectName);
    putObjectResult.setAccessKeyId(SSO_SERVICE_TOKEN);
    putObjectResult.setExpires(Long.MAX_VALUE);
    putObjectResult.setSignature(signature);
    baseUriMockRule.stubFor(put(urlEqualTo(completeMultipartUrl)).willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
        .withBody(new Gson().toJson(putObjectResult))));

    byte[] data = new byte[(int)
        (FDSClientConfiguration.DEFAULT_UPLOAD_PART_SIZE * 2.5)];
    Arrays.fill(data, (byte) 1);
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(data.length);
    InputStream in = new ByteArrayInputStream(data);
    try {
      PutObjectResult result = client.putObject(bucketName, in, metadata);
      assertNotNull(result);
      assertEquals(SSO_SERVICE_TOKEN, result.getAccessKeyId());
      assertEquals(bucketName, result.getBucketName());
      assertEquals(objectName, result.getObjectName());
      assertEquals(Long.MAX_VALUE, result.getExpires());
      assertEquals(signature, result.getSignature());
      assertEquals(config.getBaseUriForUnitTest() + "/" + bucketName + "/"
          + objectName + "?" + Consts.GALAXY_ACCESS_KEY_ID + "="
          + SSO_SERVICE_TOKEN + "&" + Consts.EXPIRES + "=" + Long.MAX_VALUE
          + "&" + Consts.SIGNATURE + "=" + signature,
          result.getAbsolutePresignedUri());
    } catch (GalaxyFDSClientException e) {
      System.out.print(Util.getStackTrace(e));
      fail("should not throw any exceptions");
    }

    baseUriMockRule.verify(postRequestedFor(urlEqualTo(initMultipartUrl)));
    for (int partNumber = 1; partNumber <= numParts; partNumber++) {
      baseUriMockRule.verify(putRequestedFor(
          urlEqualTo(getUploadPartUrl(bucketName, objectName, uploadId, partNumber))));
    }
    baseUriMockRule.verify(putRequestedFor(urlEqualTo(completeMultipartUrl))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("SSO"))
        .withRequestBody(equalToJson(new Gson().toJson(uploadPartResultList))));
  }

  @Test(timeout = 120*1000)
  public void testGetInvalidParam() {
    String bucketName = "testGetInvalidParam_bucket";
    String objectName = "testGetInvalidParam_object";
    String url = "/" + bucketName + "/" + objectName + "?serviceToken="
        + SSO_SERVICE_TOKEN;
    byte[] data = new byte[FDSClientConfiguration.DEFAULT_UPLOAD_PART_SIZE];

    baseUriMockRule.stubFor(get(urlEqualTo(url)).willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_OCTET_STREAM.toString())
        .withBody(data)));
    try {
      client.getObject(bucketName, objectName, new File("/root/no_permission"));
      fail("Have no rights to access file, should fail");
    } catch (GalaxyFDSClientException e) {
      System.out.print(Util.getStackTrace(e));
    }

    baseUriMockRule.stubFor(get(urlEqualTo(url)).willReturn(aResponse().withStatus(HttpStatus.SC_FORBIDDEN)));
    try {
      client.getObject(bucketName, objectName);
      fail("Have no rights to access file, should fail");
    } catch (GalaxyFDSClientException e) {
      System.out.print(Util.getStackTrace(e));
    }
    try {
      client.getObject(bucketName, objectName);
      fail("Have no rights to access file, should fail");
    } catch (GalaxyFDSClientException e) {
      System.out.print(Util.getStackTrace(e));
    }
  }

  @Test(timeout = 120*1000)
  public void testGetNormal() {
    String bucketName = "testGetNormal_bucket";
    String objectName = "testGetNormal_object";
    String url = "/" + bucketName + "/" + objectName + "?serviceToken="
        + SSO_SERVICE_TOKEN;
    int size = FDSClientConfiguration.DEFAULT_UPLOAD_PART_SIZE * 10;
    byte[] data = new byte[size];
    Arrays.fill(data, (byte) 2);

    baseUriMockRule.stubFor(get(urlEqualTo(url)).willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_OCTET_STREAM.toString())
        .withHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(size))
        .withBody(data)));
    try {
      FDSObject object = client.getObject(bucketName, objectName);
      assertNotNull(object);
      assertEquals(data.length, object.getObjectMetadata().getContentLength());
      assertEquals(ContentType.APPLICATION_OCTET_STREAM.toString(),
          object.getObjectMetadata().getContentType());
      InputStream in = object.getObjectContent();
      ByteArrayOutputStream out = new ByteArrayOutputStream(size);
      byte[] buffer = new byte[partSize];
      int bytesRead;
      while ((bytesRead = in.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
      }
      assertArrayEquals(data, out.toByteArray());
    } catch (Exception e) {
      fail("Should not throw any exception here");
    }
    baseUriMockRule.verify(getRequestedFor(urlEqualTo(url)));
  }

  @Test(timeout = 120*1000)
  public void testGetRange() {
    String bucketName = "testGetRange_bucket";
    String objectName = "testGetRange_object";
    long expire = 123456;
    String url = "/" + bucketName + "/" + objectName + "?expires=" + expire
        + "&serviceToken=" + SSO_SERVICE_TOKEN;
    int numParts = 10;
    int size = partSize * numParts;
    int off = (int) (2.5 * partSize);
    byte[] data = new byte[size];
    for (int partNumber = 1; partNumber <= numParts; partNumber++) {
      Arrays.fill(data, (partNumber - 1) * partSize, partNumber * partSize,
          (byte) partNumber);
    }

    byte[] rangeData = Arrays.copyOfRange(data, off, size);
    baseUriMockRule.stubFor(get(urlEqualTo(url)).willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_OCTET_STREAM.toString())
        .withHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(size - off))
        .withHeader(HttpHeaders.CONTENT_RANGE, "bytes=" + Integer.toString(off) + "-")
        .withBody(rangeData)));

    try {
      List<UserParam> params = new ArrayList<UserParam>(1);
      params.add(new ExpiresParam(expire));
      FDSObject object = client.getObject(bucketName, objectName, off, params, null);
      assertNotNull(object);
      assertEquals(rangeData.length, object.getObjectMetadata().getContentLength());
      assertEquals(ContentType.APPLICATION_OCTET_STREAM.toString(),
          object.getObjectMetadata().getContentType());
      InputStream in = object.getObjectContent();
      ByteArrayOutputStream out = new ByteArrayOutputStream(size);
      byte[] buffer = new byte[FDSClientConfiguration.DEFAULT_UPLOAD_PART_SIZE];
      int bytesRead;
      while ((bytesRead = in.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
      }
      assertArrayEquals(rangeData, out.toByteArray());
    } catch (Exception e) {
      System.out.print(Util.getStackTrace(e));
      fail("Should not throw any exception here");
    }
  }

  @Test(timeout = 120*1000)
  public void testGetWithUserParams() {
    String bucketName = "testGetWithUserParams_bucket";
    String objectName = "testGetWithUserParams_object";
    ResponseContentTypeParam contentTypeParam = new ResponseContentTypeParam();
    ResponseExpiresParam responseExpiresParam = new ResponseExpiresParam();
    ExpiresParam expiresParam = new ExpiresParam(1000);

    String url = "/" + bucketName + "/" + objectName + "?response-content-type"
        + "&response-expires&expires=1000&serviceToken=" + SSO_SERVICE_TOKEN;

    try {
      List<UserParam> params = new ArrayList<UserParam>(3);
      params.add(contentTypeParam);
      params.add(responseExpiresParam);
      params.add(expiresParam);
      client.getObject(bucketName, objectName, 0, params, null);
    } catch (Exception e) {
    }
    verify(getRequestedFor(urlEqualTo(url)));
  }

  @Test(timeout = 120*1000)
  public void testGetFromCdn() {
    String bucketName = "testGetCdn_bucket";
    String objectName = "testGetCdn_object";
    String url = "/" + bucketName + "/" + objectName + "?serviceToken="
        + SSO_SERVICE_TOKEN;
    int size = FDSClientConfiguration.DEFAULT_UPLOAD_PART_SIZE * 10;
    byte[] data = new byte[size];
    Arrays.fill(data, (byte) 2);

    baseUriMockRule.stubFor(get(urlEqualTo(url)).willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_OCTET_STREAM.toString())
        .withHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(size))
        .withBody(data)));
    try {
      FDSObject object = client.getObject(bucketName, objectName, 0, null, null, true);
      assertNotNull(object);
      assertEquals(data.length, object.getObjectMetadata().getContentLength());
      assertEquals(ContentType.APPLICATION_OCTET_STREAM.toString(),
          object.getObjectMetadata().getContentType());
      InputStream in = object.getObjectContent();
      ByteArrayOutputStream out = new ByteArrayOutputStream(size);
      byte[] buffer = new byte[partSize];
      int bytesRead;
      while ((bytesRead = in.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
      }
      assertArrayEquals(data, out.toByteArray());
    } catch (Exception e) {
      System.out.print(Util.getStackTrace(e));
      fail("Should not throw any exception here");
    }
    baseUriMockRule.verify(getRequestedFor(urlEqualTo(url)));
  }

  @Test(timeout = 120*1000)
  public void testGetFromPreSignedUrl() {
    String bucketName = "testGetFromPreSignedUrl_bucket";
    String objectName = "testGetFromPreSignedUrl_object";

    int width = 100;
    int height = 200;
    ThumbParam thumbParam = new ThumbParam(width, height);
    String preSignedUrlParams = "?GalaxyAccessKeyId=5211722473393"
        + "&Expires=1409385418532&Signature=3E6lB8NdY/1+KYxVaLLAXlLfLCM=";
    String preSignedUrl = config.getBaseUriForUnitTest() + "/" + bucketName + "/"
        + objectName + preSignedUrlParams;
    String url = "/" + bucketName + "/" + objectName + preSignedUrlParams + "&"
        + thumbParam.toString() + "&serviceToken=" + SSO_SERVICE_TOKEN;
    try {
      List<UserParam> params = new ArrayList<UserParam>(1);
      params.add(thumbParam);
      client.getObject(preSignedUrl, 0, params, null);
    } catch (Exception e) {
    }
    baseUriMockRule.verify(getRequestedFor(urlEqualTo(url)));
  }

  @Test(timeout = 120*1000)
  public void testOauthAuthentication() throws Exception {
    String bucketName = "testOAuth_bucket";
    String objectName = "testOAuth_object";

    long expireTime = 3000;
    StorageAccessToken accessToken = new StorageAccessToken();
    accessToken.setExpireTime(expireTime);
    accessToken.setToken(STORAGE_ACCESS_TOKEN);

    String getStorageAccessTokenUri = "/?storageAccessToken&appId=" + APP_ID
        + "&oauthAppId=" + OAUTH_APPID + "&oauthAccessToken=" + OAUTH_ACCESS_TOKEN
        + "&oauthProvider=" + OAUTH_PROVIDER + "&oauthMacAlgorithm=" + OAUTH_MAC_ALGORITHM
        + "&oauthMacKey=" + OAUTH_MAC_KEY;
    baseUriMockRule.stubFor(get(urlEqualTo(getStorageAccessTokenUri)).willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withBody(new Gson().toJson(accessToken))));

    int size = FDSClientConfiguration.DEFAULT_UPLOAD_PART_SIZE * 10;
    byte[] data = new byte[size];
    Arrays.fill(data, (byte) 2);
    String getObjectUri = "/" + bucketName + "/" + objectName + "?appId="
        + APP_ID + "&storageAccessToken=" + STORAGE_ACCESS_TOKEN;
    baseUriMockRule.stubFor(get(urlEqualTo(getObjectUri)).willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_OCTET_STREAM.toString())
        .withHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(size))
        .withBody(data)));

    FDSClientConfiguration newConfig = new FDSClientConfiguration()
        .withUnitTestMode(true)
        .withBaseUriForUnitTest(config.getBaseUriForUnitTest());
    newConfig.setCredential(new OAuthCredential(
        newConfig.getBaseUri(),
        APP_ID,
        OAUTH_APPID,
        OAUTH_ACCESS_TOKEN,
        OAUTH_PROVIDER,
        OAUTH_MAC_KEY,
        OAUTH_MAC_ALGORITHM));
    GalaxyFDSClient newClient = new GalaxyFDSClientImpl(newConfig);

    FDSObject object = newClient.getObject(bucketName, objectName);
    assertNotNull(object);
    assertEquals(data.length, object.getObjectMetadata().getContentLength());
    assertEquals(ContentType.APPLICATION_OCTET_STREAM.toString(),
        object.getObjectMetadata().getContentType());
    InputStream in = object.getObjectContent();
    ByteArrayOutputStream out = new ByteArrayOutputStream(size);
    byte[] buffer = new byte[partSize];
    int bytesRead;
    while ((bytesRead = in.read(buffer)) != -1) {
      out.write(buffer, 0, bytesRead);
    }
    assertArrayEquals(data, out.toByteArray());

    baseUriMockRule.verify(getRequestedFor(urlEqualTo(getStorageAccessTokenUri))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("OAuth")));
    baseUriMockRule.verify(getRequestedFor(urlEqualTo(getObjectUri)));
  }

  @Test(timeout = 120*1000)
  public void testGetObjectWithSlashInName() {
    String bucketName = "testGetObjectWithSlashInName_bucket";
    String objectName = "test/get/object";
    String url = "/" + bucketName + "/" + objectName + "?serviceToken="
        + SSO_SERVICE_TOKEN;
    int size = FDSClientConfiguration.DEFAULT_UPLOAD_PART_SIZE * 10;
    byte[] data = new byte[size];
    Arrays.fill(data, (byte) 2);

    baseUriMockRule.stubFor(get(urlEqualTo(url)).willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_OCTET_STREAM.toString())
        .withHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(size))
        .withBody(data)));
    try {
      FDSObject object = client.getObject(bucketName, objectName);
      assertNotNull(object);
      assertEquals(data.length, object.getObjectMetadata().getContentLength());
      assertEquals(ContentType.APPLICATION_OCTET_STREAM.toString(),
          object.getObjectMetadata().getContentType());
      InputStream in = object.getObjectContent();
      ByteArrayOutputStream out = new ByteArrayOutputStream(size);
      byte[] buffer = new byte[partSize];
      int bytesRead;
      while ((bytesRead = in.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
      }
      assertArrayEquals(data, out.toByteArray());
    } catch (Exception e) {
      fail("Should not throw any exception here");
    }
    baseUriMockRule.verify(getRequestedFor(urlEqualTo(url)));
  }

  @Test(timeout = 120 * 1000)
  public void testInvalidObjectMetadata() {
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.addUserMetadata(Consts.XIAOMI_META_HEADER_PREFIX
        + "test", "test-value");
    metadata.addPredefinedMetadata(HttpHeaders.CACHE_CONTROL, "no-cache");

    try {
      metadata.addUserMetadata("test-key", "test-value");
      fail("Should not arrive here");
    } catch (Exception e) {

    }
  }

  private static class MyProgressListener extends ProgressListener {

    private double transferredPercentage;

    @Override
    public void onProgress(long transferred, long total) {
      transferredPercentage = (double) transferred / total;
    }

    public double getTransferredPercentage() {
      return transferredPercentage;
    }
  }

  private String getUploadPartUrl(String bucketName, String objectName,
      String uploadId, int partNumber) {
    return  "/" + bucketName + "/" + objectName + "?uploadId=" + uploadId
        + "&partNumber=" + partNumber + "&serviceToken=" + SSO_SERVICE_TOKEN;
  }
}
