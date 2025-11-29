package smartcar.impl;

import ina.vehicle.navigation.components.Navigator;
import ina.vehicle.navigation.components.Route;
import ina.vehicle.navigation.interfaces.INavigator;
import ina.vehicle.navigation.interfaces.IRoadPoint;
import ina.vehicle.navigation.interfaces.IRoute;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/**
 * SmartCar extendido con Navigator para el Proyecto Práctico.
 * Integra el componente Navigator para calcular movimiento automático
 * basado en los pasos de simulación del servicio de tráfico.
 */
public class SmartCar {

	protected String smartCarID = null;
	protected String vehicleRole = "PrivateUsage"; // "PrivateUsage", "Ambulance", "Police", etc.
	protected int cruiserSpeed = 60; // Velocidad de crucero en km/h
	protected boolean ignoreLimits = false; // Para vehículos especiales
	
	protected RoadPlace rp = null;	// simula la ubicación actual del vehículo (legacy)
	protected INavigator navigator = null; // Navigator para calcular movimiento
	
	protected SmartCar_RoadInfoSubscriber subscriber = null;
	protected SmartCar_TrafficPublisher publisher = null;
	protected SmartCar_IncidentNotifier notifier = null;
	protected SmartCar_StepSubscriber stepSubscriber = null; // Suscriptor a pasos de simulación
	protected SmartCar_SignalsSubscriber signalsSubscriber = null; // Suscriptor a señales de tráfico
	
	protected String currentRoadSegment = null; // Segmento actual para detectar cambios
	protected int currentPosition = 0; // Posición actual en el segmento
	
	// Almacenamiento de señales de tráfico por segmento
	protected Map<String, List<SpeedLimitSignal>> speedLimitSignals = new HashMap<>();
	protected Map<String, List<TrafficLightSignal>> trafficLightSignals = new HashMap<>();
	
	// Cache de velocidad máxima por segmento (para evitar consultas REST repetidas)
	protected Map<String, Integer> segmentMaxSpeedCache = new HashMap<>();
	
	static final String REST_API_BASE = "http://tambori.dsic.upv.es:10082";
	
	public SmartCar(String id) {
		this.setSmartCarID(id);
		this.rp = new RoadPlace("R5s1", 10);
		this.currentRoadSegment = "R5s1";
		this.currentPosition = 10;

		// Inicializar y conectar el subscriber para recibir información de la carretera
		this.subscriber = new SmartCar_RoadInfoSubscriber(this);
		this.subscriber.connect();
		this.subscriber.subscribe("es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/R5s1/info");

		// Inicializar y conectar el publisher para publicar eventos de tráfico
		this.publisher = new SmartCar_TrafficPublisher(this);
		this.publisher.connect();

		// Inicializar y conectar el notifier para publicar alertas/incidentes
		this.notifier = new SmartCar_IncidentNotifier(id);
		this.notifier.connect();
		
		// Inicializar y conectar el suscriptor a pasos de simulación
		this.stepSubscriber = new SmartCar_StepSubscriber(this);
		this.stepSubscriber.connect();
		
		// Inicializar y conectar el suscriptor a señales de tráfico
		this.signalsSubscriber = new SmartCar_SignalsSubscriber(this);
		this.signalsSubscriber.connect();
		
		// NOTA: Navigator se inicializa cuando se asigna una ruta
		// Ver método setRoute()
	}
	
	
	public void setSmartCarID(String smartCarID) {
		this.smartCarID = smartCarID;
	}
	
	public String getSmartCarID() {
		return smartCarID;
	}
	
	public String getVehicleRole() {
		return vehicleRole;
	}
	
	public void setVehicleRole(String vehicleRole) {
		this.vehicleRole = vehicleRole;
	}
	
	public int getCruiserSpeed() {
		return cruiserSpeed;
	}
	
	public void setCruiserSpeed(int cruiserSpeed) {
		this.cruiserSpeed = cruiserSpeed;
	}
	
	public boolean isIgnoreLimits() {
		return ignoreLimits;
	}
	
	public void setIgnoreLimits(boolean ignoreLimits) {
		this.ignoreLimits = ignoreLimits;
	}
	
	public INavigator getNavigator() {
		return navigator;
	}
	
