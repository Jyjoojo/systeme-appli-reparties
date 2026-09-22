package edu.polytech.channels.empty;

import edu.polytech.channels.Channel;
import edu.polytech.utils.CircularBuffer;

public class CChannel implements Channel {
	private static final int CAPACITY = 1024;

    private CircularBuffer in;
    private CircularBuffer out;

    private volatile boolean localDisconnected = false;
    private volatile boolean remoteDisconnected = false;

    private CChannel peer;
    

    protected CChannel(CBroker broker, int port) {
    	in = new CircularBuffer(CAPACITY);
        out = new CircularBuffer(CAPACITY);
    }
    
    protected CChannel(CChannel c) {
        this.in = c.out;
        this.out = c.in;
    }

    protected void setPeer(CChannel p) {
        peer = p;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) {
    	if (offset < 0 || length < 0 || offset + length > bytes.length)
            throw new IllegalArgumentException();

        synchronized (in) {

            while (in.empty() && !remoteDisconnected) {
                try {
                    in.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return 0;
                }
            }

            if (in.empty() && remoteDisconnected)
                return 0;

            int n = 0;

            while (n < length && !in.empty()) {
                bytes[offset + n] = in.pull();
                n++;
            }

            in.notifyAll();

            return n;
        }
    }

    @Override
    public int write(byte[] bytes, int offset, int length) {
    	if (offset < 0 || length < 0 || offset + length > bytes.length)
            throw new IllegalArgumentException();

        if (localDisconnected)
            return length;

        synchronized (out) {

            while (out.full() && !localDisconnected) {
                try {
                    out.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return length;
                }
            }

            if (localDisconnected)
                return length;

            int n = 0;

            while (n < length && !out.full()) {
                out.push(bytes[offset + n]);
                n++;
            }

            out.notifyAll();

            return n;
        }
    }

    @Override
    public boolean disconnected() {
    	return localDisconnected && remoteDisconnected && in.empty();
    }

    @Override
    public void disconnect() {
    	 if (localDisconnected)
             return;

         localDisconnected = true;

         synchronized (out) {
             out.notifyAll();
         }

         if (peer != null) {

             peer.remoteDisconnected = true;

             synchronized (peer.in) {
                 peer.in.notifyAll();
             }
         }
    }

}
