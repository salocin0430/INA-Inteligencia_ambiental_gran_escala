package ina.vehicle.navigation.interfaces;


public interface IRouteFragment {

   @Override
   public String toString();

   public IRoadPoint getStartPoint();
   public IRoadPoint getEndPoint();
   public void setStartPoint(IRoadPoint p);
   public void setEndPoint(IRoadPoint p);

}
