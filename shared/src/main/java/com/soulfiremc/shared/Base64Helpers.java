package com.soulfiremc.shared;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

public class Base64Helpers {
  public static String joinBase64(String[] args) {
    return Arrays.stream(args)
      .map(s -> Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)))
      .collect(Collectors.joining(","));
  }

  public static String[] splitBase64(String base64) {
    return Arrays.stream(base64.split(","))
      .map(s -> new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8))
      .toArray(String[]::new);
  }
}
