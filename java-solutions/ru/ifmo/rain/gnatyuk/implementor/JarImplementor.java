package ru.ifmo.rain.gnatyuk.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;


/**
 * Class implements {@link Impler} and {@link JarImpler}.
 * Gets  or {@code .jar} and implements class.
 * @author imka
 * @version 1.0
 */

public class JarImplementor extends Implementor implements JarImpler {

    /**
     * Main. Gets args from console for {@link Implementor}
     * <ul>
     *  <li>
     *      2-args {@code className outPath} creates file
     *      {@code .java} file in {@code outPath} by {@link #implement(Class, Path)}
     *  </li>
     *  <li>
     *      3-args {@code -jar className outPath} creates file
     *       {@code .jar} file in {@code outPath} by {@link #implementJar(Class, Path)}
     *  </li>
     * </ul>
     * @param args arguments for application
     */

    public static void main(String[] args) {
        //new Implementor().implement(Child.class, Path.of("C:\\Users\\ASUS\\IdeaProjects\\javaAdvanced"));
        if (args == null || args.length > 3 || args.length < 2) {
            System.err.println("Expected 2 or 3 args: (-jar)? classname, outPath");
            return;
        }
        for (String arg : args) {
            if (arg == null) {
                System.out.println(Arrays.toString(args) + "is null");
            }
        }
        try {
            if (args.length == 2) {
                new JarImplementor().implement(Class.forName(args[0]), Path.of(args[1]));
            } else if (args[0].equals("-jar") || args[0].equals("--jar")) {
                new JarImplementor().implementJar(Class.forName(args[0]), Path.of(args[1]));
            } else {
                System.err.println("args[0] should be -jar or --jar, but " + args[0] + " found");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Class not Found" + e.getMessage());
        } catch (ImplerException e) {
            System.err.println("Failed to Implement" + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("Failed to make Path" + e.getMessage());
        }
    }

    /**
     * Compiles implemented class extending or implementing {@code token}
     * and stores <code>.class</code> file in given {@code tempDirectory}.
     * Uses <code>-classpath</code> pointing to location of class or interface specified by {@code token}.
     *
     * @param token type token that was implemented
     * @param tempDirectory temporary directory where all <code>.class</code> files are stored
     * @throws ImplerException if an error occurs during compilation or compiler is null
     */

    private void compileClass(Class<?> token, Path tempDirectory) throws ImplerException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Can not find java compiler");
        }

        Path originPath;
        try {
            CodeSource codeSource = token.getProtectionDomain().getCodeSource();
            String uri = codeSource == null ? "" : codeSource.getLocation().getPath();
            if (uri.startsWith("/")) {
                uri = uri.substring(1);
            }
            originPath = Path.of(uri);
        } catch (InvalidPathException e) {
            throw new ImplerException(String.format("Can not find valid class path: %s", e));
        }
        String[] cmdArgs = new String[]{
                "-cp",
                tempDirectory.toString() + File.pathSeparator + originPath.toString(),
                Path.of(tempDirectory.toString(), token.getPackageName().replace('.', File.separatorChar), getClassName(token) + ".java").toString()
        };
        System.out.println(Path.of(tempDirectory.toString(), token.getPackageName().replace('.', File.separatorChar), getClassName(token) + ".java").toString());
        if (compiler.run(null, null, null, cmdArgs) != 0) {
            throw new ImplerException("Can not compile generated code");
        }
    }

    /**
     * Builds a <code>.jar</code> file containing compiled by {@link #compileClass(Class, Path)}
     * sources of implemented class using basic {@link Manifest}.
     *
     * @param jarFile path where resulting {@code .jar} should be saved
     * @param tempDirectory temporary directory where all <code>.class</code> files are stored
     * @param token type token that was implemented
     * @throws ImplerException if {@link JarOutputStream} throws an {@link IOException}
     */

    private void buildJar(Path jarFile, Path tempDirectory, Class<?> token) throws ImplerException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (JarOutputStream stream = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            String pathSuffix = token.getPackageName().replace('.', '/') + "/" + getClassName(token) + ".class";
            System.out.println(pathSuffix);
            stream.putNextEntry(new ZipEntry(pathSuffix));
            Files.copy(tempDirectory.resolve(pathSuffix), stream);
        } catch (IOException e) {
            throw new ImplerException(e.getMessage());
        }
    }

    /**
     * implements Jar for {@code token}
     * @param token type token to create implementation for.
     * @param jarFile target {@code jar} file.
     * @throws ImplerException if {@code token} or {@code jarFile} is null
     */

    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        if (token == null || jarFile == null) {
            throw new ImplerException("Invalid (null) argument given");
        }
        ImplementorUtils.createDirectoriesTo(jarFile.normalize());
        ImplementorUtils utils = new ImplementorUtils(jarFile.toAbsolutePath().getParent());
        implement(token, utils.getTempDirectory());
        compileClass(token, utils.getTempDirectory());
        buildJar(jarFile, utils.getTempDirectory(), token);

    }
}
