package dispositivo.iniciador;

import dispositivo.componentes.SignalSpeed;
import dispositivo.utils.MySimpleLogger;

public class SignalSpeedIniciador {

    public static void main(String[] args) {

        MySimpleLogger.info("SignalSpeedIniciador", "=== INICIANDO SEÑAL DE LÍMITE DE VELOCIDAD ===");

        // Verificar número de argumentos
        if (args.length < 6) {
            MySimpleLogger.error("SignalSpeedIniciador",
                "Número de argumentos incorrecto. Uso esperado: java SignalSpeedIniciador <mqtt-broker> <id-senal> <road-segment> <velocidad-maxima> <posicion-inicio> <posicion-fin>");
            System.exit(1);
        }

        try {
            String mqttBroker = args[0];
            String id = args[1];
            String roadSegment = args[2];
            int velocidadMaxima = Integer.parseInt(args[3]);
            int posicionInicio = Integer.parseInt(args[4]);
            int posicionFin = Integer.parseInt(args[5]);
            
            // Parámetro opcional 7: nombre del Thing en AWS IoT (para integración con AWS)
            String awsThingName = null;
            if (args.length >= 7) {
                awsThingName = args[6];
            }

            MySimpleLogger.info("SignalSpeedIniciador", "============================================");
            MySimpleLogger.info("SignalSpeedIniciador", "Configuración de Señal de Velocidad:");
            MySimpleLogger.info("SignalSpeedIniciador", "  MQTT Broker: " + mqttBroker);
            MySimpleLogger.info("SignalSpeedIniciador", "  ID Señal: " + id);
            MySimpleLogger.info("SignalSpeedIniciador", "  Road Segment: " + roadSegment);
            MySimpleLogger.info("SignalSpeedIniciador", "  Velocidad Máxima: " + velocidadMaxima);
            MySimpleLogger.info("SignalSpeedIniciador", "  Posición Inicio: " + posicionInicio);
            MySimpleLogger.info("SignalSpeedIniciador", "  Posición Fin: " + posicionFin);
            if (awsThingName != null) {
                MySimpleLogger.info("SignalSpeedIniciador", "  AWS Thing Name: " + awsThingName);
            }
            MySimpleLogger.info("SignalSpeedIniciador", "============================================");

            // Crear instancia SignalSpeed
            SignalSpeed signalSpeed = new SignalSpeed(roadSegment, id, velocidadMaxima, posicionInicio, posicionFin,
                                                      mqttBroker);
            
            // Habilitar integración con AWS IoT Device Shadow si se proporciona thingName
            if (awsThingName != null && !awsThingName.isEmpty()) {
                MySimpleLogger.info("SignalSpeedIniciador", "Habilitando integración con AWS IoT...");
                signalSpeed.enableAWSShadow(awsThingName);
                MySimpleLogger.info("SignalSpeedIniciador", "AWS IoT Device Shadow habilitado para Thing: " + awsThingName);
            }
            
            // Publicar estado inicial
            signalSpeed.publicarEstado();

            MySimpleLogger.info("SignalSpeedIniciador", "Señal iniciada correctamente");
            MySimpleLogger.info("SignalSpeedIniciador", "Presiona Ctrl+C para detener");
            MySimpleLogger.info("SignalSpeedIniciador", "============================================");
            
            // Mantener el programa corriendo
            // La señal está suscrita al topic 'step' y publicará su estado cuando se reciba un paso
            try {
                while (true) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                MySimpleLogger.info("SignalSpeedIniciador", "Programa interrumpido");
            } finally {
                // Cerrar todas las conexiones
                signalSpeed.cerrar();
                MySimpleLogger.info("SignalSpeedIniciador", "Conexiones cerradas");
            }
            return;

        } catch (Exception e) {
            MySimpleLogger.error("SignalSpeedIniciador", "Error al iniciar SignalSpeed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}