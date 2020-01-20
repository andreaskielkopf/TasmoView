# TasmoView
Tool to search Tasmota-Devices in the local Network via HTTP

Since I use some Tasmota devices, I often had to search for the corresponding IP in my network. Then I found TasmoManager. This works very well, but I had to set up an MQTT server and configure each device to use mqtt ...

To make it easier for me (and my wife), I wrote a small program in my preferred programming language java.

This may also be useful for others, so I will publish this little program here.

To date, the program has only been tested in my local network and with Linux (manjaro) as the operating system. But it should run anywhere if you have Java installed. (Testers with a different configuration are welcome)

The program is provided as a JAR file with the source code in it. This means that all (!) Source files are contained in this one jar file. Nevertheless, the JAR file can be executed directly. (java -jar TasmoView.jar)

----

Da ich einige Tasmota-Geräte benutze, musste ich oft nach der entsprechenden IP in meinem Netzwerk suchen. Dann habe ich TasmoManager gefunden. Das funktioniert sehr gut, aber ich musste einen MQTT-Server einrichten und jedes Gerät für die Verwendung von mqtt konfigurieren ...

Um es mir (und meiner Frau) einfacher zu machen, habe ich ein kleines Programm in meiner bevorzugten Programmiersprache java geschrieben. Dies kann eventuell auch für andere nützlich sein, so dass ich dieses kleine Programm hier veröffentliche.

Das Programm ist bis heute nur in meinem lokalen Netzwerk und mit Linux (manjaro) als Betriebssystem getestet. Aber es sollte überall laufen wenn Sie Java installiert haben. (Tester mit anderer Kopnfiguration sind willkommen)

Das Programm wird als JAR-Datei mit enthaltenen Quelltexten zur Verfügung gestellt. Dies bedeutet, dass alle (!) Quelldateien in dieser einen Jar-Datei enthalten sind. Trotzdem ist die JAR-Datei direkt ausführbar. (java -jar TasmoView.jar)
