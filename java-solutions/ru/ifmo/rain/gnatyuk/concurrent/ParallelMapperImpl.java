package ru.ifmo.rain.gnatyuk.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private List<Thread> workers;
    private final Queue<Runnable> tasks;

    private final int threads;

    private static final int MAX_TASKS = 1000000;

    /**
     * Thread-count constructor.
     * Creates a ParallelMapperImpl instance operating with maximum of {@code threads}
     * threads of type {@link Thread}.
     *
     * @param threads maximum count of operable threads
     */
    public ParallelMapperImpl(int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Threads number must be positive");
        }
        this.threads = threads;
        tasks = new ArrayDeque<>();
        workers = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            workers.add(new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        runSynchronized();
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    Thread.currentThread().interrupt();
                }
            } ));
        }
        workers.forEach(Thread::start);
    }

    private void runSynchronized() throws InterruptedException {
        Runnable task;
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

        CounterList(List<E> list) {
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
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        CounterList<R> collector = new CounterList<>(Collections.nCopies(args.size(), null));
        for (int i = 0; i < args.size(); i++) {
            final int index = i;
            synchronized (tasks) {
                while (tasks.size() >= MAX_TASKS) {
                    tasks.wait();
                }
                tasks.add(() -> {
                    collector.set(index, f.apply(args.get(index)));
                    collector.incCounter();
                });
                tasks.notifyAll();
            }
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
