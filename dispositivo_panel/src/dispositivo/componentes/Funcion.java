package dispositivo.componentes;

import dispositivo.interfaces.FuncionStatus;
import dispositivo.interfaces.IFuncion;
import dispositivo.utils.MySimpleLogger;

public class Funcion implements IFuncion {
	
	protected String id = null;
	protected boolean habilitada = true; // Por defecto habilitada

	protected FuncionStatus initialStatus = null;
	protected FuncionStatus status = null;
	protected boolean isInitialized = false; // Bandera para detectar inicialización
	
	private String loggerId = null;
	
	public static Funcion build(String id) {
		return new Funcion(id, FuncionStatus.OFF);
	}
	
	public static Funcion build(String id, FuncionStatus initialStatus) {
		return new Funcion(id, initialStatus);
	}

	protected Funcion(String id, FuncionStatus initialStatus) {
		this.id = id;
		this.initialStatus = initialStatus;
		this.loggerId = "Funcion " + id;
	}
		
	@Override
	public String getId() {
		return this.id;
	}
		
	@Override
	public IFuncion encender() {
		if (!this.habilitada) {
			MySimpleLogger.warn(this.loggerId, "==> No se puede encender: función deshabilitada");
			return this;
		}
		MySimpleLogger.info(this.loggerId, "==> Encender");
		this.setStatus(FuncionStatus.ON);
		
		return this;
	}

	@Override
	public IFuncion apagar() {
		if (!this.habilitada) {
			MySimpleLogger.warn(this.loggerId, "==> No se puede apagar: función deshabilitada");
			return this;
		}
		MySimpleLogger.info(this.loggerId, "==> Apagar");
		this.setStatus(FuncionStatus.OFF);
		
		return this;
	}

	@Override
	public IFuncion parpadear() {
		if (!this.habilitada) {
			MySimpleLogger.warn(this.loggerId, "==> No se puede parpadear: función deshabilitada");
			return this;
		}
		MySimpleLogger.info(this.loggerId, "==> Parpadear");
		this.setStatus(FuncionStatus.BLINK);
		
		return this;
	}
	
	protected IFuncion _putIntoInitialStatus() {
		switch (this.initialStatus) {
		case ON:
			this.encender();
			break;
		case OFF:
			this.apagar();
			break;
		case BLINK:
			this.parpadear();
			break;

		default:
			break;
		}
		
		return this;

	}

	@Override
	public FuncionStatus getStatus() {
		return this.status;
	}
	
	protected IFuncion setStatus(FuncionStatus status) {
		// Ejercicio 9: Solo publicar si el estado realmente cambió
		boolean estadoCambio = (this.status != status);
		this.status = status;
		
		// Publicar cambio de estado solo si realmente cambió Y ya está inicializado
		if (estadoCambio && this.mqttPublisher != null && this.isInitialized) {
			this.mqttPublisher.publishStatus(this);
		}
		
		return this;
	}
	
	// Ejercicio 9: Referencia al publisher MQTT
	private dispositivo.api.mqtt.FuncionPublisher_APIMQTT mqttPublisher = null;
	
	public void setMqttPublisher(dispositivo.api.mqtt.FuncionPublisher_APIMQTT publisher) {
		this.mqttPublisher = publisher;
	}
	
	@Override
	public IFuncion iniciar() {
		this._putIntoInitialStatus();
		this.isInitialized = true; // Marcar como inicializada después del primer setStatus
		return this;
	}
	
	@Override
	public IFuncion detener() {
		return this;
	}
	
	// Ejercicio 4: Capacidad habilitar/deshabilitar función
	@Override
	public boolean isHabilitada() {
		return this.habilitada;
	}
	
	@Override
	public IFuncion habilitar() {
		this.habilitada = true;
		MySimpleLogger.info(this.loggerId, "==> Función habilitada");
		return this;
	}
	
	@Override
	public IFuncion deshabilitar() {
		this.habilitada = false;
		MySimpleLogger.info(this.loggerId, "==> Función deshabilitada");
		return this;
	}
	
}