	/**
	 * Asigna una ruta al vehículo y crea el Navigator si no existe
	 */
	public void setRoute(IRoute route) {
		if (this.navigator == null) {
			this.navigator = new Navigator(this.smartCarID + "-navigator");
		}
		this.navigator.setRoute(route);
		this.navigator.startRouting();
		
		// Actualizar posición inicial
		IRoadPoint initialPos = this.navigator.getCurrentPosition();
		if (initialPos != null) {
			this.currentRoadSegment = initialPos.getRoadSegment();
			this.currentPosition = initialPos.getPosition();
			this.rp = new RoadPlace(this.currentRoadSegment, this.currentPosition);
			
			// Publicar VEHICLE_IN en el segmento inicial
			if (this.publisher != null) {
				this.publisher.publishVehicleIn(this.currentRoadSegment, this.currentPosition);
			}
			
			// Suscribirse al topic de info del segmento inicial
			if (this.subscriber != null) {
				String topic = "es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/" + this.currentRoadSegment + "/info";
				this.subscriber.subscribe(topic);
			}
			
			// Suscribirse al topic de señales del segmento inicial
			if (this.signalsSubscriber != null) {
				this.signalsSubscriber.subscribe(this.currentRoadSegment);
			}
		}
	}

	public RoadPlace getCurrentPlace() {
		return rp;
	}

	public void changeKm(int km) {
		this.getCurrentPlace().setKm(km);
		this.currentPosition = km;
	}
	
	public void changeRoad(String road, int km) {
		String previousRoad = this.getCurrentPlace().getRoad();
		
		// Si cambiamos de carretera, publicar VEHICLE_OUT en la anterior y VEHICLE_IN en la nueva
		if (!road.equals(previousRoad)) {
			// Publicar salida de la carretera anterior
			if (this.publisher != null) {
				this.publisher.publishVehicleOut(previousRoad, this.currentPosition);
			}
			
			// Actualizar la ubicación
			this.getCurrentPlace().setRoad(road);
			this.getCurrentPlace().setKm(km);
			this.currentRoadSegment = road;
			this.currentPosition = km;
			
			// Publicar entrada en la nueva carretera
			if (this.publisher != null) {
				this.publisher.publishVehicleIn(road, km);
			}
			
			// Actualizar la suscripción al topic de info de la nueva carretera
			if (this.subscriber != null) {
				// Desuscribirse del topic anterior
				String oldTopic = "es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/" + previousRoad + "/info";
				this.subscriber.unsubscribe(oldTopic);
				
				// Suscribirse al topic de la nueva carretera
				String newTopic = "es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/" + road + "/info";
				this.subscriber.subscribe(newTopic);
			}
		} else {
			// Si es la misma carretera, solo actualizar el km
			this.getCurrentPlace().setKm(km);
			this.currentPosition = km;
		}
	}
	
	/**
	 * Método llamado cada vez que se recibe un paso de simulación del topic 'step'
	 * Calcula el movimiento del vehículo usando el Navigator
	 */
	public void onSimulationStep() {
		if (this.navigator == null || !this.navigator.isRouting()) {
			return; // No hay Navigator o no está en modo routing
		}
		
		// Calcular velocidad actual (respetando límites, señales, etc.)
		int velocidadActual = calcularVelocidadActual();
		
		// Mover el Navigator (3 segundos = 3000 ms)
		this.navigator.move(3000, velocidadActual);
		
		// Obtener nueva posición
		IRoadPoint nuevaPosicion = this.navigator.getCurrentPosition();
		if (nuevaPosicion == null) {
			return;
		}
		
		String nuevoSegmento = nuevaPosicion.getRoadSegment();
		int nuevaPosicionKm = nuevaPosicion.getPosition();
		
		// Detectar si cambió de segmento
		if (!nuevoSegmento.equals(this.currentRoadSegment)) {
			// Cambió de segmento: publicar VEHICLE_OUT y VEHICLE_IN
			if (this.publisher != null) {
				this.publisher.publishVehicleOut(this.currentRoadSegment, this.currentPosition);
				this.publisher.publishVehicleIn(nuevoSegmento, nuevaPosicionKm);
			}
			
			// Actualizar suscripciones
			if (this.subscriber != null) {
				String oldTopic = "es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/" + this.currentRoadSegment + "/info";
				this.subscriber.unsubscribe(oldTopic);
				
				String newTopic = "es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/" + nuevoSegmento + "/info";
				this.subscriber.subscribe(newTopic);
			}
			
			// Actualizar suscripción a señales
			if (this.signalsSubscriber != null) {
				this.signalsSubscriber.unsubscribe(this.currentRoadSegment);
				this.signalsSubscriber.subscribe(nuevoSegmento);
			}
			
			// Limpiar señales y cache del segmento anterior
			// (Las señales del nuevo segmento se recibirán por MQTT cuando estén disponibles)
			this.speedLimitSignals.remove(this.currentRoadSegment);
			this.trafficLightSignals.remove(this.currentRoadSegment);
			this.segmentMaxSpeedCache.remove(this.currentRoadSegment);
			
			this.currentRoadSegment = nuevoSegmento;
		} else {
			// Mismo segmento: publicar VEHICLE_IN con nueva posición
			if (this.publisher != null) {
				this.publisher.publishVehicleIn(nuevoSegmento, nuevaPosicionKm);
			}
		}
		
		// Actualizar posición actual
		this.currentPosition = nuevaPosicionKm;
		this.rp = new RoadPlace(nuevoSegmento, nuevaPosicionKm);
	}
	
