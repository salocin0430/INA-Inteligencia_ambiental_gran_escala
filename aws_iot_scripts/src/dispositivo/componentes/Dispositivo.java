package dispositivo.componentes;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import dispositivo.api.iot.infraestructure.Dispositivo_RegistradorMQTT;
import dispositivo.api.mqtt.Dispositivo_APIMQTT;
import dispositivo.api.rest.Dispositivo_APIREST;
import dispositivo.interfaces.IDispositivo;
import dispositivo.interfaces.IFuncion;

public class Dispositivo implements IDispositivo {
	
	protected String deviceId = null;
	protected boolean habilitado = true; // Por defecto habilitado

	protected Map<String, IFuncion> functions = null;
	protected Dispositivo_RegistradorMQTT registrador = null;
	protected Dispositivo_APIMQTT apiFuncionesMQTT = null;
	protected Dispositivo_APIREST apiFuncionesREST = null;
	
	
	public static Dispositivo build(String deviceId, String ip, String mqttBrokerURL) {
		Dispositivo dispositivo = new Dispositivo(deviceId);
		dispositivo.registrador = Dispositivo_RegistradorMQTT.build(deviceId, ip, mqttBrokerURL);
		dispositivo.apiFuncionesMQTT = Dispositivo_APIMQTT.build(dispositivo, mqttBrokerURL);
		dispositivo.apiFuncionesREST = Dispositivo_APIREST.build(dispositivo);
		return dispositivo;
	}

	public static Dispositivo build(String deviceId, String ip, int port, String mqttBrokerURL) {
		Dispositivo dispositivo = new Dispositivo(deviceId);
		dispositivo.registrador = Dispositivo_RegistradorMQTT.build(deviceId, ip, mqttBrokerURL);
		dispositivo.apiFuncionesMQTT = Dispositivo_APIMQTT.build(dispositivo, mqttBrokerURL);
		dispositivo.apiFuncionesREST = Dispositivo_APIREST.build(dispositivo, port);
		return dispositivo;
	}

	protected Dispositivo(String deviceId) {
		this.deviceId = deviceId;
	}
	
	@Override
	public String getId() {
		return this.deviceId;
	}

	protected Map<String, IFuncion> getFunctions() {
		return this.functions;
	}
	
	protected void setFunctions(Map<String, IFuncion> fs) {
		this.functions = fs;
	}
	
	@Override
	public Collection<IFuncion> getFunciones() {
		if ( this.getFunctions() == null )
			return null;
		return this.getFunctions().values();
	}
	
	
	@Override
	public IDispositivo addFuncion(IFuncion f) {
		if ( this.getFunctions() == null )
			this.setFunctions(new HashMap<String, IFuncion>());
		this.getFunctions().put(f.getId(), f);
		
		// Ejercicio 9: Configurar publisher MQTT en la función
		if (f instanceof dispositivo.componentes.Funcion && apiFuncionesMQTT != null) {
			dispositivo.api.mqtt.FuncionPublisher_APIMQTT publisher = 
				dispositivo.api.mqtt.FuncionPublisher_APIMQTT.build(apiFuncionesMQTT.getMqttClient(), this.getId());
			((dispositivo.componentes.Funcion) f).setMqttPublisher(publisher);
		}
		
		return this;
	}
	
	
	@Override
	public IFuncion getFuncion(String funcionId) {
		if ( this.getFunctions() == null )
			return null;
		return this.getFunctions().get(funcionId);
	}
	
		
	@Override
	public IDispositivo iniciar() {
		for(IFuncion f : this.getFunciones()) {
			f.iniciar();
		}

		this.registrador.registrar();
		this.apiFuncionesMQTT.iniciar();
		this.apiFuncionesREST.iniciar();
		
		// Ejercicio 9: Configurar publisher MQTT en funciones existentes
		if (this.getFunctions() != null) {
			for(IFuncion f : this.getFunctions().values()) {
				if (f instanceof dispositivo.componentes.Funcion) {
					dispositivo.api.mqtt.FuncionPublisher_APIMQTT publisher = 
						dispositivo.api.mqtt.FuncionPublisher_APIMQTT.build(apiFuncionesMQTT.getMqttClient(), this.getId());
					((dispositivo.componentes.Funcion) f).setMqttPublisher(publisher);
				}
			}
		}
		
		return this;
	}

	@Override
	public IDispositivo detener() {
		this.registrador.desregistrar();
		this.apiFuncionesMQTT.detener();
		this.apiFuncionesREST.detener();
		for(IFuncion f : this.getFunciones()) {
			f.detener();
		}
		return this;
	}
	
	// Ejercicio 4: Capacidad habilitar/deshabilitar dispositivo
	@Override
	public boolean isHabilitado() {
		return this.habilitado;
	}
	
	@Override
	public IDispositivo habilitar() {
		this.habilitado = true;
		// Al habilitar un dispositivo, se habilitan TODAS sus funciones
		if (this.getFunciones() != null) {
			for(IFuncion f : this.getFunciones()) {
				f.habilitar();
			}
		}
		return this;
	}
	
	@Override
	public IDispositivo deshabilitar() {
		this.habilitado = false;
		// Al deshabilitar un dispositivo, se deshabilitan TODAS sus funciones
		if (this.getFunciones() != null) {
			for(IFuncion f : this.getFunciones()) {
				f.deshabilitar();
			}
		}
		return this;
	}
	
}
