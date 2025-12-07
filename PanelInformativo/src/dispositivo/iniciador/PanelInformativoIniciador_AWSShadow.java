package dispositivo.iniciador;

import dispositivo.componentes.PanelInformativo;
import dispositivo.utils.MySimpleLogger;

/**
 * Iniciador de PanelInformativo con soporte completo para AWS IoT Device Shadow.
 * Este iniciador habilita automáticamente la integración con AWS al crear el dispositivo.
 * 
 * Uso:
 *   java PanelInformativoIniciador_AWSShadow <mqtt-broker> <ttmiID> <road-segment> <ubicación-inicial> <aws-thing-name>
 * 
 * Ejemplo:
 *   java PanelInformativoIniciador_AWSShadow tcp://tambori.dsic.upv.es:1883 ttmi050 R1s6a 50 panel-R1s6a-001
 */
public class PanelInformativoIniciador_AWSShadow {
    
    public static void main(String[] args) {
        MySimpleLogger.info("PanelInformativoIniciador_AWSShadow", "=== INICIANDO CONTROLADOR PANEL-INFORMATIVO CON AWS ===" );
        
        // Verificar argumentos
        if (args.length < 5) {
            MySimpleLogger.error("PanelInformativoIniciador_AWSShadow","The number of arguments is not correct. Please, check the usage. You used " + String.valueOf(args.length) + " arguments.");
            MySimpleLogger.error("PanelInformativoIniciador_AWSShadow", 
                "Uso: java PanelInformativoIniciador_AWSShadow <mqtt-broker> <ttmiID> <road-segment> <ubicación-inicial> <aws-thing-name>");
            MySimpleLogger.error("PanelInformativoIniciador_AWSShadow", 
                "Ejemplo: java PanelInformativoIniciador_AWSShadow tcp://tambori.dsic.upv.es:1883 ttmi050 R1s6a 50 panel-R1s6a-001");
            System.exit(1);
        }
        
        String mqttBroker = args[0];
        String ttmiID = args[1];
        String roadSegment = args[2];
        int ubicacionInicial = Integer.parseInt(args[3]);
        String awsThingName = args[4];
        
        // Mostrar configuración
        MySimpleLogger.info("PanelInformativoIniciador_AWSShadow", "Configuración:");
        MySimpleLogger.info("PanelInformativoIniciador_AWSShadow", "  MQTT Broker: " + mqttBroker);
        MySimpleLogger.info("PanelInformativoIniciador_AWSShadow", "  ttmiID: " + ttmiID);
        MySimpleLogger.info("PanelInformativoIniciador_AWSShadow", "  roadSegment: " + roadSegment);
        MySimpleLogger.info("PanelInformativoIniciador_AWSShadow", "  ubicaciónInicial: " + ubicacionInicial);
        MySimpleLogger.info("PanelInformativoIniciador_AWSShadow", "  AWS IoT Thing Name: " + awsThingName);
        
        // Crear y configurar el PanelInformativo
        final PanelInformativo panelInformativo;
        
        try {
            panelInformativo = new PanelInformativo(mqttBroker, ttmiID, roadSegment, ubicacionInicial);
            
            // Habilitar integración con AWS IoT Device Shadow
            MySimpleLogger.info("PanelInformativoIniciador_AWSShadow", "Habilitando integración AWS IoT Device Shadow...");
            panelInformativo.enableAWSShadow(awsThingName);
            MySimpleLogger.info("PanelInformativoIniciador_AWSShadow", "✓ AWS IoT Device Shadow habilitado para Thing: " + awsThingName);
            
            // Agregar shutdown hook para cerrar correctamente
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                MySimpleLogger.info("PanelInformativoIniciador_AWSShadow", "Cerrando controlador...");
                panelInformativo.cerrar();
            }));
            
            // Iniciar el controlador
            panelInformativo.iniciar();
            
        } catch (Exception e) {
            MySimpleLogger.error("PanelInformativoIniciador_AWSShadow", "Error fatal: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
