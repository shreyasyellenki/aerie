package xmltol.profiles;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class ExternalDatasetHeaderSerializer extends StdSerializer<ExternalDatasetHeader> {
  public ExternalDatasetHeaderSerializer() {
    super(ExternalDatasetHeader.class);
  }

  public void serialize(ExternalDatasetHeader value, JsonGenerator jsonGenerator, SerializerProvider provider) throws
                                                                                                      IOException
  {
    jsonGenerator.writeStartObject();
    jsonGenerator.writeNumberField("planId", value.getPlanId());
    if(value.getSimulationDatasetId() >= 0){
      jsonGenerator.writeNumberField("simulationDatasetId", value.getSimulationDatasetId());
    }
    jsonGenerator.writeStringField("datasetStart", value.getStartTime());
    jsonGenerator.writeEndObject();
  }
}
