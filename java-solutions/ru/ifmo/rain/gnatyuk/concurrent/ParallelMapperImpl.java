package ru.ifmo.rain.gnatyuk.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class ParallelMapperImpl implements ParallelMapper {
    private List<Thread> workers;
    private final SynchronizedQueue<Runnable> tasks;

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
        tasks = new SynchronizedQueue<>(new ArrayDeque<>());
        final Runnable TASK = () -> {
            try {
                while (!Thread.interrupted()) {
                    final Runnable task;
                    task = tasks.poll();
                    task.run();
                }
            } catch (final InterruptedException ignored) {
            } finally {
                Thread.currentThread().interrupt();
            }
        };
        workers = IntStream.range(0, threads).mapToObj(x -> new Thread(TASK)).collect(Collectors.toList());
        workers.forEach(Thread::start);
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
    }

//    private class CounterList<E> extends ArrayList<E> {
//        int counter;
//
//        CounterList(List<E> list) {
//            super(list);
//            counter = 0;
//        }
//
//        int getCounter() {return counter;}
//
//        synchronized void incCounter() {
//            counter++;
//            if (counter == this.size()) {
//                notifyAll();
//            }
//        }
//    }

//    private class SynchronizedTasks<E> {
//        private List<E> tasks;
//        private int counter;
//
//        SynchronizedTasks(int size) {
//            tasks = new ArrayList<>(Collections.nCopies(size, null));
//            counter = 0;
//        }
//
//        synchronized void setTasks(final int pos, E el) {
//            tasks.set(pos, el);
//            counter++;
//            if (counter == tasks.size()) {
//                notifyAll();
//            }
//        }
//
//        synchronized List<E> getTasks() throws InterruptedException {
//            while (counter < tasks.size()) {
//                wait();
//            }
//            return tasks;
//        }
//    }

    private static class SynchronizedTasks<E, T> {
        private final List<E> tasks;
        private final List<T> errors;
        private int counter;

        SynchronizedTasks(int size) {
            this.tasks = new ArrayList<>(Collections.nCopies(size, null));
            this.errors = new ArrayList<>();
            this.counter = 0;
        }

        synchronized void setTasks(final int pos, E el) {
            synchronized (tasks) {
                tasks.set(pos, el);
            }
            counter++;
            if (counter == tasks.size()) {
                notify();
            }
        }

        synchronized void setError(final T e) {
            synchronized (errors) {
                errors.add(e);
            }
            counter++;
            if (counter == tasks.size()) {
                notify();
            }
        }

        private synchronized void waitCounter() throws InterruptedException {
            while (counter < tasks.size()) {
                wait();
            }
        }

        synchronized List<E> getTasks() throws InterruptedException {
            waitCounter();
            return tasks;
        }

        synchronized boolean hasErrors() throws InterruptedException {
            waitCounter();
            return !errors.isEmpty();
        }

        synchronized List<T> getErrors() throws InterruptedException {
            waitCounter();
            return errors;
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
        SynchronizedTasks<R, RuntimeException> collector = new SynchronizedTasks<>(args.size());
//        List<RuntimeException> runtimeExceptions = new ArrayList<>();
//        for (int i = 0; i < args.size(); i++) {
//            final int index = i;
//            synchronized (tasks) {
//                while (tasks.size() >= MAX_TASKS) {
//                    tasks.wait();
//                }
//                tasks.add(() -> {
//                    R val = null;
//                    try {
//                        val = f.apply(args.get(index));
//                    } catch (RuntimeException e) {
//                        synchronized (runtimeExceptions) {
//                            runtimeExceptions.add(e);
//                        }
//                    }
//                    collector.setTasks(index, val);
//                });
//                tasks.notifyAll();
//            }
//        }
        for (int i = 0; i < args.size(); i++) {
            final int index = i;
            tasks.add(() -> {
                try {
                    collector.setTasks(index, f.apply(args.get(index)));
                } catch (final RuntimeException e) {
                    collector.setError(e);
                }
            });
        }
        if (collector.hasErrors()) {
            final RuntimeException mapFail = new RuntimeException("Errors occured while mapping some of the values");
            collector.getErrors().forEach(mapFail::addSuppressed);
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
