package ru.ifmo.rain.elfimov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class Implementor implements Impler {
    private final String LINE_SEPARATOR_STRING = System.lineSeparator();
    private final String TABULATOR_STRING = "\t";

    /**
     * Produce compiling <strong>.java</strong> file implementing class or interface specified by provided <strong>token</strong>.
     *
     * Generated class located at <strong>root</strong>/<strong>token</strong> package.
     * Generated class named as <strong>token</strong> class name with suffix <strong>Impl</strong>.
     *
     * Generated class must to compile but shouldn't to execute any logic.
     *
     * @param token type token to create implementation for.
     * @param root root directory.
     * @throws ImplerException when cannot create generated file directory(ies)/file or write in file.
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        checkImplementingExceptions(token, root);

        Path implementorFileDirectoryPath = Paths.get(root.toString(),
                token.getPackageName().replaceAll("\\.", "\\" + File.separator));

        Path implementorFilePath = Paths.get(implementorFileDirectoryPath.toString(),
                String.format("%sImpl.java", token.getSimpleName()));

        try {
            Files.createDirectories(implementorFileDirectoryPath);
        } catch (IOException e) {
            throw new ImplerException(String.format("Cannot create directory(s) '%s'", implementorFileDirectoryPath), e);
        }

        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(implementorFilePath, StandardCharsets.UTF_8)) {
            bufferedWriter.write(escapeUnicode(getClassString(token)));
        } catch (IOException e) {
            throw new ImplerException("Cannot create writer to write the implementor", e);
        }
    }

    /**
     * Check <strong>token</strong> ans <strong>path</strong> for correctness to implement class/interface.
     *
     * Check the ability to implement not null and not empty <strong>token</strong>
     * at not null and not empty <strong>path</strong>.
     *
     * @param token token, which is checked for the possibility of implementation.
     * @param path path, which is checked for the possibility of implementation at.
     * @throws ImplerException when it isn't possible to implement <strong>token</strong> at <strong>path</strong>.
     */
    void checkImplementingExceptions(Class<?> token, Path path) throws ImplerException {
        if (token == null) {
            throw new ImplerException("'token' mustn't be a null");
        }

        if (token.equals(Enum.class)) {
            throw new ImplerException("'token' mustn't be an enum");
        }

        if (token.isPrimitive() || Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("'token' mustn't be a primitive of final class");
        }

        if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("'token' mustn't be a private class/interface");
        }

        if (path == null || path.toString().isEmpty()) {
            throw new ImplerException("'path' mustn't be a null or empty");
        }
    }

    /**
     * Return <strong>token</strong> implementation class {@link java.lang.String}.
     *
     * The returning text must to compile if it's generated a <strong>.java</strong> file with this text and
     * this file knows about <strong>token</strong> file.
     *
     * @param token the class whose implementation is returned as {@link java.lang.String}.
     * @return <strong>token</strong> class implementation as {@link java.lang.String}.
     * @throws ImplerException when cannot implement method/constructor.
     */
    private String getClassString(Class<?> token) throws ImplerException {
        List<String> methodsAndConstructorsString = getMethodsString(getComparingMethodsSet(token));
        List <String> constructorsString = getConstructorsString(token);

        methodsAndConstructorsString.addAll(constructorsString);

        StringBuilder stringBuilder = new StringBuilder();

        if (!token.getPackage().getName().isEmpty()) {
            stringBuilder.append(String.format("%s;%s%s",
                    token.getPackage().toString(),
                    LINE_SEPARATOR_STRING,
                    LINE_SEPARATOR_STRING));
        }

        stringBuilder.append(String.format("public class %s %s %s {%s",
                String.format("%sImpl", token.getSimpleName()),
                (token.isInterface() ? "implements" : "extends"),
                token.getCanonicalName(),
                LINE_SEPARATOR_STRING));

        for (String method : methodsAndConstructorsString) {
            stringBuilder.append(String.format("%s%s%s%s",
                    LINE_SEPARATOR_STRING,
                    TABULATOR_STRING,
                    method.replaceAll(LINE_SEPARATOR_STRING, LINE_SEPARATOR_STRING + TABULATOR_STRING),
                    LINE_SEPARATOR_STRING));
        }

        stringBuilder.append("}");

        return stringBuilder.toString();
    }

    /**
     * Return <strong>token</strong> implementation class {@link List}
     * of constructors {@link java.lang.String}.
     *
     * @param token the class whose constructors implementation is returned as {@link List} of {@link java.lang.String}.
     * @return <strong>token</strong> class constructors implementation as {@link List} of {@link java.lang.String}.
     * @throws ImplerException when cannot implement private or default constructor.
     */
    private List<String> getConstructorsString(Class<?> token) throws ImplerException {
        List<String> implementedConstructors = new ArrayList<>();

        boolean hasDefaultConstructor = false;
        Constructor<?>[] declaredConstructors = token.getDeclaredConstructors();

        for (Constructor<?> constructor : declaredConstructors) {
            if (constructor.getParameters().length == 0) {
                if (Modifier.isPrivate(constructor.getModifiers())) {
                    throw new ImplerException("Cannot implement private or default constructor");
                }

                hasDefaultConstructor = true;
                break;
            }
        }

        if (!hasDefaultConstructor && !token.isInterface()) {
            for (Constructor<?> constructor : declaredConstructors) {
                implementedConstructors.add(getConstructorString(constructor));
            }
        }

        return implementedConstructors;
    }

    /**
     * Get {@link Set} of {@link ComparingImplementingMethodWrapper}.
     *
     * Choose methods to implement and return set of this method's wrappers as {@link ComparingImplementingMethodWrapper}.
     *
     * @param token a token to get {@link Set} of implementing method's wrappers.
     * @return {@link Set} of implementing in <strong>token</strong> method's wrappers.
     */
    private Set<ComparingImplementingMethodWrapper> getComparingMethodsSet(Class<?> token) {
        Set<ComparingImplementingMethodWrapper> comparingMethodsSet = new HashSet<>();

        for (Class<?> interfaceToken : token.getInterfaces()) {
            comparingMethodsSet.addAll(getComparingMethodsSet(interfaceToken));
        }

        if (token.getSuperclass() != null) {
            comparingMethodsSet.addAll(getComparingMethodsSet(token.getSuperclass()));
        }

        for (Method method : token.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if (!Modifier.isPrivate(modifiers)) {
                if (Modifier.isAbstract(method.getModifiers())) {
                    ComparingImplementingMethodWrapper comparingImplementingMethodWrapper = new ComparingImplementingMethodWrapper(method);

                    comparingMethodsSet.remove(comparingImplementingMethodWrapper);
                    comparingMethodsSet.add(comparingImplementingMethodWrapper);
                } else {
                    comparingMethodsSet.remove(new ComparingImplementingMethodWrapper(method));
                }
            }
        }

        return comparingMethodsSet;
    }

    /**
     * Get {@link List} of {@link String} implementation of methods from
     * <strong>comparingMethodsSet</strong> of method's wrappers.
     *
     * Implement methods from <strong>comparingMethodsSet</strong> and return {@link List} of {@link String} of them implementation.
     *
     * @param comparingMethodsSet {@link Set} of method's wrappers as {@link ComparingImplementingMethodWrapper}.
     * @return {@link List} of {@link String} of implementation methods from <strong>comparingMethodsSet</strong>.
     */
    private List<String> getMethodsString(Set<ComparingImplementingMethodWrapper> comparingMethodsSet) {
        List<String> implementedMethods = new ArrayList<>();

        for (ComparingImplementingMethodWrapper comparingImplementingMethodWrapper : comparingMethodsSet) {
            Method method = comparingImplementingMethodWrapper.getMethod();
            int modifiers = method.getModifiers();
            if (Modifier.isAbstract(method.getModifiers()) && !Modifier.isPrivate(modifiers)) {
                implementedMethods.add(getMethodString(method));
            }
        }

        return implementedMethods;
    }

    /**
     * Return {@link java.lang.String} text of implemented method.
     *
     * Text built of <strong>head</strong> <strong>exception</strong> and <strong>body</strong> {@link java.lang.String}.
     *
     * @param head consists of method's modifiers, simple name and parameters in brackets.
     * @param exception consists of method's exceptions {@link java.lang.String} without <strong>throw</strong> text.
     * @param body consists of method's body {@link java.lang.String} (everything between <strong>{</strong> and <strong>}</strong>}).
     * @return {@link java.lang.String} text of implemented method.
     */
    private String getMethodString(String head, String exception, String body) {

        return String.format("%s%s {%s",
                head,
                (exception.isEmpty() ? "" : String.format(" throws %s", exception)),
                LINE_SEPARATOR_STRING) +
                String.format("%s%s", TABULATOR_STRING, body) +
                String.format("%s}", LINE_SEPARATOR_STRING);
    }

    /**
     * Return exceptions {@link java.lang.String} text of implemented method.
     *
     * All exceptions are separated with <strong>, </strong> text if it's more than one exception.
     *
     * @param methodOrConstructor <strong>methodOrConstructor</strong> to get itself exceptions {@link java.lang.String}.
     * @param <T> a type extends {@link java.lang.reflect.Executable}
     *           (excepted {@link java.lang.reflect.Method} or {@link java.lang.reflect.Constructor}).
     * @return exceptions {@link java.lang.String} text of implemented method.
     */
    private <T extends Executable> String getMethodOrConstructorThrowsExceptionsString(T methodOrConstructor) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Class<?> exception : methodOrConstructor.getExceptionTypes()) {
            if (stringBuilder.length() != 0) {
                stringBuilder.append(", ");
            }

            stringBuilder.append(exception.getCanonicalName());
        }

        return stringBuilder.toString();
    }

    /**
     * Return the <strong>constructor</strong> implementation {@link java.lang.String}.
     *
     * @param constructor a {@link java.lang.reflect.Constructor} to implement and convert to a {@link java.lang.String}.
     * @return the <strong>constructor</strong> implementation {@link java.lang.String}.
     */
    private String getConstructorString(Constructor constructor) {
        return getMethodString(
                String.format("%s %s(%s)",
                        Modifier.toString(constructor.getModifiers() & ~Modifier.TRANSIENT & ~Modifier.ABSTRACT),
                        String.format("%sImpl", constructor.getDeclaringClass().getSimpleName()),
                        getMethodParametersString(constructor.getParameters())),
                getMethodOrConstructorThrowsExceptionsString(constructor),
                String.format("super(%s);", getConstructorArgumentsNameString(constructor)));
    }

    /**
     * Return the <strong>constructor</strong>'s arguments name by {@link java.lang.String}.
     *
     * All names are separated with <strong>, </strong> text if it's more than one argument.
     * If it's no arguments at all, then returning an empty {@link java.lang.String}.
     *
     * @param constructor a {@link java.lang.reflect.Constructor} to convert itself parameters names into {@link java.lang.String}.
     * @return the <strong>constructor</strong>'s parameters name {@link java.lang.String}.
     */
    private String getConstructorArgumentsNameString(Constructor constructor) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Parameter parameter : constructor.getParameters()) {
            if (stringBuilder.length() != 0) {
                stringBuilder.append(", ");
            }

            stringBuilder.append(parameter.getName());
        }

        return  stringBuilder.toString();
    }

    /**
     * Return the <strong>method</strong> implementation {@link java.lang.String}.
     *
     * @param method a {@link java.lang.reflect.Method} to implement and convert to a {@link java.lang.String}.
     * @return the <strong>method</strong> implementation {@link java.lang.String}.
     */
    private String getMethodString(Method method) {
        return getMethodString(
                String.format("%s %s %s(%s)",
                        Modifier.toString(method.getModifiers() & ~Modifier.TRANSIENT & ~Modifier.ABSTRACT),
                        method.getReturnType().getCanonicalName(),
                        method.getName(),
                        getMethodParametersString(method.getParameters())),
                getMethodOrConstructorThrowsExceptionsString(method),
                getMethodReturnObjectString(method.getReturnType()));
    }

    /**
     * Return a method parameters {@link java.lang.String}.
     *
     * All parameters are separated with <strong>, </strong> text if it's more than one parameter.
     * If it's no parameters at all, then returning an empty {@link java.lang.String}.
     *
     * @param parameters a {@link java.lang.reflect.Parameter} array to convert itself into a {@link java.lang.String}.
     * @return the <strong>parameters</strong>'s {@link java.lang.String}.
     */
    private String getMethodParametersString(Parameter[] parameters) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Parameter parameter : parameters) {
            if (stringBuilder.length() != 0) {
                stringBuilder.append(", ");
            }

            stringBuilder.append(String.format("%s %s", parameter.getType().getCanonicalName(), parameter.getName()));
        }

        return stringBuilder.toString();
    }

    /**
     * Convert <strong>returnType</strong> type into correct method's return {@link java.lang.String}.
     *
     * @param returnType the class to convert to a return {@link java.lang.String}.
     * @return a {@link java.lang.String} with correct return for <strong>returnType</strong>
     * class type with <strong>return</strong> text.
     */
    private String getMethodReturnObjectString(Class<?> returnType) {
        if (returnType.isPrimitive()) {
            if (returnType.equals(boolean.class)) {
                return "return false;";
            } else if (returnType.equals(void.class)) {
                return "return;";
            } else {
                return "return 0;";
            }
        } else {
            return "return null;";
        }
    }

    /**
     *Convert {@link java.lang.String} to unicode encoding.
     *
     * If a character of <strong>basicString</strong> is more then 127, then convert it to unicode format.
     * And keep symbol otherwise.
     *
     * @param basicString a {@link java.lang.String}, converting to unicode.
     * @return a {@link java.lang.String} converted to unicode from <strong>basicString</strong>.
     */
    private String escapeUnicode(String basicString) {
        StringBuilder unicodeClassStringBuilder = new StringBuilder();

        for (char classStringChar : basicString.toCharArray()) {
            unicodeClassStringBuilder.append(classStringChar >= 128 ?
                    String.format("\\u%04X", (int) classStringChar) :
                    classStringChar);
        }

        return unicodeClassStringBuilder.toString();
    }

    /**
     * An accessory class defining rule of deleting file/directory.
     */
    static class RecursiveDeletingPathVisitor extends SimpleFileVisitor<Path> {
        /**
         * {@inheritDoc}
         *
         * Delete <strong>file</strong> when visit it.
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);

            return FileVisitResult.CONTINUE;
        }

        /**
         * {@inheritDoc}
         *
         * Delete directory <strong>file</strong> after visiting it.
         */
        @Override
        public FileVisitResult postVisitDirectory(Path file, IOException exc) throws IOException {
            Files.delete(file);

            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * Wrapper to compare implementing methods.
     */
    static class ComparingImplementingMethodWrapper {
        private final Method method;

        /**
         * Get {@link Method} to wrap up it.
         *
         * @param method method to wrap up.
         */
        ComparingImplementingMethodWrapper(Method method) {
            this.method = method;
        }

        /**
         * {@inheritDoc}
         *
         * Return false if input {@link Object} is not an instance of {@link ComparingImplementingMethodWrapper}.
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ComparingImplementingMethodWrapper)) {
                return false;
            }

            ComparingImplementingMethodWrapper comparingMethod = (ComparingImplementingMethodWrapper)o;

            return (method.getName().equals(comparingMethod.getMethod().getName()) &&
                    Arrays.equals(method.getParameterTypes(), comparingMethod.getMethod().getParameterTypes()));
        }

        @Override
        public int hashCode() {
            return method.getName().hashCode() * 31 +
                    Arrays.hashCode(method.getParameterTypes()) * 37;
        }

        /**
         * Getter for {@link Method} in current wrapper.
         *
         * @return wrapped up {@link Method}.
         */
        Method getMethod() {
            return method;
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Implementor <Class name to implement>");
        } else {
            try {
                Class<?> implementingClass = Class.forName(args[0]);

                Implementor implementor = new Implementor();

                implementor.implement(implementingClass, Paths.get(".").toAbsolutePath());

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