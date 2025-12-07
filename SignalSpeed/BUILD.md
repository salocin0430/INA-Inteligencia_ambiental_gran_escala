SignalSpeed — Build & Run

Instrucciones rápidas para compilar y ejecutar SignalSpeed (incluye ejemplo AWS Shadow).

Requisitos:
- JDK instalado (javac/java en PATH)
- Certificados AWS colocados en `certs/` si vas a usar AWS (ver `AWS_SHADOW_CONFIG.md`).

Bash (Linux/macOS/Git Bash/WSL):

    cd SignalSpeed
    ./compilar.sh
    ./scripts/ejecutar_signalspeed_aws.sh

PowerShell (Windows):

    # Si tienes Git Bash en PATH, puedes ejecutar los scripts bash:
    bash ./SignalSpeed/compilar.sh
    bash ./SignalSpeed/scripts/ejecutar_signalspeed_aws.sh

    # O compilar manualmente con javac (ejemplo básico):
    # Obtener lista de fuentes y compilar:
    # javac -d SignalSpeed\bin -cp SignalSpeed\bin;SignalSpeed\lib\* (lista de archivos .java)

Notas:
- Ajusta los parámetros en `scripts/ejecutar_signalspeed_aws.sh` según tu entorno.
- Antes de usar AWS, asegúrate de tener los certificados y el Thing configurado.
