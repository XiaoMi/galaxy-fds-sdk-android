package com.xiaomi.infra.galaxy.fds.android;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import com.xiaomi.infra.galaxy.fds.android.exception.GalaxyFDSClientException;
import com.xiaomi.infra.galaxy.fds.android.model.FDSObject;
import com.xiaomi.infra.galaxy.fds.android.model.ObjectMetadata;
import com.xiaomi.infra.galaxy.fds.android.model.ProgressListener;
import com.xiaomi.infra.galaxy.fds.android.model.PutObjectResult;
import com.xiaomi.infra.galaxy.fds.android.model.UserParam;

public interface GalaxyFDSClient {

  /**
   * @see #getObject(String, String, long, List, ProgressListener, boolean)
   */
  FDSObject getObject(String bucketName, String objectName)
      throws GalaxyFDSClientException;

  /**
   * @see #getObject(String, String, long, List, ProgressListener, boolean)
   */
  FDSObject getObject(String bucketName, String objectName, long offset,
      List<UserParam> params) throws GalaxyFDSClientException;

  /**
   * @see #getObject(String, String, long, List, ProgressListener, boolean)
   */
  FDSObject getObject(String bucketName, String objectName, long offset,
      List<UserParam> params, ProgressListener progressListener)
      throws GalaxyFDSClientException;

  /**
   * <p>
   * Gets part of object stored in Galaxy FDS under the specified bucket, with
   * user defined parameters and with a progress listener which will notify the
   * caller periodically of how many bytes have been read.
   * Content range of the got object is from [offset, object size).
   * </p>
   * <p>
   * Be extremely careful when using this method; the returned Galaxy FDS object
   * contains a direct stream of data from the HTTP connection. The underlying
   * HTTP connection cannot be closed until the user finishes reading the data
   * and closes the stream.
   * Therefore:
   * </p>
   * <ul>
   * <li>Use the data from the input stream in Galaxy FDS object as soon as
   * possible</li>
   * <li>Close the input stream in Galaxy FDS object as soon as possible</li>
   * </ul>
   * If these rules are not followed, the client can run out of resources by
   * allocating too many open, but unused, HTTP connections.
   * </p>
   * <p>
   * To get an object from Galaxy FDS, the caller must have read permission
   * access to the object.
   * </p>
   *
   * @param bucketName       The name of the bucket containing the desired object.
   * @param objectName       The name of the object to be downloaded
   * @param offset           The position of object content where client begins
   *                         downloading
   * @param params           The user defined parameters
   * @param progressListener The progress listener for receiving updates about
   *                         object download status.
   * @param fromCdn          Whether get object from cdn or not
   * @return The object stored in Galaxy FDS in the specified bucket and object.
   * @throws GalaxyFDSClientException If any errors are encountered in the client
   *                                  while making the request or handling the
   *                                  response.
   */
  FDSObject getObject(String bucketName, String objectName, long offset,
      List<UserParam> params, ProgressListener progressListener, boolean fromCdn)
      throws GalaxyFDSClientException;

  /**
   * <p>
   * Gets object stored in Galaxy FDS from URL, with user defined parameters
   * and with a progress listener which will notify the caller periodically
   * of how many bytes have been read. Content range of the got object is
   * from [offset, object size).
   * </p>
   * <p>
   * Be extremely careful when using this method; the returned Galaxy FDS object
   * contains a direct stream of data from the HTTP connection. The underlying
   * HTTP connection cannot be closed until the user finishes reading the data
   * and closes the stream.
   * Therefore:
   * </p>
   * <ul>
   * <li>Use the data from the input stream in Galaxy FDS object as soon as
   * possible</li>
   * <li>Close the input stream in Galaxy FDS object as soon as possible</li>
   * </ul>
   * If these rules are not followed, the client can run out of resources by
   * allocating too many open, but unused, HTTP connections.
   * </p>
   * <p>
   * To get an object from Galaxy FDS, the caller must have read permission
   * access to the object.
   * </p>
   *
   * @param uriString        The preSignedUrl for getting object without authentication
   * @param offset           The position of object content where client begins
   *                         downloading
   * @param params           The user defined parameters
   * @param progressListener The progress listener for receiving updates about
   *                         object download status.
   * @param progressListener The progress listener for receiving updates about
   *                         object download status.
   * @return The object stored in Galaxy FDS in the specified bucket and object.
   * @throws GalaxyFDSClientException If any errors are encountered in the client
   *                                  while making the request or handling the
   *                                  response.
   */
  FDSObject getObject(String uriString, long offset, List<UserParam> params,
      ProgressListener progressListener) throws GalaxyFDSClientException;

