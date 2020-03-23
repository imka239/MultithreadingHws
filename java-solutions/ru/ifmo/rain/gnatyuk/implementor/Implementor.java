package ru.ifmo.rain.gnatyuk.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Class implements {@link Impler} and {@link JarImpler}.
 * Gets  or {@code .jar} and implements class.
 * @author imka
 * @version 1.0
 */

public class Implementor implements Impler {

    /**
     * Main. Gets args from console for {@link Implementor}
     * 2-args {@code className outPath} creates file
     * {@code .java} file in {@code outPath} by {@link #implement(Class, Path)}
     * @param args arguments for application
     */

    public static void main(String[] args) {
        //new Implementor().implement(Child.class, Path.of("C:\\Users\\ASUS\\IdeaProjects\\javaAdvanced"));
        if (args == null || args.length != 2) {
            System.err.println("Expected 2 args: classname, outPath");
            return;
        }
        for (String arg : args) {
            if (arg == null) {
                System.out.println(Arrays.toString(args) + "is null");
            }
        }
        try {
            new Implementor().implement(Class.forName(args[0]), Path.of(args[1]));
        } catch (ClassNotFoundException e) {
            System.err.println("Class not Found" + e.getMessage());
        } catch (ImplerException e) {
            System.err.println("Failed to Implement" + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("Failed to make Path" + e.getMessage());
        }
    }

    /**
     * return default value for {@link #getMethod(Method)}
     * @param ret method for default value.
     * @return {@link String} representing default value of {@code ret}
     */

    private String getDefaultValue(Class<?> ret) {
        if (!ret.isPrimitive()) {
            return "null";
        } else if (ret.equals(void.class)) {
            return "";
        } else if (ret.equals(boolean.class)) {
            return "false";
        } else {
            return "0";
        }
    }

    /**
     * returns {@link String} of Exceptions, if they aren't null.
     * @param executable an instance of {@link Executable} (method or constructor)
     * @return "", if there are no {@link Exception} in {@code executable}, "throws" + list of exceptions, separated by ", " otherwise.
     */

    private String getExecutableExceptions(Executable executable) {
        return getIfNotEmpty(Packer.merge(executable.getExceptionTypes(), Class::getCanonicalName));
    }

    /**
     * Joins <code>prefix</code> and <code>itemList</code>, with separator <code>System.lineSeparator</code> if itemList isn't empty.
     * @param itemList string, that will merge prefix, if its't empty.
     * @return "" if <code>itemList</code> isn't empty, otherwise concatenation of {@code prefix} and {@code item} with <code>System.lineSeparator</code>
     */
    private static String getIfNotEmpty(String itemList) {
        if (!itemList.isEmpty()) {
            return Packer.mergeWithSeparator(System.lineSeparator(), "throws", itemList);
        }
        return "";
    }

    /**
     * return all Modifiers of class {@code token}
     * {@link Modifier#ABSTRACT}, {@link Modifier#INTERFACE}, {@link Modifier#STATIC}, {@link Modifier#PROTECTED} excluded
     * @param token instance of {@link Class}
     * @return {@link String} of token's Modifiers.
     */

    private String getClassModifiers(Class<?> token) {
        return Modifier.toString(token.getModifiers() & ~Modifier.INTERFACE & ~Modifier.ABSTRACT & ~Modifier.STATIC & ~Modifier.PROTECTED);
    }

    /**
     * return all Modifiers of class {@code executable}
     * {@link Modifier#ABSTRACT}, {@link Modifier#NATIVE}, {@link Modifier#TRANSIENT} excluded
     * @param executable instance of {@link Executable}
     * @return {@link String} of token's Modifiers.
     */
    private String getExecutableModifiers(Executable executable) {
        return Modifier.toString(executable.getModifiers() & ~Modifier.NATIVE & ~Modifier.TRANSIENT & ~Modifier.ABSTRACT);
    }

    /**
     * return a list of types and names using {@link Packer#merge}
     * @param executable method or constructor, instnace of {@link Executable}
     * @return {@link String} from list of types from {@code executable}
     */


    private String getExecutableArguments(Executable executable) {
        Class<?>[] elems = executable.getParameterTypes();
        String[] str = new String[elems.length];
        IntStream.range(0, elems.length).forEachOrdered(i -> str[i] = elems[i].getCanonicalName() + " _" + i);
        return "(" + String.join(", ",str) + ")";
    }

    /**
     * return a list of types and names using {@link Packer#merge}
     * @param executable method or constructor, instnace of {@link Executable}
     * @return {@link String} from list of types from {@code executable}
     */

    private String getExecutableArgumentsNames(Executable executable) {
        Class<?>[] elems = executable.getParameterTypes();
        String[] str = new String[elems.length];
        IntStream.range(0, elems.length).forEachOrdered(i -> str[i] = "_" + i);
        return "(" + String.join(", ", str) + ")";
    }

    /**
     * return default method body using {@link #getDefaultValue(Class)}.
     * @param method {@link Method} from where {@link #getDefaultValue(Class)} get its value.
     * @return {@link String} "return" + {@link #getDefaultValue(Class)} + ";" default method body
     */

    private String getMethodBody(Method method) {
        return Packer.mergeWithSeparator(" ", "return", getDefaultValue(method.getReturnType())) + ";";
    }

    /**
     * builder of Method. it combine {@link #getExecutableModifiers(Executable)} {@link Method#getReturnType()}
     * {code name} and {@link #getExecutableExceptions(Executable)} {@link #getMethodBody(Method)}
     * @param method {@link Method} method, that program will write.
     * @return {@link String} implemented method {@link Method}
     */

    private String getMethod(Method method) {
        return Packer.mergeWithSeparator(" ", getExecutableModifiers(method), method.getReturnType().getCanonicalName(), method.getName() + getExecutableArguments(method),
                getExecutableExceptions(method), Packer.mergeWithSeparator(System.lineSeparator(), "{", getMethodBody(method) + '}'));
    }

    /**
     * Class that helps to hash all {@link Method} to forbid reassignment
     * it contain {@link Method}
     */

    private static class Hasher {
        /**
         * The wrapped {@link Method} object.
         */
        private final Method method;

        /**
         * Prime multiplier used in hashing.
         */

        private final int PRIME = 31;

        /**
         * Prime base module used in hashing.
         */

        private final int BASE = 1000000007;

        /**
         * Constructor, wrapping {@link Method}
         * @param method instance of {@link Method} class to be wrapped inside
         */
        Hasher(Method method) {
            this.method = method;
        }

        /**
         * Getter for wrapped{@link Method}.
         *
         * @return wrapped {@link #method}
         */
        Method get() {
            return method;
        }

        /**
         * Hash code calculator (polynomial).
         * It's using name, parameter types and return type.
         *
         * @return integer hash code value
         */

        @Override
        public int hashCode() {
            return ((method.getName().hashCode() +
                    PRIME * Arrays.hashCode(method.getParameterTypes())) % BASE +
                    (PRIME * PRIME) % BASE * method.getReturnType().hashCode()) % BASE;
        }

        /**
         * Checker for equals for to objects
         * @param obj second object
         * @return {@code true} if objects are equal, {@code false} otherwise
         */

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Hasher) {
                Hasher hm = (Hasher) obj;
                return method.getName().equals(hm.method.getName()) &&
                        Arrays.equals(method.getParameterTypes(), hm.method.getParameterTypes()) &&
                        method.getReturnType().equals(hm.method.getReturnType());
            }
            return false;
        }
    }

    /**
     * function that will return string of methods with deffault implementation {@link #getMethod(Method)}
     * @param token instance of {@link Class}
     * @return {@link String} all implemented from abstract non private methods.
     * @throws ImplerException if token is {@link Modifier#isPrivate(int)}
     */

    private String getMethods(Class<?> token) throws ImplerException {
        Set<Hasher> methods = new HashSet<>();
        Arrays.stream(token.getMethods()).map(Hasher::new).forEach(methods::add);
        if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Can't override private class");
        }
        while (token != null) {
            Arrays.stream(token.getDeclaredMethods()).map(Hasher::new).forEach(methods::add);
            token = token.getSuperclass();
        }
        return methods.stream().filter(a -> Modifier.isAbstract(a.get().getModifiers()))
                .map(a -> getMethod(a.get())).collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * return default method body using {@link #getExecutableArgumentsNames(Executable)} )}.
     * @param constructor {@link Constructor} from where {@link #getExecutableArgumentsNames(Executable)} get its value.
     * @return {@link String} "super" + {@link #getExecutableArgumentsNames(Executable)} + ";" default constructor's body
     */

    private String getConstructorBody(Constructor<?> constructor) {
        return "super" + getExecutableArgumentsNames(constructor) + ";";
    }

    /**
     * builder of Constructor. it combine {@link #getExecutableModifiers(Executable)} {@link #getClassName(Class)}
     * and {@link #getExecutableExceptions(Executable)} {@link #getConstructorBody(Constructor)}
     * @param constructor {@link Constructor} constructor, that program will write.
     * @return {@link String} default constructor
     */

    private String getConstructor(Constructor<?> constructor) {
        return Packer.mergeWithSeparator(" ",
                getExecutableModifiers(constructor),
                getClassName(constructor.getDeclaringClass()) + getExecutableArguments(constructor),
                getExecutableExceptions(constructor),
                Packer.mergeWithSeparator(System.lineSeparator(), "{", getConstructorBody(constructor), "}")
        );
    }

    /**
     * return {@link String} of constructors of {@code token}  but only non-private
     * separated by lineSeparator
     * @param token instance of {@link Class}
     * @return {@link String} of constructors of {@code token}
     * @throws ImplerException if all constructors are private, or there are no constructors.
     */

    private String getConstructors(Class<?> token) throws ImplerException {
        if (token.isInterface()) {
            return "";
        }
        List<Constructor<?>> constructors = Arrays.stream(token.getDeclaredConstructors())
                .filter(c -> !Modifier.isPrivate(c.getModifiers()))
                .collect(Collectors.toList());
        if (constructors.isEmpty()) {
            throw new ImplerException("Class with no non-private constructors can not be extended");
        }
        return constructors.stream()
                .map(this::getConstructor)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * return {@link String} package for {@code token}, if it's null, return ""
     * @param token instance of {@link Class}
     * @return full package of {@code token}, if it's null return ""
     */

    private static String getPackage(Class<?> token) {
        return token.getPackageName() == null ? "" : Packer.mergeWithSeparator(" " , token.getPackage() + ";");
    }

    /**
     * return the full name of the class {@code token}. It's combining
     * {@link #getClassModifiers(Class)}, {@link #getClassName(Class)}.
     * @param token  instance of {@link Class}
     * @return {@link String} full class name
     */

    private String getClassDefinition(Class<?> token) {
        return Packer.mergeWithSeparator( " ",
                getClassModifiers(token),
                "class", getClassName(token),
                Packer.mergeWithSeparator(" ", token.isInterface() ? "implements" : "extends", token.getCanonicalName())
        );
    }

    /**
     * Builder for classes and interfaces. Combining {@link #getPackage(Class)}
     * {@link #getConstructors(Class)} and {@link #getMethods(Class)}
     * @param token instance of {@link Class}
     * @return String~--- full class.
     * @throws ImplerException if there are exceptions in {@link #getConstructors(Class)} or {@link #getMethods(Class)}
     */

    private String getFullClass(Class<?> token) throws ImplerException {
        return Packer.mergeWithSeparator(System.lineSeparator(),
                getPackage(token),
                Packer.mergeWithSeparator(" ", getClassDefinition(token), "{"),
                getConstructors(token),
                getMethods(token),
                "}"
        );
    }

    /**
     * return {@link Class#getSimpleName()} with word "Impl"
     * used to generate name for implemented class.
     * @param token instance of {@link Class}
     * @return String of Simple name for the answer.
     */

    String getClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * creating directories to {@code file}
     * @param file {@link Path} of full path for file to write.
     * @throws IOException if there are no possibilities to create Directories.
     */

    private static void newPath(Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    /**
     * Encodes {@code String}, escaping all unicode characters in {@code arg}.
     *
     * @param arg the {@link String} to be encoded
     * @return the encoded {@link String}
     */

    private static String encode(String arg) {
        StringBuilder builder = new StringBuilder();
        for (char c : arg.toCharArray()) {
            builder.append(c < 128 ? String.valueOf(c) : String.format("\\u%04x", (int) c));
        }
        return builder.toString();
    }

    /**
     * tryes to write to {@code root} file implemented from {@code tokem}
     * @param token type token to create implementation for.
     * @param root root directory.
     * @throws ImplerException if there no way to make path to {@code root},
     * if token {@link Class#isPrimitive()}, {@link Class#isArray()}, {@link Modifier#isFinal(int)} or it's Enum.
     * if {@link BufferedWriter} throws {@link IOException}, we will rethrom {@link ImplerException}
     */

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(root, "root");
        Path realPath;
        if (token.isPrimitive() || token.isArray() ||
                Modifier.isFinal(token.getModifiers()) || token == Enum.class) {
            throw new ImplerException("Unsupported class token given");
        }
        try {
            realPath = root.resolve(Path.of(token.getPackageName().replace('.', File.separatorChar),
                    getClassName(token)  + ".java"));
            newPath(realPath);
        } catch (IOException e) {
            throw new ImplerException(e);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(realPath)) {
            writer.write(encode(getFullClass(token)));
        } catch (IOException e) {
            throw new ImplerException(e);
        }
    }
}
