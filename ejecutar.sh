#!/bin/bash
echo "Lanzando el dispositivo IoT para ttmi060..."
#sudo java -jar dispositivo-pi4.jar ttmi060 ttmi060.iot.upv.es 8182 tcp://tambori.dsic.upv.es:1883
sudo java -jar dispositivo-pi4.jar $(hostname) $(hostname).iot.upv.es 8182 tcp://tambori.dsic.upv.es:1883