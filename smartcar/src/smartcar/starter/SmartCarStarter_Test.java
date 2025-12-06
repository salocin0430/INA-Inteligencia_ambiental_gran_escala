package smartcar.starter;

import smartcar.impl.SmartCar;
import ina.vehicle.navigation.components.Route;
import ina.vehicle.navigation.interfaces.IRoute;

/**
 * Starter parametrizable para crear diferentes tipos de vehículos para pruebas.
 * 
 * Uso:
 *   java SmartCarStarter_Test <tipo> <id> <thingName> <velocidad> <ruta>
 * 
 * Tipos: normal, police, ambulance
 * 
 * Ejemplo:
 *   java SmartCarStarter_Test normal SmartCar001 SmartCar097 60 "R5s1:0:580,R1s4a:490:600"
 */
public class SmartCarStarter_Test {

	public static void main(String[] args) {
		
		if (args.length < 4) {
			System.out.println("Uso: SmartCarStarter_Test <tipo> <id> <thingName> <velocidad> [ruta]");
			System.out.println("  tipo: normal, police, ambulance");
			System.out.println("  id: identificador del vehículo (ej: SmartCar001)");
			System.out.println("  thingName: nombre del Thing en AWS IoT (ej: SmartCar097)");
			System.out.println("  velocidad: velocidad de crucero en km/h (ej: 60)");
			System.out.println("  ruta: formato 'segmento1:inicio:fin,segmento2:inicio:fin' (opcional)");
			System.out.println("    Ejemplo: R5s1:0:580,R1s4a:490:600");
			System.out.println("    Si no se especifica, usa ruta por defecto: R5s1:0:580,R1s4a:490:600");
			System.exit(1);
		}
		
		String tipo = args[0].toLowerCase();
		String id = args[1];
		String thingName = args[2];
		int velocidad = Integer.parseInt(args[3]);
		
		// Ruta por defecto si no se especifica
		String rutaStr = args.length > 4 ? args[4] : "R5s1:0:580,R1s4a:490:600";
		
		// Crear vehículo según el tipo
		SmartCar car = null;
		IRoute ruta = parseRoute(rutaStr);
		
		switch (tipo) {
			case "normal":
				car = new SmartCar(id);
				car.setVehicleRole("PrivateUsage");
				car.setCruiserSpeed(velocidad);
				car.setRoute(ruta);
				System.out.println("✅ Vehículo NORMAL creado: " + id);
				break;
				
			case "police":
				car = SmartCar.createPolice(id, velocidad, ruta);
				System.out.println("✅ Vehículo POLICÍA creado: " + id);
				break;
				
			case "ambulance":
				car = SmartCar.createAmbulance(id, velocidad, ruta);
				System.out.println("✅ Vehículo AMBULANCIA creado: " + id);
				break;
				
			default:
				System.err.println("❌ Tipo inválido: " + tipo);
				System.err.println("Tipos válidos: normal, police, ambulance");
				System.exit(1);
		}
		
		// Habilitar AWS IoT Device Shadow
		car.enableAWSShadow(thingName);
		
		System.out.println("==========================================");
		System.out.println("SmartCar iniciado");
		System.out.println("  Tipo: " + tipo.toUpperCase());
		System.out.println("  ID: " + id);
		System.out.println("  Thing Name: " + thingName);
		System.out.println("  Velocidad: " + velocidad + " km/h");
		System.out.println("  Ruta: " + rutaStr);
		System.out.println("==========================================");
		System.out.println("El vehículo se moverá automáticamente");
		System.out.println("Presiona Ctrl+C para detener");
		System.out.println("==========================================");
		
		// Mantener corriendo
		try {
			while (true) {
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			System.out.println("Programa interrumpido");
		} finally {
			car.disconnect();
			System.out.println("SmartCar desconectado");
		}
	}
	
	/**
	 * Parsea una cadena de ruta en formato "segmento1:inicio:fin,segmento2:inicio:fin"
	 */
	private static IRoute parseRoute(String rutaStr) {
		Route ruta = new Route();
		
		String[] fragmentos = rutaStr.split(",");
		for (String fragmento : fragmentos) {
			String[] partes = fragmento.trim().split(":");
			if (partes.length == 3) {
				String segmento = partes[0].trim();
				int inicio = Integer.parseInt(partes[1].trim());
				int fin = Integer.parseInt(partes[2].trim());
				ruta.addRouteFragment(segmento, inicio, fin);
				System.out.println("  Ruta: " + segmento + " desde " + inicio + " hasta " + fin);
			}
		}
		
		return ruta;
	}
}

