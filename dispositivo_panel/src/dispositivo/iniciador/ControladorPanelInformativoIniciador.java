package dispositivo.iniciador;
// Ejercicio 11 - Controlador Maestro-Esclavos Iniciador
import dispositivo.componentes.Dispositivo;
import dispositivo.componentes.Funcion;
import dispositivo.interfaces.FuncionStatus;
import dispositivo.interfaces.IDispositivo;
import dispositivo.interfaces.IFuncion;

import dispositivo.utils.MySimpleLogger;

public class ControladorPanelInformativoIniciador {
    
    public static void main(String[] args) {
        MySimpleLogger.info("ControladorPanelInformativoIniciador", "=== INICIANDO CONTROLADOR PANEL-INFORMATIVO ===");
        
        // Verificar argumentos
        if (args.length < 4) {
            MySimpleLogger.error("ControladorPanelInformativoIniciador","The number of arguments is not correct. Please, check the usage. You used " + String.valueOf(args.length) + " arguments.");
            MySimpleLogger.error("ControladorPanelInformativoIniciador", 
                "Uso: java ControladorPanelInformativoIniciador <mqtt-broker> <ttmiID> <road-segment> <ubicación-inicial>");
            MySimpleLogger.error("ControladorPanelInformativoIniciador", 
                "Ejemplo: java ControladorPanelInformativoIniciador tcp://tambori.dsic.upv.es:1883 ttmi050 R1s6a 50");
            System.exit(1);
        }
        
        String mqttBroker = args[0];
        String ttmiID = args[1];
        String roadSegment = args[2];
        int ubicacionInicial = Integer.parseInt(args[3]);
        
        // Mostrar configuración
        MySimpleLogger.info("ControladorPanelInformativoIniciador", "Configuración:");
        MySimpleLogger.info("ControladorPanelInformativoIniciador", "  MQTT Broker: " + mqttBroker);
        MySimpleLogger.info("ControladorPanelInformativoIniciador", "  ttmiID: " + ttmiID);
        MySimpleLogger.info("ControladorPanelInformativoIniciador", "  roadSegment: " + roadSegment);
        MySimpleLogger.info("ControladorPanelInformativoIniciador", "  ubicaciónInicial: " + ubicacionInicial);
        
        // Crear y configurar el controlador
        final ControladorPanelInformativo controlador;
        
        try {
            controlador = new ControladorPanelInformativo(mqttBroker, ttmiID, roadSegment, ubicacionInicial);

            IDispositivo d = Dispositivo.build(ttmiID, ttmiID+".iot.upv.es", 8182 , mqttBroker);
		
            // Añadimos funciones al dispositivo
            IFuncion f1 = Funcion.build("f1", FuncionStatus.OFF);
            d.addFuncion(f1);
            
            IFuncion f2 = Funcion.build("f2", FuncionStatus.OFF);
            d.addFuncion(f2);

            //Ejercicio 1 - Parpadear
            IFuncion f3 = Funcion.build("f3", FuncionStatus.OFF);
            d.addFuncion(f3);
            
            // Arrancamos el dispositivo
            d.iniciar();
            
            // Agregar shutdown hook para cerrar correctamente
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                MySimpleLogger.info("ControladorPanelInformativoIniciador", "Cerrando controlador...");
                controlador.cerrar();
            }));
            
            // Iniciar el controlador
            controlador.iniciar(d);
            
        } catch (Exception e) {
            MySimpleLogger.error("ControladorPanelInformativoIniciador", "Error fatal: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
