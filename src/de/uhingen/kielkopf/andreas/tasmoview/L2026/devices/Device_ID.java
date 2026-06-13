/**
 *
 */
package de.uhingen.kielkopf.andreas.tasmoview.L2026.devices;

/**
 * Zeigt nur an, dass ein Objekt eines der Interfaces implementiert und über Notifier<Device_ID> weitergereicht werden darf
 *
 * @author Andreas Kielkopf
 */
public sealed interface Device_ID permits Device, ID {}
