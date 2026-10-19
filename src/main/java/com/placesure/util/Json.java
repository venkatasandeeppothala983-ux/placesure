package com.placesure.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

public final class Json {
  public static final ObjectMapper MAPPER =
      new ObjectMapper()
          .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

  private Json() {}

  public static String stringify(Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Could not encode a saved snapshot.", e);
    }
  }

  public static <T> T parse(String value, Class<T> type) {
    try {
      return MAPPER.readValue(value, type);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Could not read a saved snapshot.", e);
    }
  }
}
