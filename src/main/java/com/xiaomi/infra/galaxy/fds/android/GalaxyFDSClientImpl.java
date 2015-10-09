package com.xiaomi.infra.galaxy.fds.android;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.util.Log;
import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.xiaomi.infra.galaxy.fds.android.auth.GalaxyFDSCredential;
import com.xiaomi.infra.galaxy.fds.android.exception.GalaxyFDSClientException;
import com.xiaomi.infra.galaxy.fds.android.model.FDSObject;
import com.xiaomi.infra.galaxy.fds.android.model.HttpHeaders;
import com.xiaomi.infra.galaxy.fds.android.model.HttpMethod;
import com.xiaomi.infra.galaxy.fds.android.model.InitMultipartUploadResult;
import com.xiaomi.infra.galaxy.fds.android.model.ObjectMetadata;
import com.xiaomi.infra.galaxy.fds.android.model.ProgressListener;
import com.xiaomi.infra.galaxy.fds.android.model.PutObjectResult;
import com.xiaomi.infra.galaxy.fds.android.model.ThumbParam;
import com.xiaomi.infra.galaxy.fds.android.model.UploadPartResult;
import com.xiaomi.infra.galaxy.fds.android.model.UploadPartResultList;
import com.xiaomi.infra.galaxy.fds.android.model.UserParam;
import com.xiaomi.infra.galaxy.fds.android.util.Args;
import com.xiaomi.infra.galaxy.fds.android.util.Consts;
import com.xiaomi.infra.galaxy.fds.android.util.ObjectInputStream;
import com.xiaomi.infra.galaxy.fds.android.util.RequestFactory;
import com.xiaomi.infra.galaxy.fds.android.util.Util;

public class GalaxyFDSClientImpl implements GalaxyFDSClient {

  private static final String LOG_TAG = "GalaxyFDSClientImpl";
  private static final String HTTP_SCHEME = "http";
  private static final String HTTPS_SCHEME = "https";

  private static final boolean TEST_MODE;
  static {
    String runtime = System.getProperty("java.runtime.name");
    if (runtime != null && runtime.equals("android runtime")) {
      TEST_MODE = false;
    } else {
      TEST_MODE = true;
    }
  }

  private final FDSClientConfiguration config;
  private final HttpClient httpClient;
  private ThreadPoolExecutor threadPoolExecutor;

  public GalaxyFDSClientImpl(FDSClientConfiguration config) {
    this.config = config;
    this.httpClient = createHttpClient(this.config);
    BlockingQueue workQueue = new ArrayBlockingQueue<Runnable>(
        config.getWorkQueueCapacity(), true);
    this.threadPoolExecutor = new ThreadPoolExecutor(
        config.getThreadPoolCoreSize(), config.getThreadPoolMaxSize(),
        config.getThreadPoolKeepAliveSecs(), TimeUnit.SECONDS, workQueue,
        new ThreadFactory() {
          @Override
          public Thread newThread(Runnable r) {
            return new Thread(r, "FDS-multipart-upload-thread");
          }
        });
  }

  @Deprecated
  public GalaxyFDSClientImpl(String fdsServiceBaseUri,
      GalaxyFDSCredential credential, FDSClientConfiguration config) {
    this.config = config;
    this.config.setCredential(credential);
    this.httpClient = createHttpClient(this.config);
  }

