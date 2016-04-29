package com.xiaomi.infra.galaxy.fds.android.model;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;

import com.xiaomi.infra.galaxy.fds.android.util.Consts;
import com.xiaomi.infra.galaxy.fds.android.util.Util;

/**
 * Represents the object metadata that is stored with Galaxy FDS. This includes
 * custom user-supplied metadata, as well as the standard HTTP headers that
 * Galaxy FDS sends and receives (Content-Length, Content-Type, etc.).
 */
public class ObjectMetadata {
  private static final Set<String> PREDEFINED_HEADERS = new HashSet<String>();
  static {
    PREDEFINED_HEADERS.add(HttpHeaders.LAST_MODIFIED);
    PREDEFINED_HEADERS.add(HttpHeaders.CONTENT_MD5);
    PREDEFINED_HEADERS.add(HttpHeaders.CONTENT_TYPE);
    PREDEFINED_HEADERS.add(HttpHeaders.CONTENT_LENGTH);
    PREDEFINED_HEADERS.add(HttpHeaders.CONTENT_ENCODING);
    PREDEFINED_HEADERS.add(HttpHeaders.CACHE_CONTROL);
  }

  public static ObjectMetadata parseObjectMetadata(Header[] headers) {
    ObjectMetadata metadata = new ObjectMetadata();

    for (Header header : headers) {
      String key = header.getName();
      String value = header.getValue();
      // If user defined metadata, keep it
      if (key.startsWith(Consts.XIAOMI_META_HEADER_PREFIX)) {
        metadata.addUserMetadata(key, value);
      } else if (PREDEFINED_HEADERS.contains(key)) {
        metadata.addPredefinedMetadata(key, value);
      }
    }
    return metadata;
  }

  /**
   * Custom user metadata, represented in responses with the x-xiaomi-meta-
   * header prefix
   */
  private final Map<String, String> userMetadata = new HashMap<String, String>();

  /**
   * Predefined metadata
   */
  private final Map<String, String> predefinedMetadata =
      new HashMap<String, String>();

  /**
   * <p>
   * Gets the custom user-metadata for the associated object.
   * </p>
   * <p>
   * Galaxy FDS can store additional metadata on objects by internally
   * representing it as HTTP headers prefixed with "x-xiaomi-meta-". Use
   * user-metadata to store arbitrary metadata alongside their data in Galaxy
   * FDS. When setting user metadata, callers <i>should not</i> include the
   * internal "x-xiaomi-meta-" prefix; this library will handle that for them.
   * Likewise, when callers retrieve custom user-metadata, they will not see
   * the "x-xiaomi-meta-" header prefix.
   * </p>
   * <p>
   * User-metadata keys are <b>case insensitive</b> and will be returned as
   * lowercase strings, even if they were originally specified with uppercase
   * strings.
   * </p>
   * <p>
   * Note that user-metadata for an object is limited by the HTTP request
   * header limit. All HTTP headers included in a request (including user
   * metadata headers and other standard HTTP headers) must be less than 8KB.
   * </p>
   *
   * @return The custom user metadata for the associated object.
   */
  public Map<String, String> getUserMetadata() {
    return userMetadata;
  }

  /**
   * <p>
   * Adds the key value pair of custom user-metadata for the associated
   * object. If the entry in the custom user-metadata map already contains the
   * specified key, it will be replaced with these new contents.
   * </p>
   * <p>
   * Galaxy FDS can store additional metadata on objects by internally
   * representing it as HTTP headers prefixed with "x-xiaomi-meta-".
   * Use user-metadata to store arbitrary metadata alongside their data in
   * Galaxy FDS. When setting user metadata, callers <i>should not</i> include
   * the internal "x-xiaomi-meta-" prefix; this library will handle that for
   * them. Likewise, when callers retrieve custom user-metadata, they will not
   * see the "x-xiaomi-meta-" header prefix.
   * </p>
   * <p>
   * Note that user-metadata for an object is limited by the HTTP request
   * header limit. All HTTP headers included in a request (including user
   * metadata headers and other standard HTTP headers) must be less than 8KB.
   * </p>
   *
   * @param key   The key for the custom user metadata entry. Note that the key
   *              should not include
   *              the internal FDS HTTP header prefix.
   * @param value The value for the custom user-metadata entry.
   */
  public void addUserMetadata(String key, String value) {
    this.checkMetadata(key);
    this.userMetadata.put(key, value);
  }

