package com.cruise.ship_proxy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
public class RequestExecutorService {

    private final ExecutorService sequentialExecutor = Executors.newSingleThreadExecutor();

    public <T> Future<T> executeSequentially(Callable<T> task) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        log.info("Queueing request {} on thread {}", requestId, Thread.currentThread().getName());

        return sequentialExecutor.submit(() -> {
            log.info("Executing request {} on thread {}", requestId, Thread.currentThread().getName());
            T result = task.call();
            log.info("Completed request {}", requestId);
            return result;
        });
    }

    @PreDestroy
    public void cleanUp() {
        log.info("Shutting down request executor service");
        sequentialExecutor.shutdown();
    }
}
