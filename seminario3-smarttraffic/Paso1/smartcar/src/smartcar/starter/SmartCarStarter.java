package smartcar.starter;

import smartcar.impl.SmartCar;


public class SmartCarStarter {

	public static void main(String[] args) {

		SmartCar sc1 = new SmartCar("SmartCar001");
		
		try {
			Thread.sleep(5000);  
		} catch (InterruptedException e) {
		}

		sc1.changeRoad("R5s1", 100);  // indicamos que el SmartCar está en tal segmento
		sc1.notifyIncident("accidente");  // notificamos un accidente

	}

}
