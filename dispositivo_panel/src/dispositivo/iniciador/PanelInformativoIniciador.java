package dispositivo.iniciador;

import dispositivo.componentes.PanelInformativo;
import dispositivo.utils.MySimpleLogger;

public class PanelInformativoIniciador {
    
    public static void main(String[] args) {
        MySimpleLogger.info("PanelInformativoIniciador", "=== INICIANDO CONTROLADOR PANEL-INFORMATIVO ===");
        
        // Verificar argumentos
        if (args.length < 4) {
            MySimpleLogger.error("PanelInformativoIniciador","The number of arguments is not correct. Please, check the usage. You used " + String.valueOf(args.length) + " arguments.");
            MySimpleLogger.error("CPanelInformativoIniciador", 
                "Uso: java PanelInformativoIniciador <mqtt-broker> <ttmiID> <road-segment> <ubicación-inicial>");
            MySimpleLogger.error("PanelInformativoIniciador", 
                "Ejemplo: java PanelInformativoIniciador tcp://tambori.dsic.upv.es:1883 ttmi050 R1s6a 50");
            System.exit(1);
        }
        
        String mqttBroker = args[0];
        String ttmiID = args[1];
        String roadSegment = args[2];
        int ubicacionInicial = Integer.parseInt(args[3]);
        
        // Mostrar configuración
        MySimpleLogger.info("PanelInformativoIniciador", "Configuración:");
        MySimpleLogger.info("PanelInformativoIniciador", "  MQTT Broker: " + mqttBroker);
        MySimpleLogger.info("PanelInformativoIniciador", "  ttmiID: " + ttmiID);
        MySimpleLogger.info("PanelInformativoIniciador", "  roadSegment: " + roadSegment);
        MySimpleLogger.info("PanelInformativoIniciador", "  ubicaciónInicial: " + ubicacionInicial);
        
        // Crear y configurar el PanelInformativo
        final PanelInformativo panelInformativo;
        
        try {
            panelInformativo = new PanelInformativo(mqttBroker, ttmiID, roadSegment, ubicacionInicial);
            
            // Agregar shutdown hook para cerrar correctamente
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                MySimpleLogger.info("ControladorPanelInformativoIniciador", "Cerrando controlador...");
                panelInformativo.cerrar();
            }));
            
            // Iniciar el controlador
            panelInformativo.iniciar();
            
        } catch (Exception e) {
            MySimpleLogger.error("ControladorPanelInformativoIniciador", "Error fatal: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
