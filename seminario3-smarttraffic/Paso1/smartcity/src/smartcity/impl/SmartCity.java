package smartcity.impl;

public class SmartCity {
	
	protected SmartCity_RoadIncidentsSubscriber subscriber  = null;
	protected String id = null;
	
	public SmartCity(String id) {
		this.setId(id);
		this.subscriber = new SmartCity_RoadIncidentsSubscriber(this);
		this.subscriber.connect();
		this.subscriber.subscribe("es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/+/alerts");
		

	}

	 public String getId() {
		return id;
	}
	 
	 public void setId(String id) {
		this.id = id;
	}
	 
	
}
