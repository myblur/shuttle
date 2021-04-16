package me.iblur.shuttle.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @since 2021-04-15 14:50
 */
public class NamedThreadFactory implements ThreadFactory {

    private final AtomicInteger threadCounter = new AtomicInteger();

    private final String name;

    public NamedThreadFactory(String name) {
        this.name = name;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, name + "-" + threadCounter.incrementAndGet());
    }
}
