package smartroad.impl;

public class SmartRoad {
	
	protected SmartRoad_IncidentNotifier notifier = null;
	protected SmartRoad_RoadIncidentsSubscriber subscriber  = null;
	protected String id = null;
	
	public SmartRoad(String id) {
		this.setId(id);
		this.subscriber = new SmartRoad_RoadIncidentsSubscriber(this);
		this.subscriber.connect();
		this.subscriber.subscribe("es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/" + id + "/alerts");
		
		// Inicializar y conectar el notifier para retransmitir alertas al canal info
		this.notifier = new SmartRoad_IncidentNotifier(this);
		this.notifier.connect();
	}

	 public String getId() {
		return id;
	}
	 
	 public void setId(String id) {
		this.id = id;
	}
	 
	 /**
	  * Retransmite un mensaje recibido en /alerts al canal /info
	  * @param message El mensaje JSON a retransmitir
	  */
	 public void notify(String message) {
		 this.notifier.notify(message);
	 }
	
}
