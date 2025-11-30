package dispositivo.interfaces;

public interface IFuncion {
	
	public String getId();
	
	public IFuncion iniciar();
	public IFuncion detener();
	
	public IFuncion encender();
	public IFuncion apagar();
	public IFuncion parpadear();
	
	public FuncionStatus getStatus();
	
	// Ejercicio 4: Capacidad habilitar/deshabilitar función
	public boolean isHabilitada();
	public IFuncion habilitar();
	public IFuncion deshabilitar();

}
