package ina.vehicle.navigation.components;

import ina.vehicle.navigation.interfaces.INavigator;
import ina.vehicle.navigation.interfaces.IRoadPoint;
import ina.vehicle.navigation.interfaces.IRoadSegment;
import ina.vehicle.navigation.interfaces.IRoute;
import ina.vehicle.navigation.interfaces.IRouteFragment;
import ina.vehicle.navigation.types.ENavigatorStatus;
import ina.vehicle.navigation.utils.MyBean;
import ina.vehicle.navigation.utils.MySimpleLogger;

public class Navigator implements INavigator {

   protected MyBean bean = null;
   
   public Navigator(String id) {
      this.bean = new MyBean(id);
      this.setNavigatorStatus(ENavigatorStatus.WAITING);
   }

   @Override
   public String getId() {
      return (String) this.bean.getProperty("id");
   }

   protected INavigator setId(String id) {
      this.bean.getProperties().put("id", id);
      return this;
   }
   
   protected INavigator setOffRoadMode(boolean mode) {
	   // Se usa para indicar que no se sigue o no se encuentra información de un segmento de carretera
	   MySimpleLogger.debug(this.getId(), "Setting Off-Road Mode");
	   this.bean.setProperty("offroad", mode);
	   return this;
   }
   
   @Override
   public boolean getOffRoadMode() {
	   Boolean offroad = this.bean.getProperty("offroad", Boolean.class);
	   if ( offroad == null )
		   return false;
	   return offroad;
   }
   
   
   @Override
   public IRoute getRoute() {
	  return this.bean.getProperty("route", IRoute.class);
   }

   @Override
   public INavigator setRoute(IRoute route) {
	  this.bean.setProperty("route", route);
	  if ( route != null && !route.isEmpty() ) {
		  MySimpleLogger.trace(this.getId(), "Setting route: " + route);
		  IRoadPoint rp = route.get(route.size()-1).getEndPoint();
		  this.setLastDestinationPoint(rp);
		  //nos ubicamos en el punto de inicio de la ruta
		  rp = route.get(0).getStartPoint();
		  if ( this.getCurrentPosition() != null && !this.getCurrentPosition().equals(rp) ) {
			  MySimpleLogger.debug(this.getId(), "Teleporting from " + this.getCurrentPosition() + " to " + rp);
		  }
		  this.setCurrentPosition(rp);
	  }
      return this;
   }
   

/*   
   protected IRoadSegmentRepresentation getRoadSegment(String roadsegment) throws Exception {
	   try {
		return RemoteSearchTools.getRoadSegmentStatus(roadsegment);
	} catch (Exception e) {
		logger.error(e);
		logger.warn("Cannot get Road Segment info for " + roadsegment);
		throw e;
	}
   }
*/
   
   @Override
   public IRoadPoint getCurrentPosition() {
      return this.bean.getProperty("position", IRoadPoint.class);
   }

   @Override
   public INavigator setCurrentPosition(IRoadPoint position) {
	   MySimpleLogger.trace(this.getId(), "Set current position to: " + position);
      this.bean.getProperties().put("position", position);
      return this;
   
   }
   
   protected INavigator setLastDestinationPoint(IRoadPoint dest) {
	   this.bean.setProperty("destination", dest);
	   return this;
   }
   
   @Override
	public IRoadPoint getDestinationPoint() {
	   return this.bean.getProperty("destination", IRoadPoint.class);
	}


   @Override
	public ENavigatorStatus getNavigatorStatus() {
		return this.bean.getProperty("status", ENavigatorStatus.class);
	}
   

   protected INavigator setNavigatorStatus(ENavigatorStatus status) {
	  this.bean.getProperties().put("status", status);
	  return this;
   }

   
   @Override
   public boolean isRouting() {
	  return ( this.getNavigatorStatus() == ENavigatorStatus.ROUTING );
   }
   
   @Override
   public INavigator startRouting() {
	   
	  ENavigatorStatus st = this.getNavigatorStatus();
	  IRoute r = this.getRoute();
	  switch (st) {
		case WAITING:
		  if ( r == null || r.isEmpty() ) {
				MySimpleLogger.error(this.getId(), "Route not defined. Cannot start navigation ...");
				return this;
		  }
		case STOPPED:
		  this.setNavigatorStatus(ENavigatorStatus.ROUTING);
		case ROUTING:
			MySimpleLogger.trace(this.getId(), "Navigation started ...");
			break;
		case REACHED_DESTINATION:
			if ( r != null && !r.isEmpty() ) {
				this.setNavigatorStatus(ENavigatorStatus.ROUTING);
			} else {
				MySimpleLogger.error(this.getId(), "Navigator cannot start an ended Route");
			}
		break;

	default:
		break;
	}
	
    return this;
   }

   @Override
   public INavigator stopRouting() {
	  if ( this.getNavigatorStatus() != ENavigatorStatus.REACHED_DESTINATION )
		  this.setNavigatorStatus(ENavigatorStatus.STOPPED);
	  MySimpleLogger.trace(this.getId(), "Navigation stopped ...");
      return this;
   }
   