  /**
   * @see #getObject(String, String, File, List, ProgressListener, boolean)
   */
  ObjectMetadata getObject(String bucketName, String objectName,
      File destinationFile) throws GalaxyFDSClientException;

  /**
   * @see #getObject(String, String, File, List, ProgressListener, boolean)
   */
  ObjectMetadata getObject(String bucketName, String objectName,
      File destinationFile, List<UserParam> params)
      throws GalaxyFDSClientException;

  /**
   * @see #getObject(String, String, File, List, ProgressListener, boolean)
   */
  ObjectMetadata getObject(String bucketName, String objectName,
      File destinationFile, List<UserParam> params,
      ProgressListener progressListener)
      throws GalaxyFDSClientException;

  /**
   * <p>
   * Gets the object metadata for the object stored in Galaxy FDS under the
   * specified bucket and object, and saves the object contents to the specified
   * file.
   * Progress listener which will notify the caller periodically of how many
   * bytes have been read.
   * </p>
   * <p>
   * Instead of using {@link GalaxyFDSClient#getObject(String, String)}, use
   * this method to ensure that the underlying HTTP stream resources are
   * automatically closed as soon as possible. The Galaxy FDS clients handles
   * immediate storage of the object contents to the specified file.
   * </p>
   * <p>
   * To get an object from Galaxy FDS, the caller must have read permission to
   * access to the object.
   * </p>
   *
   * @param bucketName       The name of the bucket containing the desired object.
   * @param objectName       The name of the object to be downloaded
   * @param destinationFile  Indicates the file (which might already exist) where
   *                         to save the object content being downloading from
   *                         Galaxy FDS.
   * @param params           The user defined parameters
   * @param progressListener The progress listener for receiving updates about
   *                         object download status.
   * @param fromCdn          Whether get object from cdn or not
   * @return All object metadata for the specified object.
   * @throws GalaxyFDSClientException If any errors are encountered in the client
   *                                  while making the request or handling the
   *                                  response, or writing the incoming data
   *                                  from FDS to the specified destination file.
   *                                  destination file.
   */
  ObjectMetadata getObject(String bucketName, String objectName,
      File destinationFile, List<UserParam> params,
      ProgressListener progressListener, boolean fromCdn)
      throws GalaxyFDSClientException;

  /**
   * @see #getObject(String, long, List, ProgressListener)
   */
  ObjectMetadata getObject(String uriString, File destinationFile,
      List<UserParam> params, ProgressListener progressListener)
      throws GalaxyFDSClientException;

  /**
   * @see #putObject(String, String, File, List, ProgressListener)
   */
  PutObjectResult putObject(String bucketName, String objectName, File file)
      throws GalaxyFDSClientException;

  /**
   * @see #putObject(String, String, File, List, ProgressListener)
   */
  PutObjectResult putObject(String bucketName, String objectName, File file,
      List<UserParam> params) throws GalaxyFDSClientException;

  /**
   * <p>
   * Uploads the specified file to Galaxy FDS under the specified bucket and
   * object name.
   * </p>
   * <p>
   * Galaxy FDS never stores partial objects;
   * if during this call an exception wasn't thrown,
   * the entire object was stored.
   * </p>
   * <p>
   * The client automatically computes
   * a checksum of the file.
   * Galaxy FDS uses checksums to validate the data in each file.
   * </p>
   * <p>
   * Using the file extension, Galaxy FDS attempts to determine
   * the correct content type and content disposition to use
   * for the object.
   * </p>
   * <p>
   * This operation will overwrite an existing object with the same object;
   * Galaxy FDS will store the last write request. Galaxy FDS does not provide
   * object locking. If Galaxy FDS receives multiple write requests for the same
   * object nearly simultaneously, all of the objects might be stored. However,
   * a single object will be stored with the final write request.
   * </p>
   * <p>
   * The specified bucket must already exist and the caller must have write
   * permission to the bucket to upload an object.
   * </p>
   *
   * @param bucketName       The name of an existing bucket, to which you have
   *                         write permission.
   * @param objectName       The name of the object to be downloaded
   * @param file             The file containing the data to be uploaded to
   *                         Galaxy FDS.
   * @param params           The user defined parameters
   * @param progressListener The progress listener for receiving updates about
   *                         object download status.
   * @return A {@link PutObjectResult} object containing the information
   * returned by Galaxy FDS for the newly created object.
   * @throws GalaxyFDSClientException If any errors are encountered in the client
   *                                  while making the request or handling the
   *                                  response.
   */
  PutObjectResult putObject(String bucketName, String objectName, File file,
      List<UserParam> params, ProgressListener progressListener)
      throws GalaxyFDSClientException;

