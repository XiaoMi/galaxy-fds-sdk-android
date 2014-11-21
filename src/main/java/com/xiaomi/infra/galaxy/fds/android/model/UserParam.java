package com.xiaomi.infra.galaxy.fds.android.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UserParam {
  protected final Map<String, String> params = new HashMap<String, String>();

  public Map<String, String> getParams() {
    return Collections.unmodifiableMap(params);
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    int index = 0;
    for (Map.Entry<String, String> entry : params.entrySet()) {
      if (index != 0) {
        stringBuilder.append('&');
      }
      stringBuilder.append(entry.getKey());
      String value = entry.getValue();
      if (value != null) {
        stringBuilder.append('=');
        stringBuilder.append(value);
      }
      index++;
    }
    return stringBuilder.toString();
  }
}