  private HttpClient createHttpClient(FDSClientConfiguration config) {
        /* Set HTTP client parameters */
    HttpParams httpClientParams = new BasicHttpParams();
    HttpConnectionParams.setConnectionTimeout(httpClientParams,
        config.getConnectionTimeoutMs());
    HttpConnectionParams.setSoTimeout(httpClientParams,
        config.getSocketTimeoutMs());
    HttpConnectionParams.setStaleCheckingEnabled(httpClientParams, true);
    HttpConnectionParams.setTcpNoDelay(httpClientParams, true);

    int socketSendBufferSizeHint = config.getSocketBufferSizeHints()[0];
    int socketReceiveBufferSizeHint = config.getSocketBufferSizeHints()[1];
    if (socketSendBufferSizeHint > 0 || socketReceiveBufferSizeHint > 0) {
      HttpConnectionParams.setSocketBufferSize(httpClientParams,
          Math.max(socketSendBufferSizeHint, socketReceiveBufferSizeHint));
    }

    SchemeRegistry registry = new SchemeRegistry();
    SocketFactory socketFactory = PlainSocketFactory.getSocketFactory();
    registry.register(new Scheme(HTTP_SCHEME, socketFactory, 80));

    if (config.isHttpsEnabled()) {
      SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
      sslSocketFactory.setHostnameVerifier(
          SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
      registry.register(new Scheme(HTTPS_SCHEME, sslSocketFactory, 443));
    }

    ThreadSafeClientConnManager connectionManager =
        new ThreadSafeClientConnManager(httpClientParams, registry);
    DefaultHttpClient httpClient = new DefaultHttpClient(connectionManager,
        httpClientParams);
    return httpClient;
  }

  private boolean isGetThumbnail(List<UserParam> params) {
    boolean isGetThumbnail = false;
    if (params != null) {
      for (UserParam param : params) {
        if (param instanceof ThumbParam) {
          isGetThumbnail = true;
          break;
        }
      }
    }
    return isGetThumbnail;
  }

  @Override
  public FDSObject getObject(String bucketName, String objectName)
      throws GalaxyFDSClientException {
    return getObject(bucketName, objectName, 0, null);
  }

  @Override
  public FDSObject getObject(String bucketName, String objectName, long offset,
      List<UserParam> params) throws GalaxyFDSClientException {
    return getObject(bucketName, objectName, offset, params, null);
  }

  @Override
  public FDSObject getObject(String bucketName, String objectName, long offset,
      List<UserParam> params, ProgressListener listener)
      throws GalaxyFDSClientException {
    Args.notNull(bucketName, "bucket name");
    Args.notEmpty(bucketName, "bucket name");
    Args.notNull(objectName, "object name");
    Args.notEmpty(objectName, "object name");

    StringBuilder builder = new StringBuilder();
    builder.append(config.getDownloadBaseUri());
    builder.append("/" + bucketName + "/" + objectName);
    return getObject(builder.toString(), offset, params, listener);
  }

  /**
   * In this deprecated method, parameter fromCdn is ignored.
   */
  @Override
  @Deprecated
  public FDSObject getObject(String bucketName, String objectName, long offset,
      List<UserParam> params, ProgressListener listener, boolean fromCdn)
      throws GalaxyFDSClientException {
    // Ignore param fromCdn.
    return getObject(bucketName, objectName, offset, params, listener);
  }

  @Override
  public FDSObject getObject(String uriString, long offset, List<UserParam> params,
      ProgressListener listener) throws GalaxyFDSClientException {
    Args.notNull(uriString, "URI");
    Args.notNegative(offset, "offset in content");

    if (params != null) {
      StringBuilder builder = new StringBuilder(uriString);
      for (UserParam param : params) {
        if (builder.indexOf("?") == -1) {
          builder.append('?');
        } else {
          builder.append('&');
        }
        builder.append(param.toString());
      }
      uriString = builder.toString();
    }

    String bucketName;
    String objectName;
    try {
      URI uri = new URI(uriString);
      String path = uri.getPath();
      int firstSlashIndex = 0;
      int secondSlashIndex = path.indexOf('/', firstSlashIndex + 1);
      if (secondSlashIndex == -1) {
        throw new URISyntaxException(uriString, "not a valid object URI");
      }

      bucketName = path.substring(firstSlashIndex, secondSlashIndex);
      objectName = path.substring(secondSlashIndex + 1);
    } catch (URISyntaxException e) {
      throw new GalaxyFDSClientException("Invalid URI, can't parse bucket name"
          + "and object name form it:" + uriString, e);
    }

    InputStream responseContent = null;
    GalaxyFDSClientException lastException = null;
    try {
      Map<String, String> headers = new HashMap<String, String>();
      // If gets thumbnail, should not use get range
      if (offset > 0 && !isGetThumbnail(params)) {
        headers.put(HttpHeaders.RANGE, "bytes=" + offset + "-");
      }
      HttpUriRequest request = RequestFactory.createRequest(uriString,
          config.getCredential(), HttpMethod.GET, headers);
      HttpResponse response = httpClient.execute(request);
      responseContent = response.getEntity().getContent();
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_PARTIAL_CONTENT) {
        lastException = new GalaxyFDSClientException("Unable to get object["
            + bucketName + "/" + objectName + "] from URI :" + uriString
            + ". Cause:" + response.getStatusLine().toString());
        throw lastException;
      }

      FDSObject object = new FDSObject(bucketName, objectName);
      ObjectMetadata metadata = ObjectMetadata.parseObjectMetadata(
          response.getAllHeaders());
      object.setObjectContent(new ObjectInputStream(responseContent, metadata,
          listener));
      object.setObjectMetadata(metadata);
      return object;
    } catch (IOException e) {
      lastException = new GalaxyFDSClientException("Unable to get object["
          + bucketName + "/" + objectName + "] from URI :" + uriString
          + " Exception:" + e.getMessage(), e);
      throw lastException;
    } finally {
      if (lastException != null && responseContent != null) {
        try {
          responseContent.close();
        } catch (IOException e) {
          // Ignored
        }
      }
    }
  }

