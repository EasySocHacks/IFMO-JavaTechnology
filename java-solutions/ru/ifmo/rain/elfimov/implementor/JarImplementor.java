package ru.ifmo.rain.elfimov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class JarImplementor extends Implementor implements JarImpler {

    /**
     * Produce <strong>.jar</strong> file consisting of implementing class or interface specified by provided <strong>token</strong>.
     *
     * Generated <strong>.jar</strong> file location is <strong>root</strong>.
     * Generated class inside of <strong>.jar</strong> file located at <strong>token</strong> package.
     * Generated class named as <strong>token</strong> class name with suffix <strong>Impl</strong>.
     *
     * Generated class must to compile but shouldn't to execute any logic.
     *
     * @param token type token to create implementation for.
     * @param jarFile target <strong>.jar</strong> file.
     * @throws ImplerException when cannot create generated file directory(ies)/file(s) or write in file/jar.
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        checkImplementingExceptions(token, jarFile.getParent());

        try {
            Files.createDirectories(jarFile.toAbsolutePath().getParent());
        } catch (IOException e) {
            throw new ImplerException(String.format("Cannot create directory(s) by path '%s'", jarFile), e);
        }

        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");

        Path tempDirectory = null;

        try {
            tempDirectory = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "jarFiles");

            implement(token, tempDirectory);

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

            String classpath = Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();

            if (compiler == null || compiler.run(null, null, null,
                    "-cp", classpath, tempDirectory.resolve(Paths.get(
                            token.getPackageName().replaceAll("\\.", "\\" + File.separator),
                            String.format("%sImpl.java", token.getSimpleName()))).toString()) != 0) {
                throw new ImplerException("Cannot compile .java Implementing file");
            }

            try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
                jarOutputStream.putNextEntry(new JarEntry(String.format("%s/%sImpl.class",
                        token.getPackageName().replace('.', '/'), token.getSimpleName())));

                Files.copy(tempDirectory.
                        resolve(token.getPackageName().replace('.', File.separatorChar)).
                        resolve(token.getSimpleName() + "Impl.class"), jarOutputStream);

            } catch (IOException e) {
                throw new ImplerException("Cannot create jar writer to write the implementor", e);
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        } finally {
            recursiveDeletingPath(tempDirectory);
        }
    }

    /**
     * Deleting file or directory with all its contents.
     *
     * @param path a file or directory to delete.
     * @throws ImplerException when file/directory at any depth in <strong>path</strong> cannot be deleted.
     */
    private void recursiveDeletingPath(Path path) throws ImplerException {
        try {
            Files.walkFileTree(path, new RecursiveDeletingPathVisitor());
        } catch (IOException e) {
            throw new ImplerException(String.format("Cannot delete path '%s'", path), e);
        }
    }

    /**
     * Run the program.
     *
     * Programme runs with usage: {@code java -jar Implementor.jar <Class name to implement>}
     * It implement input class into {@code <Class name>Impl.java} class and package it in .jar file.
     *
     * @param args Program arguments.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -jar Implementor.jar <Class name to implement>");
        } else {
            try {
                Class<?> implementingClass = Class.forName(args[0]);

                JarImplementor implementor = new JarImplementor();

                implementor.implementJar(implementingClass, Paths.get(implementingClass.getSimpleName() + "Impl.jar").toAbsolutePath());

            } catch (ClassNotFoundException e) {
                System.err.println(String.format("Unknown class '%s' to implement", args[0]));
                e.printStackTrace();
            } catch (ImplerException e) {
                System.err.println(String.format("Cannot implement '%s' class", args[0]));
                e.printStackTrace();
            }
        }
    }
}