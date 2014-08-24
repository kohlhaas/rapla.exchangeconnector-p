package org.rapla.plugin.exchangeconnector;

import org.rapla.components.util.TimeInterval;

public class SynchronizationStatus {
	public String username;
	public boolean enabled;
	public int unsynchronizedEvents;
	public int synchronizedEvents;
	public TimeInterval syncInterval;
}
