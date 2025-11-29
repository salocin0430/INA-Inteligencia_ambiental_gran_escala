package ina.vehicle.navigation.components;

import ina.vehicle.navigation.interfaces.IRoadSegment;
import ina.vehicle.navigation.interfaces.IRoadSegmentConfigurator;
import ina.vehicle.navigation.types.ERoadStatus;
import ina.vehicle.navigation.utils.MyBean;

public class RoadSegment implements IRoadSegment, IRoadSegmentConfigurator {

   protected MyBean bean = null;

   public RoadSegment(String id, String road, String roadSegmentCode, int pk_inicio, int pk_fin, int capacity, int speed) {
     
	  this.bean = new MyBean(id);
      this.setRoad(road);
      this.setRoadSegmentCode(roadSegmentCode);
      this.setStartKP(pk_inicio);
      this.setEndKP(pk_fin);
      this.setLength(Math.abs((pk_inicio-pk_fin)));
      this.setCapacity(capacity);
      this.setRoadSegmentMaxSpeed(speed);
      this.setStatus(ERoadStatus.Free_Flow);
      this.setNumVehicles(0);
   }

   @Override
   public String getId() {
      return this.bean.getId();
   }
   
   
   @Override
	public String getRT() {
		return "road-segment";
	}
   
   @Override
	public String getRoadSegmentCode() {
		return this.bean.getProperty("code", String.class);
	}
   
   protected IRoadSegmentConfigurator setRoadSegmentCode(String code) {
	   this.bean.setProperty("code", code);
	   return this;
   }

   @Override
   public IRoadSegmentConfigurator getRoadSegmentConfigurator() {
      return this;
   }

   
   @Override
   public int getRoadSegmentMaxSpeed() {
	   return this.bean.getProperty("max_speed", Integer.class);
   }

   @Override
   public IRoadSegmentConfigurator setRoadSegmentMaxSpeed(int maxSpeed) {
      this.bean.getProperties().put("max_speed", maxSpeed);
      Integer cmaxSpeed = this.getCurrentMaxSpeed();
      if ( cmaxSpeed == null || cmaxSpeed > maxSpeed )
    	  this.setCurrentMaxSpeed(maxSpeed);
      return this;
   }


   @Override
   public int getCurrentMaxSpeed() {
	   Integer ms = this.bean.getProperty("current_max_speed", Integer.class);
	   if ( ms != null )
		   return ms;
	   
	   ms = this.getRoadSegmentMaxSpeed();
	   this.setCurrentMaxSpeed(ms);
	   return ms;
   }
   
   @Override
   public IRoadSegmentConfigurator setCurrentMaxSpeed(int maxSpeed) {
      this.bean.getProperties().put("current_max_speed", Math.min(maxSpeed, this.getRoadSegmentMaxSpeed()));
      return this;
   }

   
   @Override
   public ERoadStatus getStatus() {
      return this.bean.getProperty("status", ERoadStatus.class);
   }

   protected void setStatus(ERoadStatus status) {
      this.bean.getProperties().put("status", status);
   }
   
   @Override
	public void closeRoadSegment() {
	   if ( this.getStatus() == ERoadStatus.Closed )
		   return;
	   
	   this.setStatus(ERoadStatus.Closed);
	}
   
   @Override
	public void openRoadSegment() {
	   ERoadStatus cstatus = this.getStatus();
	   if ( cstatus == null || cstatus == ERoadStatus.Closed ) {
		   this._updateRoadTrafficDensityStatus();
	   }
	}


   @Override
   public int getCapacity() {
      return this.bean.getProperty("capacity", Integer.class);
   }

   @Override
   public IRoadSegmentConfigurator setCapacity(int c) {
      this.bean.getProperties().put("capacity", c);
      return this;
   }

   @Override
   public String getRoad() {
      return this.bean.getProperty("road", String.class);
   
   }

   
   protected IRoadSegmentConfigurator setRoad(String road) {
      this.bean.getProperties().put("road", road);
      return this;
   }
   
   @Override
   public int getStartKP() {
	   return this.bean.getProperty("start-kp", Integer.class);
   }

   @Override
   public int getEndKP() {
	   return this.bean.getProperty("end-kp", Integer.class);
   }
   
   protected IRoadSegmentConfigurator setStartKP(int p) {
	   this.bean.setProperty("start-kp", p);
	   return this;
   }

   protected IRoadSegmentConfigurator setEndKP(int p) {
	   this.bean.setProperty("end-kp", p);
	   return this;
   }

   @Override
   public int getLength() {
      return this.bean.getProperty("length", Integer.class);
   
   }

   @Override
   public IRoadSegmentConfigurator setLength(int l) {
      this.bean.getProperties().put("length", l);
      return this;
   }
   
   protected ERoadStatus _calculateStatusValueForTrafficDensity(int trafficDensity) {
	        
      if ( trafficDensity < 20 ) {
    	  return ERoadStatus.Free_Flow;
      } else if (trafficDensity >= 20 && trafficDensity < 35) {
      	  return ERoadStatus.Mostly_Free_Flow;
      } else if (trafficDensity >= 35 && trafficDensity < 50) {
      	  return ERoadStatus.Restricted_Manouvers;
      } else if (trafficDensity >= 50 && trafficDensity < 65) {
          return ERoadStatus.Limited_Manouvers; 
      } else if (trafficDensity >= 65 && trafficDensity < 85) {
          return ERoadStatus.No_Manouvers; 
      } else {
          return ERoadStatus.Collapsed; 
      }

   }
   
   @Override
   public IRoadSegment setNumVehicles(int n) {
	   if ( n < 0 )
		   n = 0;
	   this.bean.getProperties().put("num-vehicles", n);
	   
	   // Si no está cerrado el Road Segment, actualizamos su estado en función del tráfico
	   ERoadStatus currentStatus = this.getStatus();
	   if ( currentStatus != ERoadStatus.Closed ) {
		   if ( this.getCapacity() == 0) {
			   
		   }
		   this._updateRoadTrafficDensityStatus();
	   }
	   
	   
	   return this;
   }
   
   protected IRoadSegment setTrafficDensityPctg(int densityPctg) {
	   this.bean.setProperty("density", densityPctg);
	   return this;
   }
   
   
   protected IRoadSegment _updateRoadTrafficDensityStatus() {
	   int trafficDensity = 0;
	   if ( this.getCapacity() < 0 ) {
		   trafficDensity = 0;
	   } else if ( this.getCapacity() == 0 ) {
		   trafficDensity = 100;
	   } else {
		   trafficDensity = this.getNumVehicles() * 100 / this.getCapacity();
	   }
	   this.setTrafficDensityPctg(trafficDensity);
	   ERoadStatus status = this._calculateStatusValueForTrafficDensity(trafficDensity);
	   ERoadStatus currentStatus = this.getStatus();
	   if ( currentStatus != status ) {
		   this.setStatus(status);
	   }
	   return this;
   }
   
   @Override
	public int getTrafficDensityPctg() {
		return this.bean.getProperty("density", Integer.class);
	}
   
   @Override
   public Integer getNumVehicles() {
	   Integer nv = this.bean.getProperty("num-vehicles", Integer.class);
	   if ( nv == null ) {
		   this.setNumVehicles(0);
		   return 0;
	   }
	   return nv;
   }
      
   
   @Override
	public String toString() {
		return this.getId();
	}

}
