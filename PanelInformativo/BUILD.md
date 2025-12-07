PanelInformativo — Build & Run

Instrucciones rápidas para compilar y ejecutar PanelInformativo (incluye ejemplo AWS Shadow).

Requisitos:
- JDK instalado (javac/java en PATH)
- Certificados AWS colocados en `certs/` si vas a usar AWS (ver policies/ y documentación).

Bash (Linux/macOS/Git Bash/WSL):

    cd PanelInformativo
    ./compilar.sh
    ./scripts/ejecutar_panel_aws.sh

PowerShell (Windows):

    # Si tienes Git Bash en PATH, puedes ejecutar los scripts bash:
    bash ./PanelInformativo/compilar.sh
    bash ./PanelInformativo/scripts/ejecutar_panel_aws.sh

    # O compilar manualmente con javac (ejemplo básico):
    # javac -d PanelInformativo\bin -cp PanelInformativo\bin;PanelInformativo\lib\* (lista de archivos .java)

Notas:
- Ajusta los parámetros en `scripts/ejecutar_panel_aws.sh` según tu entorno.
- Antes de usar AWS, asegúrate de tener los certificados y el Thing configurado.
