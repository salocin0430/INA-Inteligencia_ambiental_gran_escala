package ina.vehicle.navigation.components;

import java.util.ArrayList;
import java.util.Iterator;

import ina.vehicle.navigation.interfaces.IRoadPoint;
import ina.vehicle.navigation.interfaces.IRoute;
import ina.vehicle.navigation.interfaces.IRouteFragment;

public class Route extends ArrayList<IRouteFragment> implements IRoute {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 8554884433683050245L;
	protected int length = 0;
	protected String routeID = null;
      
      


      /**
       * Route Components. [RoadPoint startPoint, endPoint] It symbolize a physical single road
       *
       * @param RoadPoint
       *           startPoint, RoadPoint endPoint
       **/
      public class RouteFragment implements IRouteFragment {
      
         protected IRoadPoint startPoint = null;
         protected IRoadPoint endPoint = null;
      
         public RouteFragment(IRoadPoint startPoint, IRoadPoint endPoint) {
            this.startPoint = startPoint;
            this.endPoint = endPoint;
         }
      
         @Override
         public IRoadPoint getStartPoint() {
            return this.startPoint;
         }
      
         @Override
         public void setStartPoint(IRoadPoint startPoint) {
            this.startPoint = startPoint;
         }
      
         @Override
         public IRoadPoint getEndPoint() {
            return this.endPoint;
         }
      
         @Override
         public void setEndPoint(IRoadPoint endPoint) {
            this.endPoint = endPoint;
         }
         
         
      
	     @Override
	     public String toString() {
	    	 return "[" + this.startPoint.toString() + "-" + this.endPoint.toString() + "]";
	     }
      
      }

      
      



      
      
      @Override
      public String getRouteID() {
    	 if (this.routeID != null)
    		 return this.routeID;
    	 else
    		 return "<no-route>";
    			
      }

      public void setRouteID(String routeID) {
         this.routeID = routeID;
      }

      @Override
	   public IRouteFragment getFirst() throws IndexOutOfBoundsException {
    	  	return this.get(0);
	   }
      

      @Override
	    public IRouteFragment extractFirst() throws IndexOutOfBoundsException {
        	return this.remove(0);
	    }
      
      @Override
	    public int getLenght() {
	    	return this.length;
	    }
      
      @Override
	   public int getRemainingDistance(IRoadPoint posActual) {
    	  	if ( this.size() == 0 || posActual == null )
    	  		return 0;
    	  	int distance = Math.abs(this.get(0).getEndPoint().getPosition() - posActual.getPosition());
    	  	for(int i=1;i<this.size();i++) {
    	  		distance += Math.abs(this.get(i).getEndPoint().getPosition()-this.get(i).getStartPoint().getPosition());
    	  	}
    	  	return distance;
	    }
      
      protected void setLength(int l) {
    	  this.length = l;
      }
      
      
      // jjfons : 16/07/2016
      
      @Override
      public String toString() {
    	  
    	  if ( this.isEmpty() )
    		  return "<none>";
    	  
    	  StringBuffer rs = new StringBuffer();
    	  Iterator<IRouteFragment> it_rf = this.iterator();
    	  while ( it_rf.hasNext() )
    		  rs.append(it_rf.next().toString());
    	  return rs.toString();
	  }



      @Override
      /**
       * It adds a single RouteFragment at the end of the route
       *
       * @param RoadSegment
       *           rs_start, int pos_start, RoadSegment rs_end, int pos_end
       */
      public IRoute addRouteFragment(String rs, int pos_start, int pos_end) {
         this.add(new RouteFragment(new RoadPoint(rs, pos_start), new RoadPoint(rs, pos_end)));
         this.setLength(this.getLenght()+ Math.abs(pos_end-pos_start));
         this.setRouteID("Route from " + this.get(0).getStartPoint().toString() + " to " + this.get(this.size()-1).getEndPoint().toString());
         return this;
      }
      
      @Override
      /**
       * Gets the [index] route fragment of the route
       *
       * @param int index
       */
      public IRouteFragment getRouteFragment(int index) throws IndexOutOfBoundsException {
         return this.get(index);
      }
 
   }