  /**
   * @see #putObject(String, String, InputStream, ObjectMetadata, List, ProgressListener)
   */
  PutObjectResult putObject(String bucketName, String objectName,
      InputStream input, ObjectMetadata metadata) throws GalaxyFDSClientException;

  /**
   * @see #putObject(String, String, InputStream, ObjectMetadata, List, ProgressListener)
   */
  PutObjectResult putObject(String bucketName, String objectName,
      InputStream input, ObjectMetadata metadata, List<UserParam> params)
      throws GalaxyFDSClientException;

  /**
   * <p>
   * Uploads the specified input stream and object metadata to Galaxy FDS under
   * the specified bucket and object name. The input stream will be closed
   * whether or not putObject() succeed.
   * Progress listener which will notify the caller periodically of how many
   * bytes have been uploaded.
   * </p>
   * <p>
   * Galaxy FDS never stores partial objects; if during this call an exception
   * wasn't thrown, the entire object was stored.
   * </p>
   * <p>
   * The client automatically computes
   * a checksum of the file. This checksum is verified against another checksum
   * that is calculated once the data reaches Galaxy FDS, ensuring the data
   * has not corrupted in transit over the network.
   * </p>
   * <p>
   * Content length <b>must</b> be specified before data can be uploaded to
   * Galaxy FDS. If the caller doesn't provide it, the library will <b>have
   * to</b> buffer the contents of the input stream in order to calculate it
   * because Galaxy FDS explicitly requires that the content length be sent in
   * the request headers before any of the data is sent.
   * </p>
   * <p/>
   * <p>
   * this operation will overwrite an existing object with the same object;
   * Galaxy FDS will store the last write request. Galaxy FDS does not provide
   * object locking.
   * If Galaxy FDS receives multiple write requests for the same object nearly
   * simultaneously, all of the objects might be stored.  However, a single
   * object will be stored with the final write request.
   * </p>
   * <p/>
   * <p>
   * The specified bucket must already exist and the caller must have write
   * permission to the bucket to upload an object.
   * </p>
   *
   * @param bucketName       The name of an existing bucket, to which you have
   *                         write permission.
   * @param objectName       The name of the object to be downloaded
   * @param input            The input stream containing the data to be uploaded.
   * @param metadata         Additional metadata instructing Galaxy FDS how to
   *                         handle the uploaded data (e.g. custom user metadata,
   *                         hooks for specifying content type, etc.).
   * @param params           The user defined parameters
   * @param progressListener The progress listener for receiving updates about
   *                         object download status.
   * @return A {@link PutObjectResult} object containing the information returned
   * by Galaxy FDS for the newly created object.
   * @throws GalaxyFDSClientException If any errors are encountered in the client
   *                                  while making the request or handling the
   *                                  response.
   */
  PutObjectResult putObject(String bucketName, String objectName,
      InputStream input, ObjectMetadata metadata, List<UserParam> params,
      ProgressListener progressListener) throws GalaxyFDSClientException;

  /**
   * @see #putObject(String, File, List, ProgressListener)
   */
  PutObjectResult putObject(String bucketName, File file)
      throws GalaxyFDSClientException;

  /**
   * @see #putObject(String, File, List, ProgressListener)
   */
  PutObjectResult putObject(String bucketName, File file, List<UserParam> params)
      throws GalaxyFDSClientException;

