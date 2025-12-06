package ina.vehicle.navigation.components;

import ina.vehicle.navigation.interfaces.IRoadPoint;

public class RoadPoint implements IRoadPoint {

   protected String roadSegment = null;
   protected int position = 0;

   public RoadPoint(String roadSegment, int position) {
      this.roadSegment = roadSegment;
      this.position = position;
   }

   @Override
   public String getRoadSegment() {
      return this.roadSegment;
   }

   @Override
   public void setRoadSegment(String roadSegment) {
      this.roadSegment = roadSegment;
   }

   @Override
   public int getPosition() {
      return this.position;
   }

   @Override
   public void setPosition(int position) {
      this.position = position;
   }
   
   @Override
	public boolean equals(IRoadPoint p2) {
	   if ( p2 == null ) return false;
	   return ( this.getRoadSegment().equals(p2.getRoadSegment()) && this.getPosition()==p2.getPosition());
	}

   @Override
   public String toString() {
  	 return "(" + this.roadSegment + "," + this.position + ")"; 
   }

}