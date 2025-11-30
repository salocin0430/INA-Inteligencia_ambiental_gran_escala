package dispositivo.iniciador;
// Ejercicio 11 - Controlador Maestro-Esclavos

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import dispositivo.interfaces.IDispositivo;
import dispositivo.utils.MySimpleLogger;

import dispositivo.componentes.Dispositivo;
import dispositivo.componentes.Funcion;
import dispositivo.interfaces.FuncionStatus;
import dispositivo.interfaces.IFuncion;


public class ControladorPanelInformativo implements MqttCallback {
    
    private MqttClient mqttClientSubscriber;  // Para suscribirse (recibir)
    private MqttClient mqttClientPublisher;   // Para publicar
    private String roadSegment;
    private String loggerId;
    private String ttmiID;
    private String topicBase;
    private String topicInfo;
    private String topicTraffic;
    private String topicAlerts;
    private int ubicacionInicial;

    private IDispositivo semaforo;

    Map<String, Integer> vehiculosEspecialesEnSegmentoCerca = new HashMap<>();
    Map<String, Integer> vehiculosEspecialesEnSegmentoLejos = new HashMap<>();

    private int contadorAccidentes = 0;  // Contador inicializado a 0

    
    public ControladorPanelInformativo(String mqttBroker, String ttmiID, String roadSegment, int ubicacionInicial) {
        this.roadSegment = roadSegment;
        this.loggerId = "PanelInformativoControlador";
        this.ttmiID = ttmiID;
        this.ubicacionInicial = ubicacionInicial;
        this.topicBase = "es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/"; // Usar el ID del maestro para el topic base
        this.topicInfo = topicBase + roadSegment + "/info";
        this.topicTraffic = topicBase + roadSegment + "/traffic";
        this.topicAlerts = topicBase + roadSegment + "/alerts";
        
        try {
            // Crear cliente MQTT para suscripciones (recibir)
            this.mqttClientSubscriber = new MqttClient(mqttBroker, "PanelInformativoControlador_Sub", new MemoryPersistence());
            
            // Crear cliente MQTT para publicaciones (enviar)
            this.mqttClientPublisher = new MqttClient(mqttBroker, "PanelInformativoControlador_Pub", new MemoryPersistence());
            
            // Configurar opciones de conexión
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setKeepAliveInterval(60);
            
            // Configurar callback solo en el subscriber
            this.mqttClientSubscriber.setCallback(this);
            
            // Conectar ambos clientes al broker
            this.mqttClientSubscriber.connect(connOpts);
            this.mqttClientPublisher.connect(connOpts);
            MySimpleLogger.info(loggerId, "Conectado al broker MQTT: " + mqttBroker);
            
        } catch (MqttException e) {
            MySimpleLogger.error(loggerId, "Error al conectar con MQTT: " + e.getMessage());
            throw new RuntimeException("No se pudo conectar al broker MQTT", e);
        }
    }
    
    /**
     * Inicia el controlador maestro-esclavos
     */
    public void iniciar(IDispositivo semaforo) throws MqttException {
        this.semaforo = semaforo;


        MySimpleLogger.info(loggerId, "=== INICIANDO CONTROLADOR PanelInformativo ===");
        MySimpleLogger.info(loggerId, "RoadSegment: " + roadSegment);
        
        // Suscribirse al topic de información de la función f1 del maestro
        MySimpleLogger.info(loggerId, "Suscribiendo al topic: " + topicInfo);
        mqttClientSubscriber.subscribe(topicInfo, 0);
        MySimpleLogger.info(loggerId, "Suscrito al topic: " + topicInfo);
        MySimpleLogger.info(loggerId, "Suscribiendo al topic: " + topicTraffic);
        mqttClientSubscriber.subscribe(topicTraffic, 0);
        MySimpleLogger.info(loggerId, "Suscrito al topic: " + topicTraffic);
        MySimpleLogger.info(loggerId, "Suscribiendo al topic: " + topicAlerts);
        mqttClientSubscriber.subscribe(topicAlerts, 0);        
        MySimpleLogger.info(loggerId, "Suscrito al topic: " + topicAlerts);
    }
    