	/**
	 * Calcula la velocidad actual del vehículo.
	 * Debe respetar límites de velocidad del segmento, señales de tráfico, etc.
	 * Para vehículos especiales (ignoreLimits=true), siempre retorna la velocidad de crucero.
	 */
	private int calcularVelocidadActual() {
		// Si es vehículo especial, ignora límites
		if (this.ignoreLimits) {
			System.out.println("(SmartCar: " + this.smartCarID + ") [VELOCIDAD] Vehículo especial - usando velocidad crucero: " + this.cruiserSpeed + " km/h");
			return this.cruiserSpeed;
		}
		
		// 1. Obtener velocidad máxima del segmento (vía REST API)
		int maxSegmentSpeed = getMaxSpeedFromSegment(this.currentRoadSegment);
		System.out.println("(SmartCar: " + this.smartCarID + ") [VELOCIDAD] Segmento " + this.currentRoadSegment + " - max-speed: " + maxSegmentSpeed + " km/h");
		
		// 2. Inicializar velocidad mínima con el mínimo entre velocidad de crucero y límite del segmento
		int minSpeed = Math.min(this.cruiserSpeed, maxSegmentSpeed);
		System.out.println("(SmartCar: " + this.smartCarID + ") [VELOCIDAD] Velocidad crucero: " + this.cruiserSpeed + " km/h, min inicial: " + minSpeed + " km/h");
		
		// 3. Consultar señales speed-limit activas en la posición actual
		List<SpeedLimitSignal> speedLimits = this.speedLimitSignals.get(this.currentRoadSegment);
		if (speedLimits != null && !speedLimits.isEmpty()) {
			System.out.println("(SmartCar: " + this.smartCarID + ") [VELOCIDAD] Encontradas " + speedLimits.size() + " señal(es) speed-limit en segmento " + this.currentRoadSegment);
			for (SpeedLimitSignal signal : speedLimits) {
				if (signal.affectsPosition(this.currentPosition)) {
					System.out.println("(SmartCar: " + this.smartCarID + ") [VELOCIDAD] Señal speed-limit aplica (posición " + this.currentPosition + "): " + signal);
					minSpeed = Math.min(minSpeed, signal.getValue());
				} else {
					System.out.println("(SmartCar: " + this.smartCarID + ") [VELOCIDAD] Señal speed-limit NO aplica (posición " + this.currentPosition + " fuera de rango): " + signal);
				}
			}
		} else {
			System.out.println("(SmartCar: " + this.smartCarID + ") [VELOCIDAD] No hay señales speed-limit en segmento " + this.currentRoadSegment);
		}
		
		// 4. Consultar semáforos cercanos (< 50m)
		// Si hay un semáforo rojo a menos de 50m, detenerse (velocidad = 0)
		List<TrafficLightSignal> trafficLights = this.trafficLightSignals.get(this.currentRoadSegment);
		if (trafficLights != null && !trafficLights.isEmpty()) {
			System.out.println("(SmartCar: " + this.smartCarID + ") [VELOCIDAD] Encontrados " + trafficLights.size() + " semáforo(s) en segmento " + this.currentRoadSegment);
			for (TrafficLightSignal signal : trafficLights) {
				int distance = signal.getDistanceFrom(this.currentPosition);
				System.out.println("(SmartCar: " + this.smartCarID + ") [VELOCIDAD] Semáforo en posición " + signal.getPosition() + " (distancia: " + distance + "m, estado: " + signal.getState() + ")");
				if (distance < 50 && signal.isRed()) {
					// Semáforo rojo a menos de 50m: detenerse
					System.out.println("(SmartCar: " + this.smartCarID + ") [VELOCIDAD] ⛔ SEMÁFORO ROJO A " + distance + "m - DETENIÉNDOSE (velocidad = 0)");
					return 0;
				}
			}
		} else {
			System.out.println("(SmartCar: " + this.smartCarID + ") [VELOCIDAD] No hay semáforos en segmento " + this.currentRoadSegment);
		}
		
		System.out.println("(SmartCar: " + this.smartCarID + ") [VELOCIDAD] ✅ Velocidad final calculada: " + minSpeed + " km/h");
		return minSpeed;
	}
	
