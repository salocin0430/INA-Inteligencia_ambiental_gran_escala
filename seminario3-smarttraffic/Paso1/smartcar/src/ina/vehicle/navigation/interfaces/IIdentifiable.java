package ina.vehicle.navigation.interfaces;

/**
*
*	Joan Fons	jjfons@pros.upv.es
*	PROS Research Center
*	UPV, Valencia, Spain
*	2016
*
**/

import java.util.UUID;

public interface IIdentifiable { 
	
	public static final String ID = "id";
	
	public String getId();
	
	public static String getFreshId() { 
		return UUID.randomUUID().toString().replace("-", ""); 
	}

}
