package dispositivo.interfaces;

import java.util.Collection;

public interface IDispositivo {

	public String getId();
	
	public IDispositivo iniciar();
	public IDispositivo detener();
		
	public IDispositivo addFuncion(IFuncion f);
	public IFuncion getFuncion(String funcionId);
	public Collection<IFuncion> getFunciones();
	
	// Ejercicio 4: Capacidad habilitar/deshabilitar dispositivo
	public boolean isHabilitado();
	public IDispositivo habilitar();
	public IDispositivo deshabilitar();
		
}
