package edu.polytech.channels.empty;

import java.util.HashMap;
import java.util.Map;

import edu.polytech.channels.Broker;
import edu.polytech.channels.Channel;

public class CBroker implements Broker {
	private final String name;
    private final BrokerManager manager;
    private final Map<Integer, RDV> rdvs = new HashMap<>();

    CBroker(String name) {
    	this.name = name;
        this.manager = BrokerManager.self();
        manager.add(this);
    }

    @Override
    public String getName() {
    	return name;
    }
    
    private synchronized RDV rdv(int port) {
        return rdvs.computeIfAbsent(port, p -> new RDV());
    }

    @Override
    public Channel connect(String name, int port) {
    	CBroker remote = manager.get(name);

        if (remote == null)
            return null;

        try {
            return remote.rdv(port).connect(remote, port);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public Channel accept(int port) {
    	try {
            return rdv(port).accept(this, port);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

}
