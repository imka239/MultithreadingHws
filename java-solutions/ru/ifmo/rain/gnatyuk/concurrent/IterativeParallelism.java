package ru.ifmo.rain.gnatyuk.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
/**
 * {@link IterativeParallelism} uses treads, given by user and {@link List} of values to do some functions.
 * Implements interface {@link AdvancedIP}.
 * @author gnatyuk
 * @version 1.0
 *
 */

public class IterativeParallelism implements AdvancedIP {

    private final ParallelMapper mapper;

    /**
     * Constructor if {@link ParallelMapper} given. Makes mapper for {@link #oneFunc(int, List, Function)}.
     * @param mapper {@link ParallelMapper} class, that will do all work with threads
     */

    public IterativeParallelism(final ParallelMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Default constructors for {@link IterativeParallelism}.
     * {@link #mapper} is null
     */
    public IterativeParallelism() {
        this.mapper = null;
    }

    private static <T> List<Stream<T>> split(final int threads, final List<T> values) {
        final List<Stream<T>> parts = new ArrayList<>();
        final int pack = values.size() / threads;
        int r = values.size() % threads;

        int start = 0;
        while (start < values.size()) {
            final int end = pack + (r > 0 ? 1 : 0);
            parts.add(values.subList(start, start + end).stream());
            r--;
            start += end;
        }
        return parts;
    }

    private static void joinAll(final List<Thread> workers) throws InterruptedException {
        for (int i = 0; i < workers.size(); i++) {
            try {
                workers.get(i).join();
            } catch (final InterruptedException e) {
                final InterruptedException exception = new InterruptedException("Some threads were interrupted");
                exception.addSuppressed(e);
                for (int j = i; j < workers.size(); j++) {
                    workers.get(j).interrupt();
                }
                for (int j = i; j < workers.size(); j++) {
                    try {
                        workers.get(j).join();
                    } catch (final InterruptedException er) {
                        exception.addSuppressed(er);
                        j--;
                    }
                }
                throw exception;
            }
        }
    }

    static void joinAllNoThrow(final List<Thread> workers) {
        workers.forEach(thread -> {
            try {
                thread.join();
            } catch (final InterruptedException ignored) {
            }
        });
    }

    private <T, M, R> R twoFunc(final int threads, final List<T> values, final Function<Stream<T>, M> process1, final Function<Stream<M>, R> process2) throws InterruptedException {
        return process2.apply(oneFunc(threads, values, process1).stream());
    }

    private <T> T doubleFunc(final int threads, final List<T> values, final Function<Stream<T>, T> func) throws InterruptedException {
        return func.apply(oneFunc(threads, values, func).stream());
    }

    private <T, M> List<M> oneFunc(final int threads, final List<T> values, final Function<Stream<T>, M> func) throws InterruptedException {
        final List<Stream<T>> parts = split(threads, values);
        final List<M> counted;
        if (mapper == null) {
            final List<Thread> workers = new ArrayList<>();
            counted = new ArrayList<>(Collections.nCopies(parts.size(), null));
            for (int i = 0; i < parts.size(); i++) {
                final int index = i;
                final Thread thread = new Thread(() -> counted.set(index, func.apply(parts.get(index))));
                workers.add(thread);
                thread.start();
            }
            joinAll(workers);
        } else {
            counted = mapper.map(func, parts);
        }
        return counted;
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
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return twoFunc(threads, values,
                s -> s.map(Object::toString).collect(Collectors.joining()),
                s -> s.collect(Collectors.joining()));
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
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return twoFunc(threads, values, s -> s.filter(predicate).collect(Collectors.toList()), IterativeParallelism::merge);
    }

    private static <T> List<T> merge(final Stream<? extends List<? extends T>> streams) {
        return streams.flatMap(List::stream).collect(Collectors.toList());
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
    // :NOTE: Метод получился, по сути, онопоточный, так как стримы создаются в параллельных потоках,
    // а вычисляются в одном
    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f) throws InterruptedException {
        return twoFunc(threads, values, s -> s.map(f).collect(Collectors.toList()), IterativeParallelism::merge);
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
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return minimum(threads, values, comparator.reversed());
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
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return doubleFunc(threads, values, (s -> s.min(comparator).orElse(null)));
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
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
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
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
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
    public <T> T reduce(final int threads, final List<T> values, final Monoid<T> monoid) throws InterruptedException {
        return doubleFunc(threads, values, s -> getMapReduce(s, monoid, Function.identity()));
    }

    private static <T, R> R getMapReduce(final Stream<T> stream, final Monoid<R> monoid, final Function<T, R> lift) {
        return stream.map(lift).reduce(monoid.getIdentity(), monoid.getOperator());
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
    public <T, R> R mapReduce(final int threads, final List<T> values, final Function<T, R> lift, final Monoid<R> monoid) throws InterruptedException {
        // :NOTE: копипаста
        return twoFunc(threads, values, s -> getMapReduce(s, monoid, lift), s -> getMapReduce(s, monoid, Function.identity()));
    }
}
