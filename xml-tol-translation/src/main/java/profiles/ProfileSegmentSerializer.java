package xmltol.profiles;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.nasa.jpl.aerie.merlin.driver.engine.ProfileSegment;

import java.io.IOException;
import java.util.Optional;

public class ProfileSegmentSerializer extends StdSerializer<ProfileSegment> {
  public ProfileSegmentSerializer() {
    super(ProfileSegment.class);
  }



  public void serialize(ProfileSegment profileSegment, JsonGenerator jsonGenerator, SerializerProvider provider) throws
                                                                                                                 IOException
  {
    jsonGenerator.writeStartObject();
    jsonGenerator.writeObjectField("duration", profileSegment.extent().micros());
    if(profileSegment.dynamics() instanceof Optional<?>){
      Optional<?> value = (Optional<?>) profileSegment.dynamics();
      if(value.isPresent()){
        jsonGenerator.writeObjectField("dynamics", value.get());
      }else {
        jsonGenerator.writeNullField("null");
      }
    }
    else {
      jsonGenerator.writeObjectField("dynamics", profileSegment.dynamics());
    }
    jsonGenerator.writeEndObject();
  }
}
