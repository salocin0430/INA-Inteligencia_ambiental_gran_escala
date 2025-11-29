package ina.vehicle.navigation.types;

public enum ERoadStatus {
	
// jjfons : 20160904
// Extret de la Memòria Anual d'Aforaments de Carreteres, campanya 2015 (pàg 30)
// Cegesev, Centre de Gestió i Seguretat Viària
// Basat en HCM2010, simplificat

   Free_Flow			(0, "Flujo Libre"), 
   Mostly_Free_Flow		(1, "Flujo razonablemente libre"),
   Restricted_Manouvers	(2, "Flujo con maniobras restringidas"), 
   Limited_Manouvers	(3, "Flujo con maniobras limitadas y reducción de velocidad"), 
   No_Manouvers			(4, "Flujo con maniobras impedidas. Se alcanza la capacidad"),
   Collapsed			(5, "Flujo inestable. Congestión"),
   Closed				(6, "Closed");				// Afegida
   

   private final int _code;
   private final String _name;

   ERoadStatus(final int code, final String name) {
      this._code = code;
      this._name = name;
   }

   public int getCode() {
	   return this._code;
   }
   
   public String getName() {
      return this._name;
   }

   public static ERoadStatus get(String name) {
      for (final ERoadStatus t : ERoadStatus.values()) {
         if (t.getName().equalsIgnoreCase(name) ||
        	 t.name().equalsIgnoreCase(name)) {
            return t;
         }
      }
      return null;
   }
   
      
}
