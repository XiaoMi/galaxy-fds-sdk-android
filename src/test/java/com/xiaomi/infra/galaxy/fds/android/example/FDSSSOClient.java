package com.xiaomi.infra.galaxy.fds.android.example;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.xiaomi.infra.galaxy.fds.android.FDSClientConfiguration;
import com.xiaomi.infra.galaxy.fds.android.GalaxyFDSClient;
import com.xiaomi.infra.galaxy.fds.android.GalaxyFDSClientImpl;
import com.xiaomi.infra.galaxy.fds.android.auth.GalaxyFDSCredential;
import com.xiaomi.infra.galaxy.fds.android.auth.SSOCredential;
import com.xiaomi.infra.galaxy.fds.android.model.FDSObject;
import com.xiaomi.infra.galaxy.fds.android.model.ObjectMetadata;
import com.xiaomi.infra.galaxy.fds.android.model.PutObjectResult;

public class FDSSSOClient {

  // This is a simple demo for FDS android SDK, note that before use android
  // sdk, you should do the following preparation:
  //    1. Create the bucket which you want to upload your object
  //    2. Grant proper bucket permission for your 'app'

  private static final String SSO_SERVICE_TOKEN = "your_service_token";
  private static final String BUCKET_NAME = "your_bucket";
  private static final String APP_ID = "your_app_id";

  public static void main(String[] args) throws Exception {
    // Initialize the sso client
    GalaxyFDSCredential credential = new SSOCredential(
        SSO_SERVICE_TOKEN, APP_ID);
    FDSClientConfiguration config = new FDSClientConfiguration()
        .withCredential(credential);
    GalaxyFDSClient client = new GalaxyFDSClientImpl(config);

    // Put an object to a specified bucket with a given name, if the object
    // already exists, it will be overrided.
    String objectContent = "This is a simple test object";
    InputStream inputStream = new ByteArrayInputStream(
        objectContent.getBytes());
    String objectName = "text-object" + System.currentTimeMillis();
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentType("plain/text");
    metadata.setContentLength(objectContent.length());
    client.putObject(BUCKET_NAME, objectName, inputStream, metadata);

    // Download the object with the bucket name and object name specified
    FDSObject object = client.getObject(BUCKET_NAME, objectName);
    byte[] buffer = new byte[objectContent.length()];
    object.getObjectContent().read(buffer);
    System.out.println(new String(buffer));

    // Put an object to a specified bucket, the server will generate an unique
    // object name for the object.
    inputStream = new ByteArrayInputStream(objectContent.getBytes());
    PutObjectResult result = client.putObject(BUCKET_NAME, inputStream, metadata);
    System.out.println(result.getObjectName());

    // Download the object with the bucket name and object name specified
    object = client.getObject(BUCKET_NAME, result.getObjectName());
    buffer = new byte[objectContent.length()];
    object.getObjectContent().read(buffer);
    System.out.println(new String(buffer));
  }
}
