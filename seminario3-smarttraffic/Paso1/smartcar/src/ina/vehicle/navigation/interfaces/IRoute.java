package ina.vehicle.navigation.interfaces;


import java.util.List;

public interface IRoute extends List<IRouteFragment> {

   public IRoute addRouteFragment(String rs, int pos_start, int pos_end);
   public String getRouteID();

   public IRouteFragment getFirst() throws IndexOutOfBoundsException;
   public IRouteFragment extractFirst() throws IndexOutOfBoundsException;
   public IRouteFragment getRouteFragment(int index) throws IndexOutOfBoundsException;
   
   public int getLenght();
   public int getRemainingDistance(IRoadPoint posActual);
   
   
}