   protected INavigator reachedDestination(IRoadPoint destination) {
	   MySimpleLogger.info(this.getId(), "Reached Destination");
	   this.setNavigatorStatus(ENavigatorStatus.REACHED_DESTINATION);
	   this.setLastDestinationPoint(destination);
	   this.setCurrentPosition(destination);
//	   this.setOffRoadMode(false);
	   return this;
   }

   @Override
	public IRouteFragment getCurrentRouteStep() {
	   if ( this.getRoute() == null || this.getRoute().isEmpty() )
		   return null;
	   
	   return this.getRoute().getFirst();
	}
   
   protected INavigator setCurrentRoadSegment(IRoadSegment r) {
	   this.bean.setProperty("current-road-segment", r);
	   return this;
   }
   
   @Override
	public IRoadSegment getCurrentRoadSegment() {
	   return this.bean.getProperty("current-road-segment", IRoadSegment.class);
	}
   


   @Override
   public INavigator move(long milliseconds, int vehicle_current_speed) {
	   	   
	  switch( this.getNavigatorStatus() ) {
	  
	  case WAITING:
	  case STOPPED:
		  break;
		  
	  case REACHED_DESTINATION:
		  break;
		  
	  case ROUTING:
		  
		  int travelled_distance = (int)(milliseconds * vehicle_current_speed * 0.0002778 ); // 1 seg -> m = speed*1000/3600
		  MySimpleLogger.debug(this.getId(), "Starting Point: " + String.format("%12s", this.getCurrentPosition()) + "\t Travelled Distance: " + travelled_distance + " m\t Speed " +  vehicle_current_speed + " Km/h");
		  this._move(travelled_distance, this.getCurrentPosition());
		  break;
	  }
	   
      return this;
   }
      
   
   protected INavigator _move(int distance, IRoadPoint posicionActual) {
	
	   // Esta función actualiza la ruta del navegador calculando el movimiento realizado desde la posición actual (IRoadPoint) una distancia indicada
	   
	   IRouteFragment rf = null;
	   try {
		   rf = this.getCurrentRouteStep();
		} catch (IndexOutOfBoundsException e) {
			this.reachedDestination(posicionActual);
			return this;
		}
 
	  if ( posicionActual == null ) {
		  // Si la posición actual es NULL es porque acabo de iniciar la ruta
		  // Establezco la posición actual al primer paso de la ruta
		  posicionActual = rf.getStartPoint();
	  }
	  
	  if ( distance <= 0 ) {
		  // Si no hay distancia que recorrer, hemos terminado
		  return this;
	  }
	  
	  boolean movemos_direccion_ascendente_PKs = ( rf.getEndPoint().getPosition() >= posicionActual.getPosition() ? true : false);

	  // calculamos hasta donde deberíamos avanzar ...
	  boolean salimosDeRoadSegment = false;

	  int nextPos;
	  if ( movemos_direccion_ascendente_PKs ) {
		  nextPos = posicionActual.getPosition() + distance;
		  if (  nextPos > rf.getEndPoint().getPosition() ) {
			  // Al movernos deberíamos salirnos del fragmento actual
			  salimosDeRoadSegment = true;
			  nextPos = rf.getEndPoint().getPosition();
		  }
	  } else {
		  nextPos = posicionActual.getPosition() - distance;
		  if (  nextPos < rf.getEndPoint().getPosition() ) {
			  // Al movernos deberíamos salirnos del fragmento actual
			  salimosDeRoadSegment = true;
			  nextPos = rf.getEndPoint().getPosition();
		  }
	  }

	  
	  // ... en nextpos tenemos la posición a la que llegamos
	  if ( !salimosDeRoadSegment ) {
		  
		  // Si no llegamos a salir del segmento, nos ubicamo en la posición calculada
		  
		  this.setCurrentPosition(new RoadPoint(posicionActual.getRoadSegment(), nextPos));
		  
	  } else {
		  
		  // Salimos del RoadSegment actual y entramos en el siguiente ...
		  
		  // Para ello, se realizarán dos pasos:
		  //	1-  nos moveremos justo al punto inicial del siguiente fragmento,
		  //	2-  relanzaremos recursivamente esta operación para movernos la distancia restante
		  
		  
		  // ... eliminamos el fragmento actual (que hemos completado) de la ruta ...
		  this.getRoute().extractFirst();
		  
		  if ( this.getRoute().isEmpty() ) {
			  
			  // hemos llegado al final de la ruta
			  this.reachedDestination(new RoadPoint(rf.getEndPoint().getRoadSegment(), rf.getEndPoint().getPosition()));
			  return this;
			  
		  } else {
			  int travelled = Math.abs(nextPos - posicionActual.getPosition());
			  int remaining_distance = distance-travelled;
			  if ( remaining_distance > 0 ) {
				  
				  // recursivamente nos movemos la distancia 'sobrante' ...
				  this._move(remaining_distance, this.getCurrentRouteStep().getStartPoint());
				  
			  } else {
				  this.reachedDestination(new RoadPoint(rf.getEndPoint().getRoadSegment(), rf.getEndPoint().getPosition()));
			  }
		  }
	  }
	  
	  return this;
	   
   }

   
}
