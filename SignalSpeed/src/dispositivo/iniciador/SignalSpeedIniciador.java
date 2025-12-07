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

            MySimpleLogger.info("SignalSpeedIniciador", "Configuración:");
            MySimpleLogger.info("SignalSpeedIniciador", "  MQTT Broker: " + mqttBroker);
            MySimpleLogger.info("SignalSpeedIniciador", "  ID Señal: " + id);
            MySimpleLogger.info("SignalSpeedIniciador", "  Road Segment: " + roadSegment);
            MySimpleLogger.info("SignalSpeedIniciador", "  Velocidad Máxima: " + velocidadMaxima);
            MySimpleLogger.info("SignalSpeedIniciador", "  Posición Inicio: " + posicionInicio);
            MySimpleLogger.info("SignalSpeedIniciador", "  Posición Fin: " + posicionFin);

            // Crear instancia SignalSpeed
            SignalSpeed signalSpeed = new SignalSpeed(roadSegment, id, velocidadMaxima, posicionInicio, posicionFin,
                                                      mqttBroker);
                                                    
            signalSpeed.publicarEstado();

            // Añade aquí el arranque de publicación periódica o lógica necesaria
            // Por ejemplo, si SignalSpeed tiene un método iniciarPublicacionPeriodicá(intervaloMs)
            // signalSpeed.iniciarPublicacionPeriodica(1000);

            // Para evitar cerrar el programa inmediatamente (si es necesario)
            // Thread.sleep o espera eventos MQTT, según tu diseño

        } catch (Exception e) {
            MySimpleLogger.error("SignalSpeedIniciador", "Error al iniciar SignalSpeed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}