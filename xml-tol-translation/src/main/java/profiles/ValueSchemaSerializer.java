package xmltol.profiles;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.io.IOException;

public class ValueSchemaSerializer extends StdSerializer<ValueSchema> {
  public ValueSchemaSerializer() {
    super(ValueSchema.class);
  }

  public String getSerializedValueType(ValueSchema valueSchema){
    if (valueSchema == ValueSchema.REAL){
      return "real";
    }
    else if (valueSchema == ValueSchema.BOOLEAN){
      return "boolean";
    }
    else if (valueSchema == ValueSchema.INT){
      return "int";
    }
    else if (valueSchema == ValueSchema.STRING){
      return "string";
    }
    else if (valueSchema == ValueSchema.DURATION){
      return "duration";
    }
    else
      return "";
  }

  public void serialize(ValueSchema valueSchema, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws
                                                                                                                     IOException
  {
    jsonGenerator.writeStartObject();
    jsonGenerator.writeStringField("type", getSerializedValueType(valueSchema));
    //jsonGenerator.writeEndObject();
    jsonGenerator.writeEndObject();
  }
}
