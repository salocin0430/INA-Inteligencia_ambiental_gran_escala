package ina.vehicle.navigation.interfaces;


public interface IRoadPoint {

   @Override
   public String toString();

   public String getRoadSegment();

   public int getPosition();

   public void setRoadSegment(String rs);

   public void setPosition(int position);
   
   public boolean equals(IRoadPoint p2);

}
