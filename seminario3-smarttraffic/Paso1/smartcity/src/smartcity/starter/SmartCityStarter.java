package smartcity.starter;

import smartcity.impl.SmartCity;


public class SmartCityStarter {

	public static void main(String[] args) {

		SmartCity sc1 = new SmartCity("VLC.net");
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

}
