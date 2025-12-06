package roadmanager.impl;

/**
 * Representa una señal de límite de velocidad
 */
public class SpeedLimitSignal {
	private int value; // Velocidad máxima en km/h
	private int positionStart; // Posición inicial donde aplica
	private int positionEnd; // Posición final donde aplica
	private long validity; // Validez en milisegundos
	
	public SpeedLimitSignal(int value, int positionStart, int positionEnd, long validity) {
		this.value = value;
		this.positionStart = positionStart;
		this.positionEnd = positionEnd;
		this.validity = validity;
	}
	
	public int getValue() {
		return value;
	}
	
	public int getPositionStart() {
		return positionStart;
	}
	
	public int getPositionEnd() {
		return positionEnd;
	}
	
	public long getValidity() {
		return validity;
	}
	
	/**
	 * Verifica si esta señal afecta a la posición dada
	 */
	public boolean affectsPosition(int position) {
		return position >= positionStart && position <= positionEnd;
	}
	
	@Override
	public String toString() {
		return "SpeedLimitSignal{value=" + value + " km/h, range=[" + positionStart + "-" + positionEnd + "]}";
	}
}

