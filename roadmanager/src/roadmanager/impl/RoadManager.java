package roadmanager.impl;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestor de Carreteras (Road Manager)
 * 
 * Responsabilidades:
 * 1. Gestionar alertas: monitorizar canal de alertas y retransmitir a canal de información
 * 2. Auto-regulación de velocidad según densidad de tráfico (mediante señales speed-limit)
 * 3. Gestionar señalización de velocidad (crear/eliminar señales speed-limit según densidad)
 */
public class RoadManager {
	
	protected String managerId = null;
	
	// Componentes MQTT del simulador
	protected RoadManager_AlertsSubscriber alertsSubscriber = null; // Suscribe a alerts de todos los segmentos
	protected RoadManager_InfoPublisher infoPublisher = null; // Publica en info de todos los segmentos
	protected RoadManager_RoadStatusSubscriber roadStatusSubscriber = null; // Suscribe a info para ROAD_STATUS
	
	// Componentes AWS IoT
	protected RoadManager_AWSAlertsSubscriber awsAlertsSubscriber = null; // Suscribe a alerts en AWS IoT
	protected RoadManager_AWSInfoPublisher awsInfoPublisher = null; // Publica en info en AWS IoT
	protected RoadManager_AWSRoadStatusSubscriber awsRoadStatusSubscriber = null; // Suscribe a info en AWS IoT
	
	// Gestión de señales de velocidad por segmento
	// Mapa: road-segment -> SpeedLimitSignal activa (null si no hay señal)
	protected Map<String, SpeedLimitSignal> activeSpeedLimitSignals = new HashMap<>();
	
	// Mapa: road-segment -> velocidad máxima del segmento (para calcular límites dinámicos)
	protected Map<String, Integer> segmentMaxSpeeds = new HashMap<>();
	
	public RoadManager(String managerId) {
		this.managerId = managerId;
		
		// Inicializar componentes del simulador
		this.alertsSubscriber = new RoadManager_AlertsSubscriber(this);
		this.infoPublisher = new RoadManager_InfoPublisher(this);
		this.roadStatusSubscriber = new RoadManager_RoadStatusSubscriber(this);
		
		// Conectar componentes del simulador
		this.alertsSubscriber.connect();
		this.infoPublisher.connect();
		this.roadStatusSubscriber.connect();
		
		// Suscribirse a alertas de todos los segmentos (usando wildcard)
		this.alertsSubscriber.subscribe("es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/+/alerts");
		
		// Suscribirse a info de todos los segmentos para recibir ROAD_STATUS
		this.roadStatusSubscriber.subscribe("es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/+/info");
	}
	
	/**
	 * Habilita la integración con AWS IoT
	 */
	public void enableAWSIoT() {
		// Inicializar componentes AWS IoT
		// Los constructores AWS requieren un topic inicial
		this.awsAlertsSubscriber = new RoadManager_AWSAlertsSubscriber(this, "smartcities/traffic/PTPaterna/road/+/alerts");
		this.awsInfoPublisher = new RoadManager_AWSInfoPublisher(this);
		this.awsRoadStatusSubscriber = new RoadManager_AWSRoadStatusSubscriber(this, "smartcities/traffic/PTPaterna/road/+/info");
		
		// Conectar componentes AWS IoT (la suscripción se hace automáticamente en connect())
		this.awsAlertsSubscriber.connect();
		this.awsInfoPublisher.connect();
		this.awsRoadStatusSubscriber.connect();
	}
	
	/**
	 * Retransmite una alerta recibida en el canal alerts al canal info del mismo segmento
	 * @param roadSegment Segmento de carretera donde ocurrió la alerta
	 * @param alertMessage Mensaje JSON de la alerta
	 */
	public void retransmitAlert(String roadSegment, String alertMessage) {
		// Retransmitir en el simulador
		if (this.infoPublisher != null && this.infoPublisher.isConnected()) {
			this.infoPublisher.publishInfo(roadSegment, alertMessage);
		}
		
		// Retransmitir en AWS IoT
		if (this.awsInfoPublisher != null && this.awsInfoPublisher.isConnected()) {
			this.awsInfoPublisher.publishInfo(roadSegment, alertMessage);
		}
	}
	
