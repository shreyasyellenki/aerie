package xmltol.profiles;
//import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nasa.jpl.aerie.merlin.server.models.ProfileSet;
import org.junit.jupiter.api.Test;
import profiles.XMLTranslator;

import java.io.File;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TranslatorTest {

  @Test
  public void testReadProfileSetFromXML() throws Exception {
    XMLTranslator xmlTranslator = new XMLTranslator();
    File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("HarderTest.xml")).toURI());
    ProfileSet profileSet = xmlTranslator.readProfileSetFromXML(file.getPath());
    assertNotNull(profileSet);
    //System.out.println(profileSet);
   // String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwczovL2hhc3VyYS5pby9qd3QvY2xhaW1zIjp7IngtaGFzdXJhLWFsbG93ZWQtcm9sZXMiOlsiYWVyaWVfYWRtaW4iLCJ1c2VyIiwidmlld2VyIl0sIngtaGFzdXJhLWRlZmF1bHQtcm9sZSI6ImFlcmllX2FkbWluIiwieC1oYXN1cmEtdXNlci1pZCI6InN0cmluZyJ9LCJ1c2VybmFtZSI6InN0cmluZyIsImlhdCI6MTc0ODM3NzY4MiwiZXhwIjoxNzQ4NTA3MjgyfQ.sjhclGqrAlkKiMhV1988iFxeggsYv7cCM_ugK_0P2EU";
    String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwczovL2hhc3VyYS5pby9qd3QvY2xhaW1zIjp7IngtaGFzdXJhLWFsbG93ZWQtcm9sZXMiOlsiYWVyaWVfYWRtaW4iLCJ1c2VyIiwidmlld2VyIl0sIngtaGFzdXJhLWRlZmF1bHQtcm9sZSI6ImFlcmllX2FkbWluIiwieC1oYXN1cmEtdXNlci1pZCI6InN0cmluZyJ9LCJ1c2VybmFtZSI6InN0cmluZyIsImlhdCI6MTc0ODQ2NDU5NSwiZXhwIjoxNzQ4NTk0MTk1fQ.y1kTpjEWT2W1MRNSu1ykpBTqWH7WsYvbcK_O8Lebqks";
    String role = "aerie_admin";
    //String json = xmlTranslator.profileSetToJson(profileSet,21);
    //xmlTranslator.createExternalDatasetJson(json, "output.json");
    xmlTranslator.loopPost(profileSet,23,"output.json",role,token);
    //xmlTranslator.uploadExternalDatasetJson(token, role,"output.json", 21);
  }
}
