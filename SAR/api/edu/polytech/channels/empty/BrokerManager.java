package edu.polytech.channels.empty;

import java.util.HashMap;
import java.util.Map;

public class BrokerManager {
	
	private static BrokerManager instance;
    private final Map<String, CBroker> brokers = new HashMap<>();
	
	BrokerManager() {
		instance = this;
	}
	
	public static BrokerManager self() {
        return instance;
    }
	
	public synchronized void add(CBroker broker) {
		brokers.put(broker.getName(), broker);
	}
  
	public synchronized void remove(CBroker broker) {
		brokers.remove(broker.getName());
	}
  
	public synchronized CBroker get(String name) {
		return brokers.get(name);
	}
  
}