  /**
   * <p>
   * Gets the Content-Length HTTP header indicating the size of the
   * associated object in bytes.
   * </p>
   * <p>
   * This field is required when uploading objects to FDS, but the Galaxy FDS
   * Java client will automatically set it when working directly with files.
   * When uploading directly from a stream, set this field if possible.
   * Otherwise the client must buffer the entire stream in order to calculate
   * the content length before sending the data to Galaxy FDS.
   * </p>
   * <p>
   * For more information on the Content-Length HTTP header, see <a
   * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.13">
   * http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.13</a>
   * </p>
   *
   * @return The Content-Length HTTP header indicating the size of the
   * associated object in bytes. Returns <code>-1</code> if it hasn't been set yet.
   */
  public long getContentLength() {
    String contentLength = predefinedMetadata.get(HttpHeaders.CONTENT_LENGTH);
    if (contentLength != null) {
      return Long.parseLong(contentLength);
    } else {
      return -1;
    }
  }

  /**
   * <p>
   * Sets the Content-Length HTTP header indicating the size of the
   * associated object in bytes.
   * </p>
   * <p>
   * This field is required when uploading objects to FDS, but the Galaxy FDS
   * Java client will automatically set it when working directly with files. When
   * uploading directly from a stream, set this field if
   * possible. Otherwise the client must buffer the entire stream in
   * order to calculate the content length before sending the data to
   * Galaxy FDS.
   * </p>
   * <p>
   * For more information on the Content-Length HTTP header, see <a
   * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.13">
   * http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.13</a>
   * </p>
   *
   * @param contentLength The Content-Length HTTP header indicating the size of
   *                      the associated object in bytes.
   * @see ObjectMetadata#getContentLength()
   */
  public void setContentLength(long contentLength) {
    predefinedMetadata.put(HttpHeaders.CONTENT_LENGTH,
        Long.toString(contentLength));
  }

  /**
   * <p>
   * Gets the base64 encoded 128-bit MD5 digest of the associated object
   * (content - not including headers) according to RFC 1864. This data is
   * used as a message integrity check to verify that the data received by
   * Galaxy FDS is the same data that the caller sent.
   * </p>
   * <p>
   * This field represents the base64 encoded 128-bit MD5 digest digest of an
   * object's content as calculated on the caller's side.
   * FDS.
   * </p>
   * <p>
   * The Galaxy FDS Java client will attempt to calculate this field automatically
   * when uploading files to Galaxy FDS.
   * </p>
   *
   * @return The base64 encoded MD5 hash of the content for the associated
   * object.  Returns <code>null</code> if the MD5 hash of the content
   * hasn't been set.
   * @see ObjectMetadata#setContentMD5(String)
   */
  public String getContentMD5() {
    return predefinedMetadata.get(HttpHeaders.CONTENT_MD5);
  }

  /**
   * <p>
   * Sets the base64 encoded 128-bit MD5 digest of the associated object
   * (content - not including headers) according to RFC 1864. This data is used
   * as a message integrity check to verify that the data received by Galaxy FDS
   * is the same data that the caller sent.
   * </p>
   * <p>
   * The Galaxy FDS Java client will attempt to calculate this field
   * automatically when uploading files to Galaxy FDS.
   * </p>
   *
   * @param contentMD5 The base64 encoded MD5 hash of the content for the object
   *                   associated with this metadata.
   * @see ObjectMetadata#getContentMD5()
   */
  public void setContentMD5(String contentMD5) {
    predefinedMetadata.put(HttpHeaders.CONTENT_MD5, contentMD5);
  }

  /**
   * <p>
   * Gets the Content-Type HTTP header, which indicates the type of content
   * stored in the associated object. The value of this header is a standard
   * MIME type.
   * </p>
   * <p>
   * When uploading files, the Galaxy FDS Java client will attempt to determine
   * the correct content type if one hasn't been set yet. Users are
   * responsible for ensuring a suitable content type is set when uploading
   * streams. If no content type is provided and cannot be determined by
   * the filename, the default content type, "application/octet-stream", will
   * be used.
   * </p>
   * <p>
   * For more information on the Content-Type header, see <a
   * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.17">
   * http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.17</a>
   * </p>
   *
   * @return The HTTP Content-Type header, indicating the type of content
   * stored in the associated FDS object. Returns <code>null</code>
   * if it hasn't been
   * set.
   * @see ObjectMetadata#setContentType(String)
   */
  public String getContentType() {
    return predefinedMetadata.get(HttpHeaders.CONTENT_TYPE);
  }

  /**
   * <p>
   * Sets the Content-Type HTTP header indicating the type of content stored in
   * the associated object. The value of this header is a standard MIME type.
   * </p>
   * <p>
   * When uploading files, the Galaxy FDS Java client will attempt to determine
   * the correct content type if one hasn't been set yet. Users are responsible
   * for ensuring a suitable content type is set when uploading streams. If no
   * content type is provided and cannot be determined by the filename, the
   * default content type "application/octet-stream" will be used.
   * </p>
   * <p>
   * For more information on the Content-Type header, see <a
   * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.17">
   * http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.17</a>
   * </p>
   *
   * @param contentType The HTTP Content-Type header indicating the type of
   *                    content stored in the associated FDS object.
   * @see ObjectMetadata#getContentType()
   */
  public void setContentType(String contentType) {
    predefinedMetadata.put(HttpHeaders.CONTENT_TYPE, contentType);
  }

