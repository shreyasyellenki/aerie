package xmltol.profiles;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.io.IOException;

public class LinearSchemaSerializer extends StdSerializer<ValueSchema> {
  public LinearSchemaSerializer() {
    super(ValueSchema.class);
  }

  public void serialize(ValueSchema valueSchema, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws
                                                                                                                     IOException
  {
    jsonGenerator.writeStartObject();
    jsonGenerator.writeStringField("type", "struct");
    jsonGenerator.writeFieldName("items");
    jsonGenerator.writeStartObject();
    jsonGenerator.writeFieldName("rate");
    jsonGenerator.writeStartObject();
    jsonGenerator.writeStringField("type", "real");
    jsonGenerator.writeEndObject();
    jsonGenerator.writeFieldName("initial");
    jsonGenerator.writeStartObject();
    jsonGenerator.writeStringField("type", "real");
    jsonGenerator.writeEndObject();
    jsonGenerator.writeEndObject();
    jsonGenerator.writeEndObject();
  }
}