    /**
     * Callback cuando llega un mensaje MQTT
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        MySimpleLogger.info(loggerId, "Mensaje recibido en topic " + topic + ": " + payload);
        MySimpleLogger.info(loggerId, "Cliente MQTT Subscriber conectado: " + (mqttClientSubscriber != null && mqttClientSubscriber.isConnected()));
        MySimpleLogger.info(loggerId, "Cliente MQTT Publisher conectado: " + (mqttClientPublisher != null && mqttClientPublisher.isConnected()));
        
        try {

            // Parsear el mensaje JSON
            JSONObject statusMessage = new JSONObject(payload);
            JSONObject msg = statusMessage.getJSONObject("msg");

            if (topic.equals(topicTraffic)) {
                // Obtener el objeto "msg"

                
                // Obtener el valor del campo "status"
                String status = msg.getString("status");
                MySimpleLogger.info(loggerId, "Status obtenido: " + status);


                switch (status) {
                    case "Free_Flow", "Mostly_Free_Flow" -> {
                        semaforo.getFuncion("f1").apagar();
                        MySimpleLogger.info(loggerId, "Apagando f1");
                    }
                    case "Limited_Manouvers" -> {
                        semaforo.getFuncion("f1").parpadear();  
                        MySimpleLogger.info(loggerId, "Parpadeando f1");
                    }
                    case "No_Manouvers", "Collapsed" -> {
                        semaforo.getFuncion("f1").encender();
                        MySimpleLogger.info(loggerId, "Encendiendo f1");
                    }
                    default -> {
                        MySimpleLogger.warn(loggerId, "Estado desconocido: " + status);
                    }
                }
            } else if (topic.equals(topicAlerts)) {

                String type = statusMessage.getString("type");

                if(type.equals("ACCIDENT")) {

                    String accidenteId = msg.getString("id");
                    String event = msg.getString("event");

                    if(event.equals("OPEN")) {
                        contadorAccidentes++;
                    } else if (event.equals("CLOSE")) {
                        contadorAccidentes = Math.max(0, contadorAccidentes - 1);
                    }

                    MySimpleLogger.info(loggerId, "Accidente " + accidenteId + " (" + event + "): número de accidentes: " + contadorAccidentes);

                    if(contadorAccidentes > 0){
                        semaforo.getFuncion("f2").encender();
                        MySimpleLogger.info(loggerId, "Encendiendo f2");
                    } else {
                        semaforo.getFuncion("f2").apagar();
                        MySimpleLogger.info(loggerId, "Apagando f2");
                    }
                }

            } else if (topic.equals(topicInfo)) {
                
                String action = msg.getString("action");              // VEHICLE_IN o VEHICLE_OUT
                String vehicleRole = msg.getString("vehicle-role");   // Ambulance, Police, PrivateUsage...
                String vehicleId = msg.getString("vehicle-id");
                int position = msg.getInt("position");

                boolean esVehiculoEspecial = vehicleRole.equals("Ambulance") || vehicleRole.equals("Police");
                int diferenciaPosicion = Math.abs(position - ubicacionInicial);

                if (esVehiculoEspecial) {
                    if ("VEHICLE_IN".equals(action)) {
                        // Añadir o actualizar posición del vehículo especial
                        if (diferenciaPosicion < 200) {
                            vehiculosEspecialesEnSegmentoCerca.put(vehicleId, position);
                            vehiculosEspecialesEnSegmentoLejos.remove(vehicleId);
                        } else {
                            vehiculosEspecialesEnSegmentoLejos.put(vehicleId, position);
                            vehiculosEspecialesEnSegmentoCerca.remove(vehicleId);
                        }
                        
                    } else if ("VEHICLE_OUT".equals(action)) {
                        // Eliminar vehículo porque salió del segmento
                        vehiculosEspecialesEnSegmentoCerca.remove(vehicleId);
                        vehiculosEspecialesEnSegmentoLejos.remove(vehicleId);
                    }

                    int vehiculosCerca = vehiculosEspecialesEnSegmentoCerca.size();
                    int vehiculosLejos = vehiculosEspecialesEnSegmentoLejos.size();

                    if (vehiculosCerca == 0 && vehiculosLejos == 0) {
                        semaforo.getFuncion("f3").apagar();
                        MySimpleLogger.info(loggerId, "Apagando f3");
                    } else if (vehiculosCerca > 0) {
                        semaforo.getFuncion("f3").parpadear();
                        MySimpleLogger.info(loggerId, "Parpadeando f3");
                    } else if (vehiculosLejos > 0) {
                        semaforo.getFuncion("f3").encender();
                        MySimpleLogger.info(loggerId, "Encendiendo f3");
                    }
                }
            } else {
                MySimpleLogger.warn(loggerId, "Topic desconocido: " + topic);
            }

            
        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error al procesar mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    /**
     * Envía un comando MQTT a una función específica
     */
    private void enviarComando(String dispositivoId, String funcionId, String accion) throws MqttException {
        String topic = "ina_08/dispositivo/" + dispositivoId + "/funcion/" + funcionId + "/comandos";
        
        try {
            JSONObject mensaje = new JSONObject();
            mensaje.put("accion", accion);
            
            MqttMessage mqttMessage = new MqttMessage(mensaje.toString().getBytes());
            mqttMessage.setQos(0);
            mqttMessage.setRetained(false);
            
            mqttClient.publish(topic, mqttMessage);
            MySimpleLogger.debug(loggerId, "Enviado comando a " + dispositivoId + "/" + funcionId + ": " + accion);
        } catch (org.json.JSONException e) {
            MySimpleLogger.error(loggerId, "Error al crear mensaje JSON: " + e.getMessage());
            throw new MqttException(e);
        }
    }
    
