package xmltol.profiles;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nasa.jpl.aerie.merlin.driver.engine.ProfileSegment;
import gov.nasa.jpl.aerie.merlin.driver.resources.ResourceProfile;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.ProfileSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;


import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class XMLTranslator {

  private String startTime = null;

  public void printAllHash(ResourceSpec spec){
    spec.getAttributes().forEach((key, value) -> System.out.println("Key: " + key + ", Value: " + value));
  }

  public ValueSchema getSchema(ResourceSpec spec){

    switch (spec.getAttributes().get("DataType")){
      case "float":
        return ValueSchema.REAL;

      case "boolean":
        return ValueSchema.BOOLEAN;

      case "integer":
        return ValueSchema.INT;

      case "string", "time":
        return ValueSchema.STRING;

      case "duration":
        return ValueSchema.DURATION;

      default:
        return null; //CHANGE THIS SO IT CAN SUPPORT ARBITRARY DATATYPE
    }
  }

  public long durationDatatypeToMicros(String duration){
    int index = duration.indexOf("T");
    String parseDuration = duration;
    long numDays = -1;
    if (index != -1) {
      numDays = Long.parseLong(duration.substring(0, index)) * 24 * 60 * 60 * 1000 * 1000;
      parseDuration = duration.substring(index+1);
    }
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSS");
    LocalTime localTime = LocalTime.parse(parseDuration, formatter);
    long micros = localTime.getHour() * 3_600_000_000L
                  + localTime.getMinute() * 60_000_000L
                  + localTime.getSecond() * 1_000_000L
                  + localTime.getLong(ChronoField.MICRO_OF_SECOND);
    return (numDays > 0) ? (micros + numDays) : micros;
  }



  public SerializedValue getSerializedValue(ValueSchema valueSchema, String value){
    if (valueSchema == ValueSchema.REAL){
      return SerializedValue.of(Double.parseDouble(value));
    }
    else if (valueSchema == ValueSchema.BOOLEAN){
      return SerializedValue.of(Boolean.parseBoolean(value));
    }
    else if (valueSchema == ValueSchema.INT){
      return SerializedValue.of(Integer.parseInt(value));
    }
    else if (valueSchema == ValueSchema.STRING){
      return SerializedValue.of(value);
    }
    else if (valueSchema == ValueSchema.DURATION){
     long micros = durationDatatypeToMicros(value) * 1000;
     return SerializedValue.of(micros);
    }
    else
      return SerializedValue.NULL;
  }


  public long getTimeDifference(String startTime, String endTime){
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-DDD'T'HH:mm:ss");
    String[] startParts = startTime.split("\\.");
    String[] endParts = endTime.split("\\.");
    LocalDateTime startLDT = LocalDateTime.parse(startParts[0], formatter);
    LocalDateTime endLDT = LocalDateTime.parse(endParts[0], formatter);
    int nanos1 = startParts.length > 1 ? Integer.parseInt(startParts[1]) * 1000 : 0;
    int nanos2 = endParts.length > 1 ? Integer.parseInt(endParts[1]) * 1000 : 0;
    startLDT = startLDT.withNano(nanos1);
    endLDT = endLDT.withNano(nanos2);
    Instant startInstant = startLDT.toInstant(ZoneOffset.UTC);
    Instant endInstant = endLDT.toInstant(ZoneOffset.UTC);

    return ChronoUnit.MICROS.between(startInstant, endInstant);
  }

  public ResourceProfile<Optional<RealDynamics>> realProfileFromResourceSpec (ResourceSpec spec) {
    ValueSchema valueSchema = getSchema(spec);
    List<ProfileSegment<Optional<RealDynamics>>> segments = new ArrayList<>();
    return new ResourceProfile<>(valueSchema, segments);
  }



  public ResourceProfile<Optional<SerializedValue>> discreteProfileFromResourceSpec (ResourceSpec spec) {
    ValueSchema valueSchema = getSchema(spec);
    List<ProfileSegment<Optional<SerializedValue>>> segments = new ArrayList<>();
    return new ResourceProfile<>(valueSchema, segments);
  }

  public String getProfileKey(Map<String, String> attributes) {
    return (attributes.containsKey("Index")) ? attributes.get("Name") + attributes.get("Index") :  attributes.get("Name");
  }




  public void updateRealProfile(ResourceProfile<Optional<RealDynamics>> profile, Map<String,String> profileDetails, Map<String,Pair<String,String>> lastDataPoints, Boolean isFinalSpec) {
    String profileName = getProfileKey(profileDetails);
    if (lastDataPoints.containsKey(profileName)){
      Pair<String,String> profileOldPoint = lastDataPoints.get(profileName);
      String currentTimeStamp = profileDetails.get("TimeStamp");
      String oldTimeStamp = profileOldPoint.getFirst();
      long diff = getTimeDifference(oldTimeStamp, currentTimeStamp);

      Duration duration = Duration.microseconds(diff);
      double oldVal = Double.parseDouble(profileOldPoint.getSecond());
      double currentVal = Double.parseDouble(profileDetails.get("Value"));
      double diffSeconds =  (double) diff / 1000000.0;
      double rate = (diffSeconds != 0.0 ) ? (currentVal-oldVal) / diffSeconds : 0.0;
      RealDynamics realDynamics = RealDynamics.linear(oldVal,rate);
      ProfileSegment<Optional<RealDynamics>> profileSegment = new ProfileSegment<>(duration, Optional.of(realDynamics));
      profile.segments().add(profileSegment);
      lastDataPoints.put(profileName, new Pair<>(currentTimeStamp, profileDetails.get("Value")));
      if(isFinalSpec){
        RealDynamics lastRealDynamics = RealDynamics.linear(currentVal,0);
        ProfileSegment<Optional<RealDynamics>> lastProfileSegment = new ProfileSegment<>(Duration.ZERO, Optional.of(lastRealDynamics));
        profile.segments().add(lastProfileSegment);
      }

    }
    else{
      lastDataPoints.put(profileName, new Pair<>(profileDetails.get("TimeStamp"), profileDetails.get("Value")));
    }
  }

  public void updateDiscreteProfile(ResourceProfile<Optional<SerializedValue>> profile, Map<String,String> profileDetails, Map<String, Pair<String,String>> lastDataPoints, Boolean isFinalSpec) {
    String profileName = getProfileKey(profileDetails);
    //The structure of the if-else statements can be changed here, probably get it looked at
    if (lastDataPoints.containsKey(profileName)){
      Pair<String,String> profileOldPoint = lastDataPoints.get(profileName);
      String currentTimeStamp = profileDetails.get("TimeStamp");
      String oldTimeStamp = profileOldPoint.getFirst();
      long diff = getTimeDifference(oldTimeStamp, currentTimeStamp);

      Duration duration = Duration.microseconds(diff);
      SerializedValue serializedValue = getSerializedValue(profile.schema(), profileOldPoint.getSecond());

      //Ask someone to look at this line below
      ProfileSegment<Optional<SerializedValue>> profileSegment = new ProfileSegment<>(duration,
                                                                                      Optional.ofNullable(
                                                                                          serializedValue));
      profile.segments().add(profileSegment);
      lastDataPoints.put(profileName, new Pair<>(currentTimeStamp, profileDetails.get("Value")));
      if (isFinalSpec){
        SerializedValue lastSerializedValue = getSerializedValue(profile.schema(), profileDetails.get("Value"));
        ProfileSegment<Optional<SerializedValue>> lastProfileSegment = new ProfileSegment<>(Duration.ZERO,
                                                                                        Optional.ofNullable(
                                                                                            lastSerializedValue));
        profile.segments().add(lastProfileSegment);
      }
    }
    else{
      lastDataPoints.put(profileName, new Pair<>(profileDetails.get("TimeStamp"), profileDetails.get("Value")));
    }
  }
  /*public void updateProfileSet(ProfileSet profileSet, Map<String,String> profileDetails, Map<String,Pair<String,String>> existingTimeStamps) {
      String profileName = profileDetails.get("Name");
      if (profileSet.realProfiles().containsKey(profileName)) {
        updateRealProfile(profileSet.realProfiles().get(profileName),profileDetails,existingTimeStamps);
      } else if (profileSet.discreteProfiles().containsKey(profileName)) {
        updateDiscreteProfile(profileSet.discreteProfiles().get(profileName),profileDetails,existingTimeStamps);
      }
      else {
        throw new RuntimeException("Profile " + profileName + " not found");
      }

  }*/


  public ProfileSet readProfileSetFromXML (String filePath) throws Exception {

    XMLInputFactory factory = XMLInputFactory.newInstance();
    FileInputStream fis = new FileInputStream(filePath);
    XMLStreamReader reader = factory.createXMLStreamReader(fis);
    Map<String, ResourceProfile<Optional<RealDynamics>>> realProfiles = new HashMap<>();
    Map<String, ResourceProfile<Optional<SerializedValue>>> discreteProfiles = new HashMap<>();

    ProfileSet profileSet = new ProfileSet(realProfiles,discreteProfiles );
    boolean updateResource  = false;
    boolean finalUpdateResource = false;
    boolean encounteredStart = false;
    ResourceSpec resourceSpec = null;
    String currentElement = null;

    Map<String, String> updatingProfileSpecs = null;
    Map<String, Pair<String,String>> profileLastDataPoint = new HashMap<>();
    while (reader.hasNext()) {
      int event = reader.next();
      switch (event) {

        case XMLStreamReader.START_ELEMENT:
          currentElement = reader.getLocalName();

          if (currentElement.equals("ResourceSpec")) {
            //  System.out.println("Encountered ResourceSpec");
              resourceSpec = new ResourceSpec(new HashMap<String,String>());
          }

          if (currentElement.equals("TOLrecord") && reader.getAttributeValue(null, "type").equals("RES_VAL")) {
           // System.out.println("Encountered RES_VAL");
            updateResource = true;
            updatingProfileSpecs = new HashMap<>();
          }

          if (currentElement.equals("TOLrecord") && reader.getAttributeValue(null, "type").equals("RES_FINAL_VAL")) {
            finalUpdateResource = true;
            updatingProfileSpecs = new HashMap<>();
          }


          break;

        case XMLStreamReader.CHARACTERS:
          String characters = reader.getText().trim();
          //System.out.println(characters);

          //System.out.println("characters: " + characters);
          if (!encounteredStart && currentElement != null && currentElement.equals("TimeStamp") ) {
            startTime = characters;
            encounteredStart = true;
          }
          if (resourceSpec != null && !characters.isEmpty()) {
            switch (currentElement) { //See if currentElement needs to be replaced by reader.getLocalName()
              case "Name":
                resourceSpec.getAttributes().put("Name", characters);
                break;
              case "DataType":
                resourceSpec.getAttributes().put("DataType", characters);
                break;

              case "Interpolation":
                //System.out.println("Characters: " + characters);
                resourceSpec.getAttributes().put("Interpolation", characters);
                break;

              case "Index":

                //String val = reader.getAttributeValue(null, "level") + characters;
                if (resourceSpec.getAttributes().containsKey("Index")) {
                 /* String oldVal = resourceSpec.getAttributes().get("Index");
                  resourceSpec.getAttributes().put("Index", oldVal + val);*/
                  resourceSpec.getAttributes().compute("Index", (k, oldVal) -> oldVal + characters);
                }
                else {
                  resourceSpec.getAttributes().put("Index", characters);
                }
                break;
              default: break;
            }
          }
          else if ((updateResource || finalUpdateResource) && !characters.isEmpty()) {
            if (currentElement.equals("TimeStamp")) {
              updatingProfileSpecs.put("TimeStamp", characters);
            }
            else if (currentElement.equals("Name")) {
              updatingProfileSpecs.put("Name", characters);
            }
            else if (currentElement.equals("Index")) {
              if (updatingProfileSpecs.containsKey("Index")) {
                updatingProfileSpecs.compute("Index", (k, oldVal) -> oldVal + characters);
              }
              else {
                updatingProfileSpecs.put("Index", characters);
              }
            }
            else if (currentElement.contains("Value")) {
              updatingProfileSpecs.put("Value", characters);
            }
          }


          break;

          case XMLStreamReader.END_ELEMENT:
            boolean conditional = false;
            if("Interpolation".equals(reader.getLocalName()) && resourceSpec != null) {
              conditional = !(resourceSpec.getAttributes().containsKey("Interpolation"));
              if (conditional) {
                resourceSpec.getAttributes().put("Interpolation", "constant");
              }
            }
            if (reader.getLocalName().contains("Value") && updatingProfileSpecs != null) {
              conditional = !(updatingProfileSpecs.containsKey("Value"));
              if (conditional) {
                updatingProfileSpecs.put("Value", "");
              }
            }
            if("ResourceSpec".equals(reader.getLocalName()) && resourceSpec != null) {
              switch(resourceSpec.getAttributes().get("Interpolation")){
                case "constant":

                  String keyI = getProfileKey(resourceSpec.getAttributes());
                //  System.out.println("KeyI: " + keyI);
                  discreteProfiles.put(keyI,discreteProfileFromResourceSpec(resourceSpec));
                  break;

                case "linear":
                  String keyL = getProfileKey(resourceSpec.getAttributes());
                  realProfiles.put(keyL,realProfileFromResourceSpec(resourceSpec));
                  break;

                default:
                 // String keyD = resourceSpec.getAttributes().get("Name") + resourceSpec.getAttributes().get("Index");
                  String keyD = getProfileKey(resourceSpec.getAttributes());
                  discreteProfiles.put(keyD,discreteProfileFromResourceSpec(resourceSpec));
                  break;
              }
              resourceSpec = null;
            }
            if("TOLrecord".equals(reader.getLocalName()) && updatingProfileSpecs != null ) {

              String keyProfile = getProfileKey(updatingProfileSpecs);
              if(profileSet.realProfiles().containsKey(keyProfile)){
                ResourceProfile<Optional<RealDynamics>> realProfile = profileSet.realProfiles().get(keyProfile);
                updateRealProfile(realProfile, updatingProfileSpecs, profileLastDataPoint, finalUpdateResource);
              } else if (profileSet.discreteProfiles().containsKey(keyProfile)) {
                ResourceProfile<Optional<SerializedValue>> discreteProfile = profileSet.discreteProfiles().get(keyProfile);
                updateDiscreteProfile(discreteProfile, updatingProfileSpecs, profileLastDataPoint, finalUpdateResource);
              }
              else {
                throw new RuntimeException("Profile " + keyProfile + " not found");
              }
              updatingProfileSpecs = null;
              updateResource = false;
              finalUpdateResource = false;
            }

            currentElement = "";
            break;
      }
    }

    reader.close();

    return profileSet;
  }

  public String profileSetToJson(ProfileSet profileSet, long planId) throws JsonProcessingException {
    System.out.println("reached profileSetToJson");
    SimpleModule module = new SimpleModule();
    module.addSerializer(ProfileSet.class, new ProfileSetSerializer());
    module.addSerializer(ExternalDatasetHeader.class, new ExternalDatasetHeaderSerializer());
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(module);

    ExternalDatasetHeader externalDatasetHeader = new ExternalDatasetHeader(planId, startTime);
    String jsonHeader = objectMapper.writeValueAsString(externalDatasetHeader);
    String jsonTail = objectMapper.writeValueAsString(profileSet);
    ObjectNode headNode = (ObjectNode) objectMapper.readTree(jsonHeader);
    ObjectNode tailNode = (ObjectNode) objectMapper.readTree(jsonTail);
    ObjectNode tailWrapper = objectMapper.createObjectNode();
    tailWrapper.set("profileSet", tailNode);
    headNode.setAll(tailWrapper);

    return objectMapper.writeValueAsString(headNode);
  }

 /* public String graphqlQuery(ObjectNode json, String endPoint) throws IOException, InterruptedException {

    String query = """
        mutation AddExternalDataset($planId: Int!, $simulationDatasetId: Int, $datasetStart: String!, $profileSet: ProfileSet!)\s
        {
          addExternalDataset( planId: $planId, simulationDatasetId: $simulationDatasetId, datasetStart: $datasetStart, profileSet: $profileSet)\s
          {
            datasetId
          }
       }
       \s""";
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode queryNode = objectMapper.createObjectNode();
    queryNode.put("query", query);
    ObjectNode variablesNode = objectMapper.createObjectNode();
    variablesNode.set("variables", json);
    queryNode.setAll(variablesNode);
    String jsonString = objectMapper.writeValueAsString(queryNode);


    HttpRequest request = HttpRequest.newBuilder()
                                     .uri(URI.create(endPoint))
                                     .header("Content-Type", "application/json")
                                     .POST(HttpRequest.BodyPublishers.ofString(jsonString))
                                     .build();

    HttpClient client = HttpClient.newHttpClient();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body();
  }*/



  public void createExternalDatasetJson (String content, String filePath) {
    System.out.println("reached createExternalDatasetJson");
    try (FileWriter fileWriter = new FileWriter(filePath)) {
      fileWriter.write(content);
      System.out.println("JSON successfully written to " + filePath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void uploadExternalDatasetJson (String token, String role, String filePath, long planId) {

    File jsonFile = new File(filePath);
    FileBody fileBody = new FileBody(jsonFile, ContentType.APPLICATION_JSON, jsonFile.getName());

    org.apache.http.HttpEntity requestBody = MultipartEntityBuilder.create().addPart("external_dataset",fileBody).addTextBody("plan_id",
                                                                                                                              String.valueOf(
                                                                                                                                  planId)).build();
    HttpPost request = new HttpPost("http://localhost:9000/uploadDataset");
    request.setEntity(requestBody);
    request.setHeader("Authorization", "Bearer " + token);
    request.setHeader("x-hasura-role", role);

    CloseableHttpClient client = HttpClients.createDefault();
    try{
      CloseableHttpResponse response = client.execute(request);
      HttpEntity entity = response.getEntity();
      String responseString = EntityUtils.toString(entity);
      System.out.println(responseString);
      System.out.println(response.getStatusLine());
    }catch (Exception e){
      e.printStackTrace();
    }
  }

  public void loopPost(ProfileSet profileSet, long planId, String filePath, String role, String token) throws JsonProcessingException {
    Map<String, ResourceProfile<Optional<RealDynamics>>> holderReals = new HashMap<>();
    Map<String, ResourceProfile<Optional<SerializedValue>>> holderDiscrete = new HashMap<>();
    ProfileSet holderProfileSet = new ProfileSet(holderReals, holderDiscrete);
    int maxSize = 5;

    while (!profileSet.realProfiles().isEmpty()) {
      Iterator<Map.Entry<String,ResourceProfile<Optional<RealDynamics>>>> it = profileSet.realProfiles().entrySet().iterator();
      int count = 0;
      while (it.hasNext() && count < maxSize)
      {
        Map.Entry<String, ResourceProfile<Optional<RealDynamics>>> entry = it.next();
        holderReals.put(entry.getKey(), entry.getValue());
        count++;
        it.remove();
      }
      String json = profileSetToJson(holderProfileSet,planId);
      createExternalDatasetJson(json,filePath);
      uploadExternalDatasetJson(token,role,filePath,planId);
      holderReals.clear();
    }

    while(!profileSet.discreteProfiles().isEmpty()) {
      Iterator<Map.Entry<String,ResourceProfile<Optional<SerializedValue>>>> it = profileSet.discreteProfiles().entrySet().iterator();
      int count = 0;
      while (it.hasNext() && count < maxSize) {
        Map.Entry<String,ResourceProfile<Optional<SerializedValue>>> entry = it.next();
        holderDiscrete.put(entry.getKey(), entry.getValue());
        count++;
        it.remove();
      }
      String json = profileSetToJson(holderProfileSet,planId);
      createExternalDatasetJson(json,filePath);
      uploadExternalDatasetJson(token,role,filePath,planId);
      holderDiscrete.clear();
    }

  }



}