	/**
	 * Procesa un mensaje ROAD_STATUS y aplica auto-regulación de velocidad según densidad
	 * @param roadSegment Segmento de carretera
	 * @param status Estado de densidad: Free_Flow, Mostly_Free_Flow, Limited_Manouvers, No_Manouvers, Collapsed
	 * @param maxSpeed Velocidad máxima del segmento
	 */
	public void processRoadStatus(String roadSegment, String status, int maxSpeed) {
		// Guardar velocidad máxima del segmento
		this.segmentMaxSpeeds.put(roadSegment, maxSpeed);
		
		// Aplicar auto-regulación según el estado
		if ("No_Manouvers".equals(status) || "Collapsed".equals(status)) {
			// Crear señal de límite de velocidad de 20 km/h
			createSpeedLimitSignal(roadSegment, 20);
		} else if ("Limited_Manouvers".equals(status)) {
			// Crear señal de límite de velocidad: maxSpeed - 20 km/h
			int speedLimit = Math.max(20, maxSpeed - 20);
			createSpeedLimitSignal(roadSegment, speedLimit);
		} else if ("Free_Flow".equals(status) || "Mostly_Free_Flow".equals(status)) {
			// Eliminar señal de límite de velocidad si existe
			removeSpeedLimitSignal(roadSegment);
		}
	}
	
	/**
	 * Crea o actualiza una señal de límite de velocidad para un segmento
	 * @param roadSegment Segmento de carretera
	 * @param speedLimit Límite de velocidad en km/h
	 */
	private void createSpeedLimitSignal(String roadSegment, int speedLimit) {
		SpeedLimitSignal existingSignal = this.activeSpeedLimitSignals.get(roadSegment);
		
		if (existingSignal != null && existingSignal.getValue() == speedLimit) {
			// Ya existe una señal con el mismo límite, no hacer nada
			return;
		}
		
		// Crear nueva señal (afecta a todo el segmento: posición 0 a 1000)
		SpeedLimitSignal signal = new SpeedLimitSignal(speedLimit, 0, 1000, Long.MAX_VALUE);
		this.activeSpeedLimitSignals.put(roadSegment, signal);
		
		// Publicar la señal
		if (this.infoPublisher != null && this.infoPublisher.isConnected()) {
			this.infoPublisher.publishSpeedLimitSignal(roadSegment, signal);
		}
		
		if (this.awsInfoPublisher != null && this.awsInfoPublisher.isConnected()) {
			this.awsInfoPublisher.publishSpeedLimitSignal(roadSegment, signal);
		}
		
		System.out.println("(RoadManager: " + this.managerId + ") Created speed-limit signal for " + roadSegment + ": " + speedLimit + " km/h");
	}
	
	/**
	 * Elimina la señal de límite de velocidad de un segmento
	 * @param roadSegment Segmento de carretera
	 */
	private void removeSpeedLimitSignal(String roadSegment) {
		SpeedLimitSignal existingSignal = this.activeSpeedLimitSignals.remove(roadSegment);
		
		if (existingSignal != null) {
			// Publicar eliminación de señal (valor 0 o señal con validez expirada)
			if (this.infoPublisher != null && this.infoPublisher.isConnected()) {
				this.infoPublisher.removeSpeedLimitSignal(roadSegment);
			}
			
			if (this.awsInfoPublisher != null && this.awsInfoPublisher.isConnected()) {
				this.awsInfoPublisher.removeSpeedLimitSignal(roadSegment);
			}
			
			System.out.println("(RoadManager: " + this.managerId + ") Removed speed-limit signal for " + roadSegment);
		}
	}
	
	/**
	 * Desconecta todos los componentes
	 */
	public void disconnect() {
		if (this.alertsSubscriber != null) {
			this.alertsSubscriber.disconnect();
		}
		if (this.infoPublisher != null) {
			this.infoPublisher.disconnect();
		}
		if (this.roadStatusSubscriber != null) {
			this.roadStatusSubscriber.disconnect();
		}
		if (this.awsAlertsSubscriber != null) {
			this.awsAlertsSubscriber.disconnect();
		}
		if (this.awsInfoPublisher != null) {
			this.awsInfoPublisher.disconnect();
		}
		if (this.awsRoadStatusSubscriber != null) {
			this.awsRoadStatusSubscriber.disconnect();
		}
	}
	
	public String getManagerId() {
		return this.managerId;
	}
}