  @Override
  public ObjectMetadata getObject(String bucketName, String objectName,
      File destinationFile) throws GalaxyFDSClientException {
    return getObject(bucketName, objectName, destinationFile, null);
  }

  @Override
  public ObjectMetadata getObject(String bucketName, String objectName,
      File destinationFile, List<UserParam> params)
      throws GalaxyFDSClientException {
    return getObject(bucketName, objectName, destinationFile, params, null);
  }

  @Override
  public ObjectMetadata getObject(String bucketName, String objectName,
      File destinationFile, List<UserParam> params, ProgressListener listener)
      throws GalaxyFDSClientException {
    Args.notNull(destinationFile, "Destination file");

    int retriedTimes = 0;
    boolean isGetThumbnail = isGetThumbnail(params);
    while (true) {
      try {
        boolean isAppend = retriedTimes != 0 && !isGetThumbnail;
        FDSObject object = getObject(bucketName, objectName,
            isAppend ? destinationFile.length() : 0, params, listener);
        // If never retried before, downloads the file from beginning,
        // otherwise downloads the file from the position where last download ends
        Util.downloadObjectToFile(object, destinationFile, isAppend);
        return object.getObjectMetadata();
      } catch (GalaxyFDSClientException e) {
        if (++retriedTimes >= config.getMaxRetryTimes()) {
          throw e;
        } else if (!TEST_MODE) {
            Log.i(LOG_TAG, "Retry the download of object:" + objectName + " bucket"
                + ":" + bucketName + " to file: " + destinationFile.getAbsolutePath()
                + " cause:" + Util.getStackTrace(e));
        }
      }
    }
  }

  /**
   * In this deprecated method, parameter fromCdn is ignored.
   */
  @Override
  @Deprecated
  public ObjectMetadata getObject(String bucketName, String objectName,
      File destinationFile, List<UserParam> params,
      ProgressListener listener, boolean fromCdn)
      throws GalaxyFDSClientException {
    return getObject(bucketName, objectName, destinationFile, params, listener);
 }

