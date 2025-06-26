package xmltol.profiles;

public class ExternalDatasetHeader {
  private long planId;
  private int simulationDatasetId;
  private String startTime;

  public ExternalDatasetHeader(long planId, int simulationDatasetId, String startTime) {
    this.planId = planId;
    this.simulationDatasetId = simulationDatasetId;
    this.startTime = startTime;
  }

  public ExternalDatasetHeader(long planId,String startTime) {
    this(planId,-1,startTime);
  }
  public long getPlanId() {return planId;}
  public String getStartTime() {return startTime;}
  public int getSimulationDatasetId() {return simulationDatasetId;}
  public void setPlanId(int planId) {this.planId = planId;}

  public void setSimulationDatasetId(final int simulationDatasetId) {
    this.simulationDatasetId = simulationDatasetId;
  }

  public void setStartTime(String startTime) {this.startTime = startTime;}
}
