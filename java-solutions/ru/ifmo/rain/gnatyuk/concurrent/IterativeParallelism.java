package ru.ifmo.rain.gnatyuk.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;

import java.util.*;
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
    // :NOTE: `> >` viva la C++!
    private <T> List<Stream<T> > split(final int threads, final List<T> values) {
        final List<Stream<T> > parts = new ArrayList<>();
        // :NOTE: Упростить
        final int pack = (values.size() % threads == 0) ? values.size() / threads : values.size() / threads + 1;
        // :NOTE: Длинный последний кусок
        for (int i = 0; i * pack < values.size(); i++) {
            parts.add(values.subList(i * pack, ((i + 1) * pack < values.size()) ? (i + 1) * pack : values.size()).stream());
        }
        return parts;
    }

    private <T, M, R> R twoFunc(final int threads, final List<T> values, final Function<Stream<T>, M> process1, final Function<Stream<M>, R> process2) throws InterruptedException {
        return process2.apply(firstFunc(threads, values, process1).stream());
    }

    private <T> T doubleFunc(final int threads, final List<T> values, final Function<Stream<T>, T> process1) throws InterruptedException {
        return process1.apply(firstFunc(threads, values, process1).stream());
    }

    // :NOTE: `process1` чудеса нейминга!
    private <T, M> List<M> firstFunc(final int threads, final List<T> values, final Function<Stream<T>, M> process1) throws InterruptedException {
        final List<Stream<T>> parts = split(threads, values);
        final List<Thread> workers = new ArrayList<>();
        final List<M> maximums = new ArrayList<>(Collections.nCopies(parts.size(), null));
        for (int i = 0; i < parts.size(); i++) {
            final int index = i;
            final Thread thread = new Thread(() -> maximums.set(index, process1.apply(parts.get(index))));
            workers.add(thread);
            thread.start();
        }
        for (final Iterator<Thread> i = workers.iterator(); i.hasNext(); ) {
            final Thread now = i.next();
            try {
                now.join();
            } catch (final InterruptedException e) {
                now.interrupt();
                final InterruptedException exception = new InterruptedException("Interrupted thread");
                exception.addSuppressed(e);
                for (; i.hasNext(); ) {
                    i.next().interrupt();
                }
                throw exception;
            }
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
    public String join(final int threads, final List<?> values) throws InterruptedException {
        final List<String> list = firstFunc(threads, values, (s -> s.map(Object::toString).collect(Collectors.joining())));
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
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
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
    // :NOTE: Метод получился, по сути, онопоточный, так как стримы создаются в параллельных потоках,
    // а вычисляются в одном
    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f) throws InterruptedException {
        final List<Stream<U> > list = firstFunc(threads, values, (s -> s.map(f)));
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
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return doubleFunc(threads, values, (s -> s.max(comparator).orElse(null)));
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
        final Function<Stream<T>, T> reducer = s -> s.reduce(monoid.getIdentity(), monoid.getOperator());
        return doubleFunc(threads, values, reducer);
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
        final Function<Stream<T>, R> reducer1 = s -> s.map(lift).reduce(monoid.getIdentity(), monoid.getOperator());
        final Function<Stream<R>, R> reducer2 = s -> s.reduce(monoid.getIdentity(), monoid.getOperator());
        return twoFunc(threads, values, reducer1, reducer2);
    }
}