    /**
     * Cierra la conexión MQTT
     */
    public void cerrar() {
        try {
            if (mqttClientSubscriber != null && mqttClientSubscriber.isConnected()) {
                mqttClientSubscriber.disconnect();
                mqttClientSubscriber.close();
                MySimpleLogger.info(loggerId, "Conexión MQTT Subscriber cerrada");
            }
            if (mqttClientPublisher != null && mqttClientPublisher.isConnected()) {
                mqttClientPublisher.disconnect();
                mqttClientPublisher.close();
                MySimpleLogger.info(loggerId, "Conexión MQTT Publisher cerrada");
            }
        } catch (MqttException e) {
            MySimpleLogger.error(loggerId, "Error al cerrar conexiones MQTT: " + e.getMessage());
        }
    }
    
    // Métodos de callback requeridos por MqttCallback
    @Override
    public void connectionLost(Throwable cause) {
        MySimpleLogger.error(loggerId, "Conexión MQTT perdida: " + (cause != null ? cause.getMessage() : "Desconocido"));
        if (cause != null) {
            cause.printStackTrace();
        }
        
        // Intentar reconectar automáticamente con retry
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                MySimpleLogger.info(loggerId, "Intentando reconectar... (intento " + (retryCount + 1) + "/" + maxRetries + ")");
                
                if (mqttClientSubscriber != null && !mqttClientSubscriber.isConnected()) {
                    // Configurar opciones de conexión
                    MqttConnectOptions connOpts = new MqttConnectOptions();
                    connOpts.setCleanSession(true);
                    connOpts.setKeepAliveInterval(60);
                    
                    mqttClientSubscriber.connect(connOpts);
                    
                    // Re-suscribirse al topic
                    mqttClientSubscriber.subscribe(topicInfo, 0);
                    MySimpleLogger.info(loggerId, "Reconectado y re-suscrito al topic: " + topicInfo);
                    mqttClientSubscriber.subscribe(topicTraffic);
                    MySimpleLogger.info(loggerId, "Reconectado y re-suscrito al topic: " + topicTraffic);
                    mqttClientSubscriber.subscribe(topicAlerts);
                    MySimpleLogger.info(loggerId, "Reconectado y re-suscrito al topic: " + topicAlerts);
                    return; // Éxito, salir del bucle
                }
            } catch (MqttException e) {
                retryCount++;
                MySimpleLogger.error(loggerId, "Error al reconectar (intento " + retryCount + "): " + e.getMessage());
                
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(2000); // Esperar 2 segundos antes del siguiente intento
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        MySimpleLogger.error(loggerId, "No se pudo reconectar después de " + maxRetries + " intentos");
    }
    
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // No necesitamos hacer nada aquí
    }
}
