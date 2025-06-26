package xmltol.profiles;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.io.IOException;

public class DurationSerializer extends StdSerializer<Duration> {
  public DurationSerializer() {
    super(Duration.class);
  }

  public void serialize(Duration duration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws
                                                                                                               IOException
  {
    jsonGenerator.writeStartObject();
    //jsonGenerator.writeNumber(duration.micros());
    jsonGenerator.writeEndObject();
  }
}
