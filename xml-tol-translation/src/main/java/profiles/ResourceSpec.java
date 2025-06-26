package xmltol.profiles;

import java.util.Map;

public class ResourceSpec {
  private Map<String, String> attributes;

  //Temporary holder for info until type of profile is specified
  public ResourceSpec(Map<String, String> attributes) {
    this.attributes = attributes;
  }
  public Map<String, String> getAttributes() {
    return attributes;
  }

}