  @Override
  public ObjectMetadata getObject(String uriString, File destinationFile,
      List<UserParam> params, ProgressListener listener)
      throws GalaxyFDSClientException {
    Args.notNull(destinationFile, "Destination file");

    int retriedTimes = 0;
    boolean isGetThumbnail = isGetThumbnail(params);
    while (true) {
      try {
        boolean isAppend = retriedTimes != 0 && !isGetThumbnail;
        FDSObject object = getObject(uriString,
            isAppend ? destinationFile.length() : 0, params, listener);
        // If never retried before, downloads the file from beginning,
        // otherwise downloads the file from the position where last download ends
        Util.downloadObjectToFile(object, destinationFile, isAppend);
        return object.getObjectMetadata();
      } catch (GalaxyFDSClientException e) {
        if (++retriedTimes >= config.getMaxRetryTimes()) {
          throw e;
        } else if (!TEST_MODE) {
          Log.i(LOG_TAG, "Retry the download of object:" + uriString
              + " to file: " + destinationFile.getAbsolutePath()
              + " cause:" + Util.getStackTrace(e));
        }
      }
    }
  }

  @Override
  public PutObjectResult putObject(String bucketName, String objectName,
      File file) throws GalaxyFDSClientException {
    return putObject(bucketName, objectName, file, null);
  }

  @Override
  public PutObjectResult putObject(String bucketName, String objectName,
      File file, List<UserParam> params) throws GalaxyFDSClientException {
    return putObject(bucketName, objectName, file, params, null);
  }

  @Override
  public PutObjectResult putObject(String bucketName, String objectName,
      File file, List<UserParam> params, ProgressListener listener)
      throws GalaxyFDSClientException {
    Args.notNull(file, "file");

    try {
      InputStream in = new BufferedInputStream(new FileInputStream(file));
      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentLength(file.length());
      metadata.setContentType(Util.getMimeType(file));
      metadata.setLastModified(new Date(file.lastModified()));
      return putObject(bucketName, objectName, in, metadata, params, listener);
    } catch (FileNotFoundException e) {
      throw new GalaxyFDSClientException("Unable to find the file to be uploaded"
          + ":" + file.getAbsolutePath(), e);
    }
  }

  @Override
  public PutObjectResult putObject(String bucketName, String objectName,
      InputStream input, ObjectMetadata metadata)
      throws GalaxyFDSClientException {
    return putObject(bucketName, objectName, input, metadata, null);
  }

  @Override
  public PutObjectResult putObject(String bucketName, String objectName,
      InputStream input, ObjectMetadata metadata, List<UserParam> params)
      throws GalaxyFDSClientException {
    return putObject(bucketName, objectName, input, metadata, params, null);
  }

  @Override
  public PutObjectResult putObject(final String bucketName, String objectName,
      InputStream input, final ObjectMetadata metadata, List<UserParam> params,
      ProgressListener listener) throws GalaxyFDSClientException {
    Args.notNull(bucketName, "bucket name");
    Args.notEmpty(bucketName, "bucket name");
    Args.notNull(input, "input stream");
    Args.notNull(metadata, "metadata");
    long contentLength = metadata.getContentLength();
    Args.notNegative(contentLength, "content length");

    if (metadata.getContentType() == null) {
      metadata.setContentType(Consts.APPLICATION_OCTET_STREAM);
    }

    String uploadId = null;
    ObjectInputStream objectInputStream = new ObjectInputStream(input, metadata,
        listener);
    try {
      InitMultipartUploadResult initMultipartUploadResult =
          initMultipartUpload(bucketName, objectName, metadata.getContentLength());
      // Object name may be null
      objectName = initMultipartUploadResult.getObjectName();
      uploadId = initMultipartUploadResult.getUploadId();
      int partSize = config.getUploadPartSize();
      int numParts = (int) (contentLength + partSize - 1) / partSize;

      final String finalUploadId = uploadId;
      final String finalObjectName = objectName;
      int remainingBytes = (int)contentLength;
      List<Future<UploadPartResult>> futures
          = new ArrayList<Future<UploadPartResult>>(numParts);
      List<UploadPartResult> results = new ArrayList<UploadPartResult>(numParts);
      for (int partNumber = 1; partNumber <= numParts; partNumber++) {
        final int uploadBytes = Math.min(partSize, remainingBytes);
        final byte[] buffer = new byte[uploadBytes];
        final int finalPartNumber = partNumber;
        objectInputStream.read(buffer, 0, uploadBytes);
        Future<UploadPartResult> future = threadPoolExecutor
            .submit(new Callable<UploadPartResult>() {
              @Override
              public UploadPartResult call() throws Exception {
                return uploadPart(finalUploadId, bucketName, finalObjectName,
                    finalPartNumber, new ObjectInputStream(
                        new ByteArrayInputStream(buffer), metadata, null),
                    uploadBytes);
              }
            });
        futures.add(future);
        remainingBytes -= uploadBytes;
      }
      for (int partNumber = 1; partNumber <= numParts; partNumber++) {
        results.add(futures.get(partNumber - 1).get());
      }
      UploadPartResultList uploadPartResultList = new UploadPartResultList();
      uploadPartResultList.setUploadPartResultList(results);
      return completeMultipartUpload(uploadId, bucketName, objectName, metadata,
          uploadPartResultList, params);
    } catch(Exception e) {
      if (uploadId != null) {
        abortMultipartUpload(bucketName, objectName, uploadId);
      }
      throw new GalaxyFDSClientException(e);
    } finally {
      try {
        objectInputStream.close();
      } catch (IOException e) {
        // Ignore IOException when close stream
      }
    }
  }

