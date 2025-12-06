package roadmanager.starter;

import roadmanager.impl.RoadManager;

/**
 * Starter parametrizable para Road Manager
 * 
 * Uso:
 *   java RoadManagerStarter_Test [managerId] [thingName]
 * 
 * Ejemplo:
 *   java RoadManagerStarter_Test RoadManager001 RoadManager097
 */
public class RoadManagerStarter_Test {
	
	public static void main(String[] args) {
		
		// Parámetros opcionales
		String managerId = args.length > 0 ? args[0] : "RoadManager001";
		String thingName = args.length > 1 ? args[1] : "RoadManager097";
		
		System.out.println("==========================================");
		System.out.println("Gestor de Carreteras (Road Manager)");
		System.out.println("==========================================");
		System.out.println("Manager ID: " + managerId);
		System.out.println("Thing Name: " + thingName);
		System.out.println("==========================================");
		System.out.println("");
		
		// Crear instancia del Road Manager
		RoadManager roadManager = new RoadManager(managerId);
		
		// Habilitar integración con AWS IoT
		roadManager.enableAWSIoT();
		
		System.out.println("");
		System.out.println("✅ Road Manager iniciado y funcionando");
		System.out.println("  - Monitorizando alertas de todos los segmentos");
		System.out.println("  - Auto-regulando velocidad según densidad de tráfico");
		System.out.println("  - Retransmitiendo alertas al canal de información");
		System.out.println("");
		System.out.println("Presiona Ctrl+C para detener");
		System.out.println("==========================================");
		
		// Mantener el programa ejecutándose
		try {
			Thread.sleep(Long.MAX_VALUE);
		} catch (InterruptedException e) {
			System.out.println("Interrumpido, desconectando...");
			roadManager.disconnect();
			System.out.println("Road Manager desconectado");
		}
	}
}

