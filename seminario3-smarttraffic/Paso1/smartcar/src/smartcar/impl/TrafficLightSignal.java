package smartcar.impl;

/**
 * Representa una señal de semáforo
 */
public class TrafficLightSignal {
	private String state; // "RED", "GREEN", "YELLOW", "HLL", "LLH"
	private int position; // Posición del semáforo en el segmento
	
	public TrafficLightSignal(String state, int position) {
		this.state = state;
		this.position = position;
	}
	
	public String getState() {
		return state;
	}
	
	public int getPosition() {
		return position;
	}
	
	/**
	 * Verifica si el semáforo está en rojo
	 */
	public boolean isRed() {
		return "RED".equalsIgnoreCase(state) || "HLL".equalsIgnoreCase(state);
	}
	
	/**
	 * Verifica si el semáforo está en verde
	 */
	public boolean isGreen() {
		return "GREEN".equalsIgnoreCase(state) || "LLH".equalsIgnoreCase(state);
	}
	
	/**
	 * Calcula la distancia desde la posición dada hasta el semáforo
	 */
	public int getDistanceFrom(int position) {
		return Math.abs(position - this.position);
	}
	
	@Override
	public String toString() {
		return "TrafficLightSignal{state=" + state + ", position=" + position + "}";
	}
}

