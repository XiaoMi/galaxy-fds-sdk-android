package com.xiaomi.infra.galaxy.fds.android.util;

public class Args {

  public static void check(final boolean expression, final String message) {
    if (!expression) {
      throw new IllegalArgumentException(message);
    }
  }

  public static void check(final boolean expression, final String message,
      final Object... args) {
    if (!expression) {
      throw new IllegalArgumentException(String.format(message, args));
    }
  }

  public static void check(final boolean expression, final String message,
      final Object arg) {
    if (!expression) {
      throw new IllegalArgumentException(String.format(message, arg));
    }
  }

  public static <T> T notNull(final T argument, final String name) {
    if (argument == null) {
      throw new IllegalArgumentException(name + " may not be null");
    }
    return argument;
  }

  public static <T extends CharSequence> T notEmpty(final T argument,
      final String name) {
    if (argument == null) {
      throw new IllegalArgumentException(name + " may not be null");
    }
    if (argument.length() == 0) {
      throw new IllegalArgumentException(name + " may not be empty");
    }
    return argument;
  }

  public static int positive(final int n, final String name) {
    if (n <= 0) {
      throw new IllegalArgumentException(name + " may not be negative or zero");
    }
    return n;
  }

  public static long positive(final long n, final String name) {
    if (n <= 0) {
      throw new IllegalArgumentException(name + " may not be negative or zero");
    }
    return n;
  }

  public static int notNegative(final int n, final String name) {
    if (n < 0) {
      throw new IllegalArgumentException(name + " may not be negative");
    }
    return n;
  }

  public static long notNegative(final long n, final String name) {
    if (n < 0) {
      throw new IllegalArgumentException(name + " may not be negative");
    }
    return n;
  }

}
