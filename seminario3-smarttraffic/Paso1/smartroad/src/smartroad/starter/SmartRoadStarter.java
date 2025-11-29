package smartroad.starter;

import smartroad.impl.SmartRoad;

public class SmartRoadStarter {

	public static void main(String[] args) {

		SmartRoad r5s1 = new SmartRoad("R5s1");
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

}
