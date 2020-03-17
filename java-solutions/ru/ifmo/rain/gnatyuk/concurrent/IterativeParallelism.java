package ru.ifmo.gnatyuk.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link IterativeParallelism} uses treads, given by user and {@link List} of values to do some functions.
 * Implements interface ListIP.
 * @author gnatyuk
 * @version 1.0
 *
 */

public class IterativeParallelism implements AdvancedIP {
    private <T> List<Stream<T> > split(int threads, List<T> values) {
        List<Stream<T> > parts = new ArrayList<>();
        int pack = (values.size() % threads == 0) ? values.size() / threads : values.size() / threads + 1;
        for (int i = 0; i * pack < values.size(); i++) {
            parts.add(values.subList(i * pack, ((i + 1) * pack < values.size()) ? (i + 1) * pack : values.size()).stream());
        }
        return parts;
    }

    private <T, M, R> R twoFunc(int threads, List<T> values, Function<Stream<T>, M> process1, Function<Stream<M>, R> process2) throws InterruptedException {
        return process2.apply(firstFunc(threads, values, process1).stream());
    }

    private <T, M> List<M> firstFunc(int threads, List<T> values, Function<Stream<T>, M> process1) throws InterruptedException {
        List<Stream<T>> parts = split(threads, values);
        List<Thread> workers = new ArrayList<>();
        List<M> maximums = new ArrayList<>(Collections.nCopies(parts.size(), null));
        for (int i = 0; i < parts.size(); i++) {
            final int index = i;
            Thread thread = new Thread(() -> maximums.set(index, process1.apply(parts.get(index))));
            workers.add(thread);
            thread.start();
        }
        for (Thread t : workers) {
            t.join();
        }
        return maximums;
    }
    /**
     * Join values to string.
     *
     * @param threads number of concurrent threads.
     * @param values values to join.
     *
     * @return list of joined result of {@link #toString()} call on each value.
     *
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        List<String> list = firstFunc(threads, values, (s -> s.map(Object::toString).collect(Collectors.joining())));
        return list.stream().map(Object::toString).collect(Collectors.joining());
    }

    /**
     * Filters values by predicate.
     *
     * @param threads number of concurrent threads.
     * @param values values to filter.
     * @param predicate filter predicate.
     *
     * @return list of values satisfying given predicated. Order of values is preserved.
     *
     * @throws InterruptedException if executing thread was interrupted.
     */

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return twoFunc(threads, values, s -> s.filter(predicate), s -> s.flatMap(Function.identity()).collect(Collectors.toList()));
    }

    /**
     * Maps values.
     *
     * @param threads number of concurrent threads.
     * @param values values to filter.
     * @param f mapper function.
     *
     * @return list of values mapped by given function.
     *
     * @throws InterruptedException if executing thread was interrupted.
     */

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        List<Stream<U> > list = firstFunc(threads, values, (s -> s.map(f)));
        return list.stream().flatMap(Function.identity()).collect(Collectors.toList());
    }

    /**
     * Returns maximum value.
     *
     * @param threads number or concurrent threads.
     * @param values values to get maximum of.
     * @param comparator value comparator.
     * @param <T> value type.
     *
     * @return maximum of given values
     *
     * @throws InterruptedException if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if not values are given.
     */

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return twoFunc(threads, values, (s -> s.max(comparator).orElse(null)), (s -> s.max(comparator).orElse(null)));
    }

    /**
     * Returns minimum value.
     *
     * @param threads number or concurrent threads.
     * @param values values to get minimum of.
     * @param comparator value comparator.
     * @param <T> value type.
     *
     * @return minimum of given values
     *
     * @throws InterruptedException if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if not values are given.
     */

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return twoFunc(threads, values, (s -> s.min(comparator).orElse(null)), (s -> s.min(comparator).orElse(null)));
    }

    /**
     * Returns whether all values satisfies predicate.
     *
     * @param threads number or concurrent threads.
     * @param values values to test.
     * @param predicate test predicate.
     * @param <T> value type.
     *
     * @return whether all values satisfies predicate or {@code true}, if no values are given.
     *
     * @throws InterruptedException if executing thread was interrupted.
     */

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return twoFunc(threads, values, (s -> s.allMatch(predicate)), (s -> s.allMatch(Boolean::booleanValue)));
    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param threads number or concurrent threads.
     * @param values values to test.
     * @param predicate test predicate.
     * @param <T> value type.
     *
     * @return whether any value satisfies predicate or {@code false}, if no values are given.
     *
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return twoFunc(threads, values, (s -> s.anyMatch(predicate)), (s -> s.anyMatch(Boolean::booleanValue)));
    }

    /**
     * Reduces values using monoid.
     *
     * @param threads number of concurrent threads.
     * @param values values to reduce.
     * @param monoid monoid to use.
     *
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if not values specified.
     *
     * @throws InterruptedException if executing thread was interrupted.
     */

    @Override
    public <T> T reduce(int threads, List<T> values, Monoid<T> monoid) throws InterruptedException {
        Function<Stream<T>, T> reducer = s -> s.reduce(monoid.getIdentity(), monoid.getOperator());
        return reducer.apply(firstFunc(threads, values, reducer).stream());
    }

    /**
     * Maps and reduces values using monoid.
     *
     * @param threads number of concurrent threads.
     * @param values values to reduce.
     * @param lift mapping function.
     * @param monoid monoid to use.
     *
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if not values specified.
     *
     * @throws InterruptedException if executing thread was interrupted.
     */

    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, Monoid<R> monoid) throws InterruptedException {
        Function<Stream<T>, R> reducer1 = s -> s.map(lift).reduce(monoid.getIdentity(), monoid.getOperator());
        Function<Stream<R>, R> reducer2 = s -> s.reduce(monoid.getIdentity(), monoid.getOperator());
        List<R> ff = firstFunc(threads, values, reducer1);
        return reducer2.apply(ff.stream());
    }
}
