package ina.vehicle.navigation.interfaces;

import ina.vehicle.navigation.types.ENavigatorStatus;

public interface INavigator extends IIdentifiable {
	
   public IRoadPoint getCurrentPosition();
   public INavigator setCurrentPosition(IRoadPoint point);
   public IRouteFragment getCurrentRouteStep();
   public IRoadSegment getCurrentRoadSegment();
   
   public IRoadPoint getDestinationPoint();
   
   public IRoute getRoute();
   public INavigator setRoute(IRoute route);
   
   public ENavigatorStatus getNavigatorStatus();
   public INavigator startRouting();
   public INavigator stopRouting();
   public boolean isRouting();
   
   public boolean getOffRoadMode();

   public INavigator move(long milliseconds, int vehicle_current_speed);
   


}
