package ru.ifmo.rain.gnatyuk.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> workers;
    private final Queue<Runnable> tasks;

    private static final int MAX_TASKS = Integer.MAX_VALUE - 1;

    /**
     * Thread-count constructor.
     * Creates a ParallelMapperImpl instance operating with maximum of {@code threads}
     * threads of type {@link Thread}.
     *
     * @param threads maximum count of operable threads
     */
    public ParallelMapperImpl(final int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Threads number must be positive");
        }
        tasks = new ArrayDeque<>();
        workers = new ArrayList<>();
        // :NOTE: Stream
        for (int i = 0; i < threads; i++) {
            // :NOTE: Одинаковые экземпляры
            workers.add(new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        runSynchronized();
                    }
                } catch (final InterruptedException ignored) {
                } finally {
                    Thread.currentThread().interrupt();
                }
            } ));
        }
        workers.forEach(Thread::start);
    }

    private void runSynchronized() throws InterruptedException {
        final Runnable task;
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }
            task = tasks.poll();
            tasks.notifyAll();
        }
        task.run();
    }

    private class CounterList<E> extends ArrayList<E> {
        int counter;

        CounterList(final List<E> list) {
            super(list);
            counter = 0;
        }

        int getCounter() {return counter;}

        synchronized void incCounter() {
            counter++;
            if (counter == this.size()) {
                notifyAll();
            }
        }
    }


    /**
     * Maps function {@code f} over specified {@code args}.
     * Mapping for each element performs in parallel.
     *
     * @throws InterruptedException if calling thread was interrupted
     */

    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args) throws InterruptedException {
        final CounterList<R> collector = new CounterList<>(Collections.nCopies(args.size(), null));
        final List<RuntimeException> runtimeExceptions = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            final int index = i;
            synchronized (tasks) {
                while (tasks.size() >= MAX_TASKS) {
                    tasks.wait();
                }
                tasks.add(() -> {
                    R val = null;
                    try {
                        val = f.apply(args.get(index));
                    } catch (final RuntimeException e) {
                        synchronized (runtimeExceptions) {
                            runtimeExceptions.add(e);
                        }
                    }
                    collector.set(index, val);
                    collector.incCounter();
                });
                tasks.notifyAll();
            }
        }
        if (!runtimeExceptions.isEmpty()) {
            final RuntimeException mapFail = new RuntimeException("Errors occured while mapping some of the values");
            runtimeExceptions.forEach(mapFail::addSuppressed);
            throw mapFail;
        }
        synchronized (collector) {
            while (collector.getCounter() < collector.size()) {
                collector.wait();
            }
        }
        return collector;
    }

    /** Stops all threads. All unfinished mappings leave in undefined state. */

    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
        IterativeParallelism.joinAllNoThrow(workers);
    }
}