  private InitMultipartUploadResult initMultipartUpload(String bucketName,
      String objectName, long estimatedSize) throws GalaxyFDSClientException {
    String uriString = config.getUploadBaseUri() + "/" + bucketName + "/"
        + (objectName == null ? "" : objectName) + "?uploads";

    InputStream responseContent = null;
    try {
      /*
       * If the object's name is not provided, should use HTTP POST method to
       * ask FDS service to generate one.
       */
      Map<String, String> headers = new HashMap<String, String>();
      headers.put(Consts.ESTIMATED_OBJECT_SIZE, Long.toString(estimatedSize));
      HttpUriRequest request = RequestFactory.createRequest(uriString,
          config.getCredential(), objectName == null ?
              HttpMethod.POST : HttpMethod.PUT, headers);
      HttpResponse response = httpClient.execute(request);
      responseContent = response.getEntity().getContent();
      if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
        throw new GalaxyFDSClientException("Unable to upload object["
            + bucketName + "/" + objectName + "] to URI :" + uriString
            + ". Fail to initiate multipart upload: "
            + response.getStatusLine().toString());
      }
      Reader reader = new InputStreamReader(responseContent);
      InitMultipartUploadResult result = new Gson().fromJson(reader,
          InitMultipartUploadResult.class);
      if (result == null || result.getUploadId() == null
          || result.getObjectName() == null || result.getBucketName() == null) {
        throw new GalaxyFDSClientException("Fail to parse the result of init "
            + "multipart upload. bucket name:" + bucketName + ", object name:"
            + objectName);
      }
      return result;
    } catch (IOException e) {
      throw new GalaxyFDSClientException("Fail to initiate multipart upload. "
          + "URI:" + uriString, e);
    } finally {
      if (responseContent != null) {
        try {
          responseContent.close();
        } catch (IOException e) {
          // Ignored
        }
      }
    }
  }

  private UploadPartResult uploadPart(String uploadId, String bucketName,
      String objectName, int partNumber, ObjectInputStream in, long contentLength)
      throws GalaxyFDSClientException {
    String uriString = config.getUploadBaseUri() + "/" + bucketName + "/"
        + objectName + "?uploadId=" + uploadId + "&partNumber=" + partNumber;

    byte[] buffer = new byte[FDSClientConfiguration.DEFAULT_UPLOAD_PART_SIZE];
    ByteArrayOutputStream out = new ByteArrayOutputStream((int) contentLength);
    try {
      long remainingBytes = contentLength;
      while (remainingBytes != 0) {
        int toReadBytes = Math.min(buffer.length, (int) remainingBytes);
        int readBytes = in.read(buffer, 0, toReadBytes);
        if (readBytes == -1) {
          break;
        }
        out.write(buffer, 0, readBytes);
        remainingBytes -= readBytes;
      }
    } catch (IOException e) {
      throw new GalaxyFDSClientException("Fail to read data from input stream,"
          + " size:" + contentLength, e);
    }

    byte[] data = out.toByteArray();
    try {
      out.close();
    } catch (IOException e) {
      //Ignored
    }

    int retriedTimes = 0;
    InputStream responseContent = null;
    while (true) {
      try {
        HttpUriRequest request = RequestFactory.createRequest(uriString,
            config.getCredential(), HttpMethod.PUT, null);
        ((HttpPut) request).setEntity(new ByteArrayEntity(data));
        HttpResponse response;
        try {
          response = httpClient.execute(request);
          responseContent = response.getEntity().getContent();
          if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new GalaxyFDSClientException("Unable to upload object["
                + bucketName + "/" + objectName + "] to URI :" + uriString
                + ". Fail to upload part " + partNumber + ": "
                + response.getStatusLine().toString());
          }
          Reader reader = new InputStreamReader(responseContent);
          UploadPartResult result = new Gson().fromJson(reader,
              UploadPartResult.class);
          if (result == null || result.getEtag() == null || result.getPartSize() == 0) {
            throw new GalaxyFDSClientException("Fail to parse the result of" +
                " uploading part. bucket name:" + bucketName + ", object name:"
                + objectName + ", upload ID:" + uploadId);
          }
          return result;
        } catch (IOException e) {
          throw new GalaxyFDSClientException("Fail to put part. URI:"
              + uriString, e);
        }
      } catch (GalaxyFDSClientException e) {
        if (++retriedTimes >= config.getMaxRetryTimes()) {
          throw e;
        } else if (!TEST_MODE) {
          Log.i(LOG_TAG, "Retry the upload of object:" + objectName + " bucket"
              + ":" + bucketName + " upload id:" + uploadId + " part number:"
              + partNumber + " cause:" + Util.getStackTrace(e));
        }
      } finally {
        if (responseContent != null) {
          try {
            responseContent.close();
          } catch (IOException e) {
            // Ignored
          }
        }
      }
    }
  }

  private PutObjectResult completeMultipartUpload(String uploadId,
      String bucketName, String objectName, ObjectMetadata metadata,
      UploadPartResultList uploadPartResultList, List<UserParam> params)
      throws GalaxyFDSClientException {
    StringBuilder builder = new StringBuilder();
    builder.append(config.getUploadBaseUri() + "/" + bucketName + "/"
        + objectName + "?uploadId=" + uploadId);
    if (params != null) {
      for (UserParam param : params) {
        builder.append('&');
        builder.append(param.toString());
      }
    }
    String uriString = builder.toString();

    InputStream responseContent = null;
    try {
      Map<String, String> headers = null;
      if (metadata != null) {
        headers = metadata.getAllMetadata();
      }

      HttpUriRequest request = RequestFactory.createRequest(uriString,
          config.getCredential(), HttpMethod.PUT, headers);
      ((HttpPut) request).setEntity(
          new StringEntity(new Gson().toJson(uploadPartResultList)));
      HttpResponse response = httpClient.execute(request);
      responseContent = response.getEntity().getContent();
      if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
        throw new GalaxyFDSClientException("Unable to upload object[" + bucketName
            + "/" + objectName + "] to URI :" + uriString
            + ". Fail to complete multipart upload: "
            + response.getStatusLine().toString());
      }
      Reader reader = new InputStreamReader(responseContent);
      PutObjectResult result = new Gson().fromJson(reader, PutObjectResult.class);
      if (result == null || result.getAccessKeyId() == null
          || result.getSignature() == null || result.getExpires() == 0) {
        throw new GalaxyFDSClientException("Fail to parse the result of" +
            " completing multipart upload. bucket name:" + bucketName
            + ", object name:" + objectName + ", upload ID:" + uploadId);
      }
      result.setFdsServiceBaseUri(config.getBaseUri());
      result.setCdnServiceBaseUri(config.getCdnBaseUri());
      return result;
    } catch (IOException e) {
      throw new GalaxyFDSClientException("Fail to complete multipart upload. "
          + "URI:" + uriString, e);
    } finally {
      if (responseContent != null) {
        try {
          responseContent.close();
        } catch (IOException e) {
          // Ignored
        }
      }
    }
  }

  private void abortMultipartUpload(String bucketName, String objectName,
      String uploadId) throws GalaxyFDSClientException {
    String uriString = config.getUploadBaseUri() + "/" + bucketName + "/"
        + objectName + "?uploadId=" + uploadId;

    InputStream responseContent = null;
    try {
      HttpUriRequest request = RequestFactory.createRequest(uriString,
          config.getCredential(), HttpMethod.DELETE, null);
      HttpResponse response = httpClient.execute(request);
      responseContent = response.getEntity().getContent();
      if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
        throw new GalaxyFDSClientException("Unable to upload object[" + bucketName
            + "/" + objectName + "] to URI :" + uriString
            + ". Fail to abort multipart upload: "
            + response.getStatusLine().toString());
      }
    } catch (IOException e) {
      throw new GalaxyFDSClientException("Fail to abort multipart upload. "
          + "URI:" + uriString, e);
    } finally {
      if (responseContent != null) {
        try {
          responseContent.close();
        } catch (IOException e) {
          // Ignored
        }
      }
    }
  }

  @Override
  public PutObjectResult putObject(String bucketName, File file)
      throws GalaxyFDSClientException {
    return putObject(bucketName, file, null);
  }

  @Override
  public PutObjectResult putObject(String bucketName, File file,
      List<UserParam> params) throws GalaxyFDSClientException {
    return putObject(bucketName, file, params, null);
  }

  @Override
  public PutObjectResult putObject(String bucketName, File file,
      List<UserParam> params, ProgressListener listener)
      throws GalaxyFDSClientException {
    return putObject(bucketName, null, file, params, listener);
  }

  @Override
  public PutObjectResult putObject(String bucketName, InputStream input,
      ObjectMetadata metadata) throws GalaxyFDSClientException {
    return putObject(bucketName, input, metadata, null);
  }

  @Override
  public PutObjectResult putObject(String bucketName, InputStream input,
      ObjectMetadata metadata, List<UserParam> params)
      throws GalaxyFDSClientException {
    return putObject(bucketName, input, metadata, params, null);
  }

  @Override
  public PutObjectResult putObject(String bucketName, InputStream input,
      ObjectMetadata metadata, List<UserParam> params, ProgressListener listener)
      throws GalaxyFDSClientException {
    return putObject(bucketName, null, input, metadata, params, listener);
  }

  @Override
  public boolean doesObjectExist(String bucketName, String objectName)
      throws GalaxyFDSClientException {
    Args.notNull(bucketName, "bucket name");
    Args.notEmpty(bucketName, "bucket name");
    Args.notNull(objectName, "object name");
    Args.notEmpty(objectName, "object name");

    String uriString = config.getBaseUri() + "/" + bucketName + "/"
        + objectName;
    try {
      HttpUriRequest request = RequestFactory.createRequest(uriString,
          config.getCredential(), HttpMethod.HEAD, null);
      HttpResponse response = httpClient.execute(request);
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == HttpStatus.SC_OK) {
        return true;
      } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
        return false;
      } else {
        throw new GalaxyFDSClientException("Unable to head object[" + bucketName
            + "/" + objectName + "] from URI :" + uriString + ". Cause:"
            + response.getStatusLine().toString());
      }
    } catch (IOException e) {
      throw new GalaxyFDSClientException("Unable to head object[" + bucketName +
          "/" + objectName + "] from URI :" + uriString + " Exception:"
          + e.getMessage(), e);
    }
  }
}