  /**
   * <p>
   * Uploads the specified file to Galaxy FDS under the specified bucket,
   * FDS service will generate a unique object name.
   * Progress listener which will notify the caller periodically of how many
   * bytes have been uploaded.
   * </p>
   * <p>
   * Galaxy FDS never stores partial objects; if during this call an exception
   * wasn't thrown, the entire object was stored.
   * </p>
   * <p>
   * The client automatically computes a checksum of the file. Galaxy FDS uses
   * checksums to validate the data in each file.
   * </p>
   * <p>
   * Using the file extension, Galaxy FDS attempts to determine the correct
   * content type and content disposition to use for the object.
   * </p>
   * <p>
   * This operation will create a new object, whose name will be unique in the
   * specified bucket.
   * </p>
   * <p>
   * The specified bucket must already exist and the caller must have write
   * permission to the bucket to upload an object.
   * </p>
   *
   * @param bucketName       The name of an existing bucket, to which you have
   *                         write permission.
   * @param file             The file containing the data to be uploaded to
   *                         Galaxy FDS.
   * @param params           The user defined parameters
   * @param progressListener The progress listener for receiving updates about
   *                         object upload status.
   * @return A {@link PutObjectResult} object containing the information
   * returned by Galaxy FDS for the newly created object.
   * @throws GalaxyFDSClientException If any errors are encountered in the client
   *                                  while making the request or handling the
   *                                  response.
   */
  PutObjectResult putObject(String bucketName, File file, List<UserParam> params,
      ProgressListener progressListener) throws GalaxyFDSClientException;

  /**
   * @see #putObject(String, InputStream, ObjectMetadata, List, ProgressListener)
   */
  PutObjectResult putObject(String bucketName, InputStream input,
      ObjectMetadata metadata) throws GalaxyFDSClientException;

  /**
   * @see #putObject(String, InputStream, ObjectMetadata, List, ProgressListener)
   */
  PutObjectResult putObject(String bucketName, InputStream input,
      ObjectMetadata metadata, List<UserParam> params)
      throws GalaxyFDSClientException;

  /**
   * <p>
   * Uploads the specified file to Galaxy FDS under the specified bucket, the
   * input stream will be closed whether or not putObject() succeed
   * FDS service will generate a unique object name.
   * Progress listener which will notify the caller periodically of how many
   * bytes have been uploaded.
   * </p>
   * <p>
   * Galaxy FDS never stores partial objects; if during this call an exception
   * wasn't thrown, the entire object was stored.
   * </p>
   * <p>
   * The client automatically computes a checksum of the file. Galaxy FDS uses
   * checksums to validate the data in each file.
   * </p>
   * <p>
   * Content length <b>must</b> be specified before data can be uploaded to
   * Galaxy FDS. If the caller doesn't provide it, the library will <b>have
   * to</b> buffer the contents of the input stream in order to calculate it
   * because Galaxy FDS explicitly requires that the content length be sent in
   * the request headers before any of the data is sent.
   * </p>
   * <p>
   * This operation will create a new object, whose name will be unique in the
   * specified bucket.
   * </p>
   * <p>
   * The specified bucket must already exist and the caller must have write
   * permission to the bucket to upload an object.
   * </p>
   *
   * @param bucketName       The name of an existing bucket, to which you have
   *                         write permission.
   * @param input            The input stream containing the data to be uploaded.
   * @param metadata         Additional metadata instructing Galaxy FDS how to
   *                         handle the uploaded data (e.g. custom user metadata,
   *                         hooks for specifying content type, etc.).
   * @param params           The user defined parameters
   * @param progressListener The progress listener for receiving updates about
   *                         object download status.
   * @return A {@link PutObjectResult} object containing the information
   * returned by Galaxy FDS for the newly created object.
   * @throws GalaxyFDSClientException If any errors are encountered in the client
   *                                  while making the request or handling the
   *                                  response.
   */
  PutObjectResult putObject(String bucketName, InputStream input,
      ObjectMetadata metadata, List<UserParam> params,
      ProgressListener progressListener) throws GalaxyFDSClientException;

  /**
   * Test if object exists.
   *
   * @param bucketName The name of the bucket containing the desired object.
   * @param objectName The name of the object to be downloaded
   * @return True if object exists, otherwise false
   * @throws GalaxyFDSClientException
   */
  boolean doesObjectExist(String bucketName, String objectName)
      throws GalaxyFDSClientException;
}
