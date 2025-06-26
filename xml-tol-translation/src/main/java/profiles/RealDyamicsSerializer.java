package xmltol.profiles;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;

import java.io.IOException;

public class RealDyamicsSerializer extends StdSerializer<RealDynamics> {
  public RealDyamicsSerializer() {
    super(RealDynamics.class);
  }

  public void serialize(RealDynamics realDynamics, JsonGenerator jsonGenerator, SerializerProvider provider) throws
                                                                                                             IOException
  {
    jsonGenerator.writeStartObject();
    jsonGenerator.writeNumberField("initial",realDynamics.initial);
    jsonGenerator.writeNumberField("rate",realDynamics.rate);
    jsonGenerator.writeEndObject();
  }
}
