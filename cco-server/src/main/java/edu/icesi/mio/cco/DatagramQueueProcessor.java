package edu.icesi.mio.cco;

import Mio.Datagram;
import Mio.HistoricalRepositoryPrx;
import edu.icesi.mio.common.CsvDatagramParser;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

final class DatagramQueueProcessor implements Runnable {
    private final BlockingQueue<Datagram> queue = new LinkedBlockingQueue<>(100_000);
    private final PositionRegistry positionRegistry;
    private final HistoricalRepositoryPrx historicalRepository;
    private final ExecutorService workers;
    private final int workerCount;
    private volatile boolean running;

    DatagramQueueProcessor(PositionRegistry positionRegistry, HistoricalRepositoryPrx historicalRepository,
                           int workerCount) {
        this.positionRegistry = positionRegistry;
        this.historicalRepository = historicalRepository;
        this.workerCount = Math.max(1, workerCount);
        this.workers = Executors.newFixedThreadPool(this.workerCount);
    }

    boolean offer(Datagram datagram) {
        return queue.offer(datagram);
    }

    void start() {
        running = true;
        for (int i = 0; i < workerCount; i++) {
            workers.submit(this);
        }
    }

    void stop() {
        running = false;
        workers.shutdownNow();
    }

    @Override
    public void run() {
        while (running) {
            try {
                Datagram datagram = queue.poll(1, TimeUnit.SECONDS);
                if (datagram != null) {
                    positionRegistry.update(CsvDatagramParser.toPosition(datagram));
                    historicalRepository.storeDatagram(datagram);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException ex) {
                System.err.println("Error procesando datagrama: " + ex.getMessage());
            }
        }
    }
}
