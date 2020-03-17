package ru.ifmo.rain.gnatyuk.implementor;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Class helps in {@link Implementor} to write answers
 * @author imka
 * @version 1.0
 */
public class Packer {

    /**
     * merge {@code elements}, transformed by {@code transform}, concatenated by ", ".
     * @param elements array of args to merge
     * @param transform func, that transform {@code <T>} to {@link String}
     * @param <T> type of given items
     * @return {@link String} from elements, transformed by {@code transform}
     */
    static <T> String merge(T[] elements, Function<T, String> transform) {
        return Arrays.stream(elements).map(transform).collect(Collectors.joining(", "));
    }

    /**
     * function that returns parts, joined with separator.
     * @param separator {@link String} that will separate parts.
     * @param parts strings that will be joined.
     * @return {@link String} of all non-empty words in parts, separated by {@code separator}.
     */
    static String mergeWithSeparator(String separator, String... parts) {
        return Arrays.stream(parts).filter(s -> !"".equals(s)).collect(Collectors.joining(separator));
    }
}
