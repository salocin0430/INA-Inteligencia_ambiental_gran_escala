package ina.vehicle.navigation.utils;

import java.util.HashMap;
import java.util.Map;

import ina.vehicle.navigation.interfaces.IIdentifiable;

public class MyBean {

   protected Map<String, Object> props = null;

   public MyBean(String id) {
      this.props = new HashMap<String, Object>();
      this.setId(id);
   }

   public MyBean setId(String id) {
	   this.getProperties().put(IIdentifiable.ID, id);
	   return this;
   }
   public String getId() {
		return (String)this.getProperties().get(IIdentifiable.ID);
   }

   public Map<String, Object> getProperties() {
      return this.props;
   }

   public Object getProperty(String p) {
      return this.getProperties().get(p);
   }
   
	public <C> C getProperty(String p, Class<C> clase) {
	   Object o = this.getProperty(p);
	   return ( o == null ? null : (C)o);
	}
   
	public MyBean setProperty(String p, Object o) {
	   this.getProperties().put(p, o);
	   return this;
	}
   
	public String toString() {
		return this.getProperties().toString();
	}
   
}