  /**
   * <p>
   * Gets the optional Content-Encoding HTTP header specifying what content
   * encodings have been applied to the object and what decoding mechanisms must
   * be applied in order to obtain the media-type referenced by the Content-Type
   * field.
   * </p>
   * <p>
   * For more information on how the Content-Encoding HTTP header works, see
   * <a
   * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.11">
   * http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.11</a>
   * </p>
   *
   * @return The HTTP Content-Encoding header.
   * Returns <code>null</code> if it hasn't been set.
   * @see ObjectMetadata#setContentType(String)
   */
  public String getContentEncoding() {
    return predefinedMetadata.get(HttpHeaders.CONTENT_ENCODING);
  }

  /**
   * <p>
   * Sets the optional Content-Encoding HTTP header specifying what
   * content encodings have been applied to the object and what decoding
   * mechanisms must be applied in order to obtain the media-type referenced
   * by the Content-Type field.
   * </p>
   * <p>
   * For more information on how the Content-Encoding HTTP header works, see
   * <a
   * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.11">
   * http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.11</a>
   * </p>
   *
   * @param contentEncoding The HTTP Content-Encoding header, as defined in RFC
   *                        2616.
   * @see <a
   * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.11"
   * >http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.11</a>
   * @see ObjectMetadata#getContentType()
   */
  public void setContentEncoding(String contentEncoding) {
    predefinedMetadata.put(HttpHeaders.CONTENT_ENCODING, contentEncoding);
  }

  /**
   * <p>
   * Gets the optional Cache-Control HTTP header which allows the user to
   * specify caching behavior along the HTTP request/reply chain.
   * </p>
   * <p>
   * For more information on how the Cache-Control HTTP header affects HTTP
   * requests and responses, see <a
   * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9">
   * http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9</a>
   * </p>
   *
   * @return The HTTP Cache-Control header as defined in RFC 2616.
   * Returns <code>null</code>  if
   * it hasn't been set.
   * @see ObjectMetadata#setCacheControl(String)
   */
  public String getCacheControl() {
    return predefinedMetadata.get(HttpHeaders.CACHE_CONTROL);
  }

  /**
   * <p>
   * Sets the optional Cache-Control HTTP header which allows the user to
   * specify caching behavior along the HTTP request/reply chain.
   * </p>
   * <p>
   * For more information on how the Cache-Control HTTP header affects HTTP
   * requests and responses see <a
   * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9">
   * http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9</a>
   * </p>
   *
   * @param cacheControl The HTTP Cache-Control header as defined in RFC 2616.
   * @see ObjectMetadata#getCacheControl()
   */
  public void setCacheControl(String cacheControl) {
    predefinedMetadata.put(HttpHeaders.CACHE_CONTROL, cacheControl);
  }

  /**
   * Gets the value of the Last-Modified header, indicating the date
   * and time at which Galaxy FDS last recorded a modification to the
   * associated object.
   *
   * @return The date and time at which Galaxy FDS last recorded a modification
   * to the associated object. Returns <code>null</code> if
   * the Last-Modified header hasn't been set.
   */
  public Date getLastModified() {
    String lastModified = predefinedMetadata.get(HttpHeaders.LAST_MODIFIED);
    if (lastModified != null) {
      try {
        return Util.parseDate(lastModified);
      } catch (ParseException e) {
        return null;
      }
    } else {
      return null;
    }
  }

  /**
   * For internal use only. Sets the Last-Modified header value
   * indicating the date and time at which Galaxy FDS last recorded a
   * modification to the associated object.
   *
   * @param lastModified The date and time at which Galaxy FDS last recorded a
   *                     modification to the associated object.
   */
  public void setLastModified(Date lastModified) {
    predefinedMetadata.put(HttpHeaders.LAST_MODIFIED,
        Util.formatDateString(lastModified));
  }

  /**
   * For internal use only. Add predefined metadata
   * @param key   The key for the predefined metadata
   * @param value The value for the predefined metadata entry.
   */
  public void addPredefinedMetadata(String key, String value) {
    this.checkMetadata(key);
    predefinedMetadata.put(key, value);
  }

  /**
   * Get all the metadata entries
   * @return All the metadata entries include predefined and user-defined metadata
   */
  public Map<String, String> getAllMetadata() {
    Map<String, String> copy = new HashMap<String, String>(predefinedMetadata);
    copy.putAll(userMetadata);
    return copy;
  }

  private void checkMetadata(String key) {
    boolean isValid = key.startsWith(Consts.XIAOMI_META_HEADER_PREFIX);

    if (!isValid) {
      for (String m : PREDEFINED_HEADERS) {
        if (key.equals(m)) {                                           
          isValid = true;
          break;
        }
      }
    }

    if (!isValid) {
      throw new RuntimeException("Invalid metadata: " + key, null);
    }
  }
}
