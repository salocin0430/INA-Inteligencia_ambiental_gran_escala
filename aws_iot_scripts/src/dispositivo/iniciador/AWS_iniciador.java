package dispositivo.iniciador;

import dispositivo.componentes.PanelInformativo_aws;
import dispositivo.componentes.SignalSpeed_aws;
import dispositivo.componentes.SmartRoad_aws;

public class AWS_iniciador {
    public static void main(String[] args) throws Exception {
        String brokerLocal = "tcp://tambori.dsic.upv.es:10083";
        String awsEndpoint = "a7sfhuya0h87y-ats.iot.us-east-1.amazonaws.com";

        // 2. Crear y configurar PanelInformativo
        PanelInformativo_aws panel = new PanelInformativo_aws(brokerLocal, "ttmi1", "R1s1", 500);
        panel.initAWS(
            awsEndpoint,
            "panel_informativo_R1s1",
            "certs/panel_R1s1-certificate.pem.crt",
            "certs/panel_R1s1-private.pem.key",
            "certs/AmazonRootCA1.pem"
        );
        panel.iniciar();

        // 3. Crear y configurar SmartRoad (servicio accidentes)
        SmartRoad_aws servicioAccidentes = new SmartRoad_aws("R1s1");
        servicioAccidentes.initAWS(
            awsEndpoint,
            "servicio_accidentes_R1s1",
            "certs/servicio_R1s1-certificate.pem.crt",
            "certs/servicio_R1s1-private.pem.key",
            "certs/AmazonRootCA1.pem"
        );

        // 4. Crear y configurar SignalSpeed
        SignalSpeed_aws signal = new SignalSpeed_aws("R1s1", "SL_R1s1_50", 50, 100, 500, brokerLocal);
        signal.initAWS(
            awsEndpoint,
            "senal_speedlimit_R1s1",
            "certs/senal_R1s1-certificate.pem.crt",
            "certs/senal_R1s1-private.pem.key",
            "certs/AmazonRootCA1.pem"
        );
        signal.publicarEstado();

        System.out.println("¡Todos los componentes arrancados y conectados!");

        // Mantener proceso vivo para escuchar mensajes MQTT y AWS
        Thread.currentThread().join();
    }
}

