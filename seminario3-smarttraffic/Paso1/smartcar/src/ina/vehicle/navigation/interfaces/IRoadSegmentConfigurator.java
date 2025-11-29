package ina.vehicle.navigation.interfaces;

public interface IRoadSegmentConfigurator {

   public IRoadSegmentConfigurator setRoadSegmentMaxSpeed(int maxSpeed);
   public IRoadSegmentConfigurator setCurrentMaxSpeed(int maxSpeed);
   // public IRoadSegmentConfigurator setStatus(ERoadStatus status);
   
   public IRoadSegmentConfigurator setLength(int length);
   // public IRoadSegmentConfigurator getLength();

   public IRoadSegmentConfigurator setCapacity(int capacity);
   // public IRoadSegmentConfigurator getCapacity();


}
