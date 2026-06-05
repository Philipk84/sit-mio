package edu.icesi.mio.cco;

import Mio.Datagram;
import Mio.HistoricalRepositoryPrx;
import edu.icesi.mio.common.CsvDatagramParser;

import java.util.concurrent.TimeUnit;

final class DatagramQueueProcessor implements Runnable {
    private final DatagramQueue datagramQueue;
    private final ThreadPoolDispatcher dispatcher;
    private final PositionRegistry positionRegistry;
    private final HistoricalRepositoryPrx historicalRepository;
    private volatile boolean running;
    private Thread consumerThread;

    DatagramQueueProcessor(DatagramQueue datagramQueue, ThreadPoolDispatcher dispatcher,
                           PositionRegistry positionRegistry, HistoricalRepositoryPrx historicalRepository) {
        this.datagramQueue = datagramQueue;
        this.dispatcher = dispatcher;
        this.positionRegistry = positionRegistry;
        this.historicalRepository = historicalRepository;
    }

    void start() {
        running = true;
        consumerThread = new Thread(this, "datagram-queue-consumer");
        consumerThread.start();
    }

    void stop() {
        running = false;
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
        dispatcher.stop();
    }

    @Override
    public void run() {
        while (running) {
            try {
                Datagram datagram = datagramQueue.dequeue(1, TimeUnit.SECONDS);
                if (datagram != null) {
                    dispatcher.dispatch(() -> process(datagram));
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void process(Datagram datagram) {
        try {
            positionRegistry.update(CsvDatagramParser.toPosition(datagram));
            historicalRepository.storeDatagram(datagram);
        } catch (RuntimeException ex) {
            System.err.println("Error procesando datagrama: " + ex.getMessage());
        }
    }
}
