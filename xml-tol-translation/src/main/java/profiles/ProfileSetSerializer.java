package xmltol.profiles;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.nasa.jpl.aerie.merlin.driver.engine.ProfileSegment;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.ProfileSet;

import java.io.IOException;
import java.util.Map;

public class ProfileSetSerializer extends StdSerializer<ProfileSet> {
  public ProfileSetSerializer() {
    super(ProfileSet.class);
  }

  public void serialize(ProfileSet profileSet, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws
                                                                                                                   IOException
  {
    ObjectMapper mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    SimpleModule simpleModule = new SimpleModule();

    module.addSerializer(ValueSchema.class, new LinearSchemaSerializer());
    module.addSerializer(Duration.class, new DurationSerializer());
    module.addSerializer(ProfileSegment.class, new ProfileSegmentSerializer());
    module.addSerializer(SerializedValue.class, new SerializedValueSerializer());
    module.addSerializer(RealDynamics.class,new RealDyamicsSerializer());
    module.addSerializer(ProfileSet.class, new ProfileSetSerializer());
    mapper.registerModule(module);


    jsonGenerator.writeStartObject();
    profileSet.realProfiles().forEach((key,value)->{
    try {
      jsonGenerator.writeFieldName(key);
      jsonGenerator.writeStartObject();
      jsonGenerator.writeStringField("type", "real");
      Map<String, Object> flattenedValue = mapper.convertValue(
          value, new TypeReference<Map<String, Object>>() {});
      for (Map.Entry<String, Object> inner : flattenedValue.entrySet()) {
        jsonGenerator.writeObjectField(inner.getKey(), inner.getValue());
      }

      jsonGenerator.writeEndObject();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  });
    simpleModule.addSerializer(ValueSchema.class, new ValueSchemaSerializer());
    simpleModule.addSerializer(Duration.class, new DurationSerializer());
    simpleModule.addSerializer(ProfileSegment.class, new ProfileSegmentSerializer());
    simpleModule.addSerializer(SerializedValue.class, new SerializedValueSerializer());
    simpleModule.addSerializer(RealDynamics.class,new RealDyamicsSerializer());
    simpleModule.addSerializer(ProfileSet.class, new ProfileSetSerializer());
    ObjectMapper mapper2 = new ObjectMapper();
    mapper2.registerModule(simpleModule);
    profileSet.discreteProfiles().forEach((key,value)->{
      try {
        jsonGenerator.writeFieldName(key);
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("type", "discrete");
        Map<String, Object> flattenedValue = mapper2.convertValue(
            value, new TypeReference<Map<String, Object>>() {});
        for (Map.Entry<String, Object> inner : flattenedValue.entrySet()) {
          jsonGenerator.writeObjectField(inner.getKey(), inner.getValue());
        }

        jsonGenerator.writeEndObject();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    jsonGenerator.writeEndObject();

  }


}

 /*profileSet.realProfiles().forEach((key,value)->{
      try {
        jsonGenerator.writeObjectField(key,value);
        //EthicalProfile ep = new EthicalProfile(value,false);


      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    profileSet.discreteProfiles().forEach((key,value)->{
      try {
        jsonGenerator.writeObjectField(key,value);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });*/