	/**
	 * Obtiene la velocidad máxima del segmento vía REST API
	 * Usa cache para evitar consultas repetidas
	 */
	private int getMaxSpeedFromSegment(String segment) {
		// Verificar cache primero
		if (this.segmentMaxSpeedCache.containsKey(segment)) {
			return this.segmentMaxSpeedCache.get(segment);
		}
		
		// Si no está en cache, consultar REST API
		try {
			URL url = new URL(REST_API_BASE + "/segment/" + segment);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(2000);
			conn.setReadTimeout(2000);
			
			int responseCode = conn.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				StringBuilder response = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					response.append(line);
				}
				in.close();
				
				// Parsear JSON
				JSONObject json = new JSONObject(response.toString());
				int maxSpeed = json.optInt("max-speed", 60); // Default 60 km/h si no existe
				
				// Guardar en cache
				this.segmentMaxSpeedCache.put(segment, maxSpeed);
				return maxSpeed;
			} else {
				// Si falla la consulta, usar valor por defecto
				System.err.println("(SmartCar: " + this.smartCarID + ") Failed to get max speed for segment " + segment + ", using default 60 km/h");
				return 60;
			}
		} catch (Exception e) {
			// Si hay error, usar valor por defecto
			System.err.println("(SmartCar: " + this.smartCarID + ") Error getting max speed for segment " + segment + ": " + e.getMessage() + ", using default 60 km/h");
			return 60;
		}
	}
	
	/**
	 * Añade una señal de límite de velocidad para un segmento
	 * Llamado por SmartCar_SignalsSubscriber cuando recibe un mensaje MQTT
	 */
	public void addSpeedLimitSignal(String segment, SpeedLimitSignal signal) {
		if (!this.speedLimitSignals.containsKey(segment)) {
			this.speedLimitSignals.put(segment, new ArrayList<>());
		}
		// Reemplazar señales existentes del mismo tipo (solo puede haber una por segmento según el proyecto)
		// O podríamos mantener múltiples y usar la más restrictiva
		this.speedLimitSignals.get(segment).clear(); // Por simplicidad, solo mantener la última
		this.speedLimitSignals.get(segment).add(signal);
	}
	
	/**
	 * Añade una señal de semáforo para un segmento
	 * Llamado por SmartCar_SignalsSubscriber cuando recibe un mensaje MQTT
	 */
	public void addTrafficLightSignal(String segment, TrafficLightSignal signal) {
		if (!this.trafficLightSignals.containsKey(segment)) {
			this.trafficLightSignals.put(segment, new ArrayList<>());
		}
		// Puede haber múltiples semáforos en un segmento
		this.trafficLightSignals.get(segment).add(signal);
	}
	
	public void notifyIncident(String incidentType) {
		if ( this.notifier == null )
			return;
		
		this.notifier.alert(this.getSmartCarID(), incidentType, this.getCurrentPlace());
	}
	
	/**
	 * Desconecta todos los componentes MQTT
	 */
	public void disconnect() {
		if (this.subscriber != null) {
			this.subscriber.disconnect();
		}
		if (this.publisher != null) {
			this.publisher.disconnect();
		}
		if (this.notifier != null) {
			this.notifier.disconnect();
		}
		if (this.stepSubscriber != null) {
			this.stepSubscriber.disconnect();
		}
		if (this.signalsSubscriber != null) {
			this.signalsSubscriber.disconnect();
		}
	}

}
