package ru.ifmo.rain.gnatyuk.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//todo

public class ParallelMapperImpl implements ParallelMapper {
    private List<Thread> workers;
    private final SynchronizedQueue<Runnable> tasks;

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
        tasks = new SynchronizedQueue<>(new ArrayDeque<>());
        final Runnable TASK = () -> {
            try {
                while (!Thread.interrupted()) {
                    runSynchronized();
                }
            } catch (final InterruptedException ignored) {
            } finally {
                Thread.currentThread().interrupt();
            }
        };
        workers = IntStream.range(0, threads).mapToObj(x -> new Thread(TASK)).collect(Collectors.toList());
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

    private class SynchronizedQueue<T> {
        private final Queue<T> data;

        SynchronizedQueue(Queue<T> data) {
            this.data = data;
        }

        void add(T value) {
            synchronized (data) {
                data.add(value);
                data.notify();
            }
        }

        T poll() throws InterruptedException {
            synchronized (data) {
                while (data.isEmpty()) {
                    data.wait();
                }

                return data.poll();
            }
        }

        boolean isEmpty() {
            return data.isEmpty();
        }

        int size() {
            return data.size();
        }
    }

    private class SynchronizedTasks<E> {
        private List<E> tasks;
        private int counter;

        SynchronizedTasks(int size) {
            tasks = new ArrayList<>(Collections.nCopies(size, null));
            counter = 0;
        }

        synchronized void setTasks(final int pos, E el) {
            tasks.set(pos, el);
            counter++;
            if (counter == tasks.size()) {
                notifyAll();
            }
        }

        synchronized List<E> getTasks() throws InterruptedException {
            while (counter < tasks.size()) {
                wait();
            }
            return tasks;
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
        SynchronizedTasks<R> collector = new SynchronizedTasks<>(args.size());
        List<RuntimeException> runtimeExceptions = new ArrayList<>();
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
                    } catch (RuntimeException e) {
                        synchronized (runtimeExceptions) {
                            runtimeExceptions.add(e);
                        }
                    }
                    collector.setTasks(index, val);
                });
                tasks.notifyAll();
            }
        }
        if (!runtimeExceptions.isEmpty()) {
            final RuntimeException mapFail = new RuntimeException("Errors occured while mapping some of the values");
            runtimeExceptions.forEach(mapFail::addSuppressed);
            throw mapFail;
        }
        return collector.getTasks();
    }

    /** Stops all threads. All unfinished mappings leave in undefined state. */

    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
        IterativeParallelism.joinAllNoThrow(workers);
    }
}
