package ina.vehicle.navigation.types;

public enum ENavigatorStatus {

   WAITING(0, "Waiting for route to start"),
   ROUTING(1, "Routing"),
   STOPPED(2, "Routing Stopped/Paused"),
   REACHED_DESTINATION(3, "Reached destination");

   private final int _code;
   private final String _name;

   ENavigatorStatus(final int code, final String name) {
      this._code = code;
      this._name = name;
   }

   public int getCode() {
	   return this._code;
   }

   public String getName() {
      return this._name;
   }

   public static final ENavigatorStatus get(String name) {
      for (final ENavigatorStatus t : ENavigatorStatus.values()) {
         if (t.getName().equalsIgnoreCase(name) ||
        	 t.name().equalsIgnoreCase(name)) {
            return t;
         }
      }
      return null;
   }

      
}
