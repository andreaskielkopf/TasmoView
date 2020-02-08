
# TasmoView 0.3 Alpha

Tool to search [Tasmota-Devices](https://tasmota.github.io/docs/#/Home) in the local Network via HTTP

Since I use some Tasmota devices, I often had to search for the corresponding IP in my network. Then I found TasmoManager. This works very well, but I had to set up an MQTT server and configure each device to use mqtt ...

To make it easier for me (and my wife), I wrote a small program in my preferred programming language java.

This may also be useful for others, so I will publish this little program here.

To date, the program has only been tested in my local network and with Linux (manjaro) as the operating system. But it should run anywhere if you have at least **Java11 installed**. (Testers with a different configuration are welcome)

The program is provided as a JAR file with the source code in it. This means that all (!) Source files are contained in this one jar file. Nevertheless, the JAR file can be executed directly. (**java -jar TasmoView.jar**)

# 0.1
* The program actively searches for Tasmota devices in the local network by addressing them via http [Scan]
* The scan can be repeated if a device is newly inserted
* "admin" is preset as username [user]
* If the devices are secured with a password, the password can be entered [password]
* Password and username are then used for the scan and refresh of all devices
* All devices found are displayed in a table
* The status of the devices is queried and shown in a series of table views
* The status of all devices can be updated [Refresh]
* If a device is marked in the table, the URL of this device can be sent to the browser at the push of a button [Browser], so that the browser displays the HTML page of the device
* The device can then be controlled and configured in the browser
* Every Tasmota device that can be reached via the browser is also found

![Screenshot](./info/Browser.png)
----

# 0.2
- [x] save Tasmota-username and password in the local java registry (on request)
- [x] tab for the observation of sensors with regular query of the sensor status
- [x] Adjustable query rate for all sensors together
- [x] Selection list for displaying the sensors of the following types
- [x] AM2301 temperature red
- [x] AM2301 humidity blue
- [x] graphic diagram with the continuous temporal course of the sensor data
- [x] horizontal lines in the diagram to better assess the values
- [x] scales for the different types of measured values
- [x] Crossfading of the measured value types with their own factor
- [x] Automatic deletion of "unnecessary" measured values
- [x] Automatic reduction of the data to 1000 measurements per sensor

![Screenshot](./info/SensorData.png)
----


Da ich einige [Tasmota-Geräte](https://tasmota.github.io/docs/#/Home) benutze, musste ich oft nach der entsprechenden IP in meinem Netzwerk suchen. Dann habe ich TasmoManager gefunden. Das funktioniert sehr gut, aber ich musste einen MQTT-Server einrichten und jedes Gerät für die Verwendung von mqtt konfigurieren ...

Um es mir (und meiner Frau) einfacher zu machen, habe ich ein kleines Programm in meiner bevorzugten Programmiersprache java geschrieben. Dies kann eventuell auch für andere nützlich sein, so dass ich dieses kleine Programm hier veröffentliche.

Das Programm ist bis heute nur in meinem lokalen Netzwerk und mit Linux (manjaro) als Betriebssystem getestet. Aber es sollte überall laufen wenn Sie zumindest **Java11 installiert** haben. (Tester mit anderer Konfiguration sind willkommen)

Das Programm wird als JAR-Datei mit enthaltenen Quelltexten zur Verfügung gestellt. Dies bedeutet, dass alle (!) Quelldateien in dieser einen Jar-Datei enthalten sind. Trotzdem ist die JAR-Datei direkt ausführbar. (**java -jar TasmoView.jar**)

# 0.1
* Das Programm sucht im lokalen Netzwerk aktiv nach Tasmota-Geräten indem es diese per http anspricht [Scan]
* Der Scan kann wiederholt werden, wenn ein Gerät neu eingesteckt wurde
* Als username ist "admin" voreingestellt [user]
* Wenn die Geräte mit Passwort gesichert sind, kann das Passwort eingegeben werden [password]
* Passwort und Username werden dann für den Scan und den Refresh aller Geräte genutzt
* Alle gefundenen Geräte werden in einer Tabelle angezeigt 
* Der Status der Geräte wird abgefragt und in einer Reihe von Tabellenansichten dargestellt
* Der Status aller Geräte kann aktualisiert werden [Refresh]
* Wird ein Gerät in der Tabelle markiert, so kann mit einem Knopfdruck [Browser] die URL dieses Gerätes an den Browser geschickt werden, so dass der Browser die HTML-Seite des Gerätes darstellt
* Im Browser kann das Gerät dann gesteuert und konfiguriert werden
* Jedes Tasmota-Gerät das über den Browser erreichbar ist wird auch gefunden

![Screenshot](./info/Browser.png)
----
# 0.2
- [x] Speichern des Tasmota-username und password in der lokalen java-Registry (auf Wunsch)
- [x] Reiter für die Beobachtung von Sensoren mit regelmässiger Abfrage des Sensorstatus
- [x] Einstellbare Abfragerate für alle Sensoren gemeinsam
- [x] Auswahlliste zur Anzeige der Sensoren folgender Typen
- [x] AM2301 Temperatur rot
- [x] AM2301 Luftfeuchte blau
- [x] grafisches Diagramm mit dem fortlaufenden zeitlichen Verlauf der Sensordaten
- [x] waagerechte Linien im Diagramm um die Werte besser beurteilen zu können
- [x] Skalen für die verschiedenen Typen der Messwerte
- [x] Überblenden der Messwerttypen mit eigenem Faktor
- [x] Automatisches löschen von "unnötigen" Messwerten
- [x] Automatisches reduzieren der Daten auf 1000 Messungen je Sensor

![Screenshot](./info/SensorData.png)
----
# 0.3
- [ ] Gestrichelte Linien für das Raster
- [x] Erweitern auf 10000 Messungen
- [ ] Werte beim erfassen mit dem richtigen Zeitpunkt übernehmen
- [ ] Doppelte Werte bereits beim Erfassen droppen
- [ ] senkrechte Linien im Diagramm um den zeitlichen Verlauf einordnen zu können
- [ ] speichern der MAC und IP der gefundenen Tasmotas ???
- [ ] Jeder Sensor bekommt einen Spitznamen (speichern)
- [ ] Die Farbe der Messskalen werden einstellbar
- [ ] Die Farben der Sensoren werden einstellbar
- [ ] weitere Sensortypen :thinking:
- [ ] 
- [ ] speichern der komprimierten Messungen (auf Wunsch)
- [ ] laden von früheren Messungen
- [ ] Einstellbare Zeitskala
- [ ] Automatik für die Abfragerate bei schnellen Änderungen je Gerät
- [ ] Vergleich historischer Messungen
- [ ] Täglich selbe Zeit
- [ ] Wöchentlich
- [ ] Mondphasen
- [ ] jährlich
- [ ] Täglich nach Sonnenaufgang/Sonnenuntergang
- [ ] X/Y-Graph mit 2 Sensoren Temperatur/Feuchte
- [ ] Graph aus Formel
- [ ] 
- [ ] 
- [ ] 
- [ ] 
- [ ] 
- [ ] 
- [ ] 
- [ ] ihr Vorschlag kann als Pull-Request hier eingefügt werden
- [ ] 
- [ ] 
- [ ] 
- [ ] 
- [ ] 
- [ ] 
- [ ] 
- [ ] 
- [ ] 
- [ ] 
- [ ] 

----

Get JAVA13:
* [OpenJDK 13](https://jdk.java.net/13/) https://jdk.java.net/13/
* [OpenJDK 11](https://jdk.java.net/archive/) https://jdk.java.net/archive/
* [Oracle java SE 13](https://www.oracle.com/technetwork/java/javase/downloads/index.html) https://www.oracle.com/technetwork/java/javase/downloads/index.html
