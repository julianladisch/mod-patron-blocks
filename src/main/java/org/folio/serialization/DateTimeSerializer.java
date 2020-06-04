package org.folio.serialization;

import java.io.IOException;

import org.joda.time.DateTime;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class DateTimeSerializer extends JsonSerializer<DateTime> {
  @Override
  public void serialize(DateTime dateTime, JsonGenerator jsonGenerator,
    SerializerProvider serializerProvider) throws IOException {

    jsonGenerator.writeString(dateTime.toString());
  }
}
