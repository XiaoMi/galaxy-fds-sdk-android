package com.xiaomi.infra.galaxy.fds.android.model;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Represents an object stored in Galaxy FDS. This object contains the data
 * content and the object metadata stored by Galaxy FDS, such as content type,
 * content length, etc.
 *
 * @see ObjectMetadata
 */
public class FDSObject implements Closeable {

  /**
   * The name of the object
   */
  private final String objectName;

  /**
   * The name of the bucket in which this object is contained
   */
  private final String bucketName;

  /**
   * The metadata stored by Galaxy FDS for this object
   */
  private ObjectMetadata metadata;

  /**
   * The stream containing the contents of this object from FDS
   */
  private InputStream objectContent;

  public FDSObject(String bucketName, String objectName) {
    this.bucketName = bucketName;
    this.objectName = objectName;
  }

  /**
   * Gets the name of the object
   *
   * @return The name of the object
   */
  public String getObjectName() {
    return objectName;
  }

  /**
   * Gets the name of the bucket in which this object is contained.
   *
   * @return The name of the bucket in which this object is contained.
   */
  public String getBucketName() {
    return bucketName;
  }

  /**
   * Gets the metadata stored by Galaxy FDS for this object. The
   * {@link ObjectMetadata} object includes any custom user metadata supplied by
   * the caller when the object was uploaded, as well as HTTP metadata such as
   * content length and content type.
   *
   * @return The metadata stored by Galaxy FDS for this object.
   * @see FDSObject#getObjectContent()
   */
  public ObjectMetadata getObjectMetadata() {
    return metadata;
  }

  /**
   * Sets the object metadata for this object in memory.
   * <p/>
   * <b>NOTE:</b> This does not update the object metadata stored in Galaxy
   * FDS, but only updates this object in local memory.
   *
   * @param metadata The new metadata to set for this object in memory.
   */
  public void setObjectMetadata(ObjectMetadata metadata) {
    this.metadata = metadata;
  }

  /**
   * Gets an input stream containing the contents of this object. Callers should
   * close this input stream as soon as possible, because the object contents
   * aren't buffered in memory and stream directly from Galaxy FDS.
   *
   * @return An input stream containing the contents of this object.
   * @see FDSObject#setObjectContent(InputStream)
   */
  public InputStream getObjectContent() {
    return objectContent;
  }

  /**
   * Sets the input stream containing this object's contents.
   *
   * @param objectContent The input stream containing this object's contents.
   * @see FDSObject#getObjectContent()
   */
  public void setObjectContent(InputStream objectContent) {
    this.objectContent = objectContent;
  }

  /**
   * Releases any underlying system resources. If the resources are already
   * released then invoking this method has no effect.
   */
  @Override
  public void close() {
    if (objectContent != null) {
      try {
        objectContent.close();
      } catch (IOException e) {
        // ignored
      }
    }
  }
}
