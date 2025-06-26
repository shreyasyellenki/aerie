package xmltol.profiles;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.io.IOException;

public class SerializedValueSerializer extends StdSerializer<SerializedValue> {
  public SerializedValueSerializer() {
    super(SerializedValue.class);
  }

  public void serialize(SerializedValue value, JsonGenerator gen, SerializerProvider provider) throws IOException {

    //gen.writeStartObject();
    if(value.asBoolean().isPresent()){
      gen.writeBoolean(value.asBoolean().get());
    }
    else if(value.asString().isPresent()){
      gen.writeString(value.asString().get());
    }
    else if(value.asInt().isPresent()){
      gen.writeNumber(value.asInt().get());
    }
    else if(value.asReal().isPresent()){
      gen.writeNumber(value.asReal().get());
    }

    //gen.writeEndObject();
  }
}
