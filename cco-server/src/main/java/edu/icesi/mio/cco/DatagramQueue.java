package edu.icesi.mio.cco;

import Mio.Datagram;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

final class DatagramQueue {
    private final ArrayBlockingQueue<Datagram> queue;

    DatagramQueue(int capacity) {
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    boolean enqueue(Datagram datagram) {
        return queue.offer(datagram);
    }

    Datagram dequeue(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    int size() {
        return queue.size();
    }

    boolean isFull() {
        return queue.remainingCapacity() == 0;
    }
}
