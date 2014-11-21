package com.xiaomi.infra.galaxy.fds.android.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.webkit.MimeTypeMap;

import com.xiaomi.infra.galaxy.fds.android.exception.GalaxyFDSClientException;
import com.xiaomi.infra.galaxy.fds.android.model.FDSObject;

public class Util {
  private static final int BUFFER_SIZE = 4096;

  private static final ThreadLocal<SimpleDateFormat> DATE_FOPMAT =
      new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
          SimpleDateFormat format = new SimpleDateFormat(
              "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
          format.setTimeZone(TimeZone.getTimeZone("GMT"));
          return format;
        }
      };

  /**
   * Download an object to the specified file
   *
   * @param object          The FDS object contains a reference to an
   *                        InputStream contains the object's data
   * @param destinationFile The file to store the object's data
   * @param isAppend          If append to the end of the file or overwrite it
   * @throws GalaxyFDSClientException
   */
  public static void downloadObjectToFile(FDSObject object,
      File destinationFile, boolean isAppend) throws GalaxyFDSClientException {
    // attempt to create the parent if it doesn't exist
    File parentDirectory = destinationFile.getParentFile();
    if (!isAppend && parentDirectory != null && !parentDirectory.exists() ) {
      parentDirectory.mkdirs();
    }

    int bytesRead = 0;
    byte[] buffer = new byte[BUFFER_SIZE];
    InputStream in = object.getObjectContent();
    OutputStream out = null;
    try {
      out = new BufferedOutputStream(new FileOutputStream(destinationFile,
          isAppend));
      while ((bytesRead = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
        out.write(buffer, 0, bytesRead);
      }
    } catch (IOException e) {
      throw new GalaxyFDSClientException("Unable to store object["
          + object.getBucketName() + "/" + object.getObjectName() + "] content "
          + "to disk:" + e.getMessage(), e);
    } finally {
      try {
        in.close();
        if (out != null) {
          out.close();
        }
      } catch (IOException e) {
        // Ignore exceptions caused by close
      }
    }
  }

  public static String getStackTrace(Exception e) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    e.printStackTrace(printWriter);
    printWriter.flush();
    return stringWriter.toString();
  }

  /**
   * Determines the mimetype of a file by looking up the file's extension in an
   * internal listing to find the corresponding mime type. If the file has no
   * extension, or the extension is not available in the listing contained in
   * this class, the default mimetype <code>application/octet-stream</code>
   * is returned.
   * <p>
   * A file extension is one or more characters that occur after the last period
   * (.) in the file's name. If a file has no extension,
   * Guesses the mimetype of file data based on the file's extension.
   *
   * @param file * the file whose extension may match a known mimetype.
   *
   * @return the file's mimetype based on its extension, or a default value of
   * <code>application/octet-stream</code> if a mime type value cannot be found.
   */
  public static String getMimeType(File file) {
    String fileName = file.getName();
    int lastPeriodIndex = fileName.lastIndexOf(".");
    if (lastPeriodIndex > 0 && lastPeriodIndex + 1 < fileName.length()) {
      String ext = fileName.substring(lastPeriodIndex + 1);
      String mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
      if (mimetype != null) {
        return mimetype;
      }
    }
    return Consts.APPLICATION_OCTET_STREAM;
  }

  /**
   * Parses date string to Date object
   * @throws ParseException
   */
  public static Date parseDate(String dateString) throws ParseException {
    return DATE_FOPMAT.get().parse(dateString);
  }

  /**
   * Formats the date object to string
   */
  public static String formatDateString(Date date) {
    return DATE_FOPMAT.get().format(date);
  }
}
