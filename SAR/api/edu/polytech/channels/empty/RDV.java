package edu.polytech.channels.empty;

import java.util.LinkedList;
import java.util.Queue;

public class RDV {

    private boolean accepting = false;
    private final Queue<PendingConnect> connects = new LinkedList<>();

    private static class PendingConnect {
        CChannel channel;
        boolean matched = false;
    }

    public synchronized CChannel connect(CBroker broker, int port)
            throws InterruptedException {

        PendingConnect pc = new PendingConnect();
        pc.channel = new CChannel(broker, port);

        connects.add(pc);
        notifyAll();

        while (!pc.matched)
            wait();

        return pc.channel;
    }

    public synchronized CChannel accept(CBroker broker, int port)
            throws InterruptedException {

        if (accepting)
            throw new IllegalStateException("accept déjà en attente");

        accepting = true;

        while (connects.isEmpty())
            wait();

        PendingConnect pc = connects.poll();

        CChannel local = new CChannel(pc.channel);

        pc.channel.setPeer(local);
        local.setPeer(pc.channel);

        pc.matched = true;
        accepting = false;

        notifyAll();

        return local;
    }
}
