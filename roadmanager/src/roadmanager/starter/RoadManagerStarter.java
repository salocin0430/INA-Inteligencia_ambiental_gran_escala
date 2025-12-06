package roadmanager.starter;

import roadmanager.impl.RoadManager;

/**
 * Clase principal para iniciar el Gestor de Carreteras (Road Manager)
 */
public class RoadManagerStarter {
	
	public static void main(String[] args) {
		System.out.println("==========================================");
		System.out.println("Gestor de Carreteras (Road Manager)");
		System.out.println("==========================================");
		System.out.println("");
		
		// Crear instancia del Road Manager
		RoadManager roadManager = new RoadManager("RoadManager001");
		
		// Habilitar integración con AWS IoT
		roadManager.enableAWSIoT();
		
		System.out.println("");
		System.out.println("Road Manager iniciado y funcionando");
		System.out.println("Monitorizando alertas de todos los segmentos");
		System.out.println("Auto-regulando velocidad según densidad de tráfico");
		System.out.println("Presiona Ctrl+C para detener");
		System.out.println("==========================================");
		
		// Mantener el programa ejecutándose
		try {
			Thread.sleep(Long.MAX_VALUE);
		} catch (InterruptedException e) {
			System.out.println("Interrumpido, desconectando...");
			roadManager.disconnect();
		}
	}
}

