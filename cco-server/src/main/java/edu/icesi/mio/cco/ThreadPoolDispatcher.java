package edu.icesi.mio.cco;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class ThreadPoolDispatcher {
    private final ExecutorService executorService;

    ThreadPoolDispatcher(int workerCount) {
        this.executorService = Executors.newFixedThreadPool(Math.max(1, workerCount));
    }

    void dispatch(Runnable task) {
        executorService.submit(task);
    }

    void stop() {
        executorService.shutdownNow();
    }
}
