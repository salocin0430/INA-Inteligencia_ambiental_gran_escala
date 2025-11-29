package ina.vehicle.navigation.interfaces;

import ina.vehicle.navigation.types.ERoadStatus;

public interface IRoadSegment extends IIdentifiable {

   public IRoadSegmentConfigurator getRoadSegmentConfigurator();
   
   // ResourceType => road-segment
   public String getRT();

   // Config params
   public int getRoadSegmentMaxSpeed();
   public int getCurrentMaxSpeed();

   public String getRoad();
   public String getRoadSegmentCode();
   public int getLength();
   public int getStartKP();   // Starting Km. Point
   public int getEndKP();		// Ending Km. Poing

   public int getCapacity();
   public int getTrafficDensityPctg();

   // Iface attrs
   public ERoadStatus getStatus();

   public void closeRoadSegment();
   public void openRoadSegment();
   
   public Integer getNumVehicles();
   
   public IRoadSegment setNumVehicles(int n);
   
      
}
