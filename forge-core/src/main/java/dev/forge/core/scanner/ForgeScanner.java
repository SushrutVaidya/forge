package dev.forge.core.scanner;

import dev.forge.core.annotation.Omnissiah;
import dev.forge.core.annotation.Stereotypes;
import dev.forge.core.exception.ForgeScannerException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Discovers Forge components on the classpath.
 *
 * <p>Given a base package, the scanner walks the corresponding directory tree,
 * loads every class it finds and returns those annotated with {@link Omnissiah}.
 * It returns <em>metadata</em> ({@code Class} objects), never instances —
 * instantiation is the container's responsibility.
 *
 * <h2>Scope</h2>
 * This scanner supports exploded classpath directories only (the layout produced
 * by an IDE or {@code mvn}). Scanning inside JAR files, and resolving classes
 * through the thread context classloader, are deliberately out of scope for the
 * current milestone.
 *
 * <h2>Thread safety</h2>
 * Instances are immutable and stateless per call; {@link #scan(String)} may be
 * invoked concurrently.
 */
public final class ForgeScanner {

    private static final String CLASS_EXTENSION = ".class";
    private static final String FILE_PROTOCOL = "file";

    /**
     * Files that live inside package directories but are not loadable component
     * classes. {@code package-info} and {@code module-info} produce
     * {@code .class} files whose names are not valid binary class names.
     */
    private static final String PACKAGE_INFO = "package-info" + CLASS_EXTENSION;
    private static final String MODULE_INFO = "module-info" + CLASS_EXTENSION;

    private final ClassLoader classLoader;

    /**
     * Creates a scanner that uses the classloader which loaded Forge.
     */
    public ForgeScanner() {
        this(ForgeScanner.class.getClassLoader());
    }

    /**
     * Creates a scanner backed by an explicit classloader.
     *
     * <p>Package-private: intended for tests that need to control resource
     * resolution. Production code uses the {@linkplain #ForgeScanner() default
     * constructor}.
     *
     * @param classLoader the classloader used to locate and load classes
     */
    ForgeScanner(ClassLoader classLoader) {
        if (classLoader == null) {
            throw new ForgeScannerException("ClassLoader must not be null");
        }
        this.classLoader = classLoader;
    }

    /**
     * Scans a base package and returns every class annotated with
     * {@link Omnissiah}, including those in nested subpackages.
     *
     * @param basePackage the package to scan, e.g. {@code "com.example.app"}
     * @return an immutable list of discovered component classes; never {@code null},
     *         empty if the package exists but contains no components
     * @throws ForgeScannerException if the package is absent, is not on the
     *         filesystem, or a discovered class cannot be loaded
     */
    public List<Class<?>> scan(String basePackage) {
        if (basePackage == null) {
            throw new ForgeScannerException("Base package must not be null");
        }
        if (basePackage.isBlank()) {
            throw new ForgeScannerException("Base package must not be blank");
        }

        String resourcePath = basePackage.replace('.', '/');
        URL resource = classLoader.getResource(resourcePath);
        if (resource == null) {
            throw new ForgeScannerException("Base package not found on classpath: " + basePackage);
        }
        if (!FILE_PROTOCOL.equals(resource.getProtocol())) {
            throw new ForgeScannerException(
                    "Only filesystem scanning is supported, but base package '" + basePackage
                            + "' resolved to protocol '" + resource.getProtocol() + "'");
        }

        Path baseDirectory;
        try {
            baseDirectory = Path.of(resource.toURI());
        } catch (URISyntaxException e) {
            throw new ForgeScannerException("Invalid resource URI for base package: " + basePackage, e);
        }

        List<Class<?>> components = new ArrayList<>();
        scanDirectory(baseDirectory, basePackage, components);
        return List.copyOf(components);
    }

    /**
     * Recursively walks {@code directory}, loading each class file and collecting
     * those annotated with {@link Omnissiah}.
     *
     * @param directory   the filesystem directory to walk
     * @param packageName the package name corresponding to {@code directory}
     * @param components  accumulator for discovered component classes
     */
    private void scanDirectory(Path directory, String packageName, List<Class<?>> components) {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                String fileName = entry.getFileName().toString();
                if (Files.isDirectory(entry)) {
                    scanDirectory(entry, packageName + "." + fileName, components);
                } else if (isComponentCandidate(fileName)) {
                    String className = packageName + "." + fileName.substring(0, fileName.length() - CLASS_EXTENSION.length());
                    Class<?> candidate = loadClass(className);
                    if (Stereotypes.isComponent(candidate)) {
                        components.add(candidate);
                    }
                }
            }
        } catch (IOException e) {
            throw new ForgeScannerException("Failed to read package directory: " + packageName, e);
        }
    }

    /**
     * @return {@code true} if the file is a loadable class file, excluding the
     *         synthetic {@code package-info} and {@code module-info} classes
     */
    private static boolean isComponentCandidate(String fileName) {
        return fileName.endsWith(CLASS_EXTENSION)
                && !fileName.equals(PACKAGE_INFO)
                && !fileName.equals(MODULE_INFO);
    }

    /**
     * Loads a class by name <em>without initializing it</em>, so that scanning a
     * class never triggers its static initializers — the scanner must have no
     * side effects on the classes it inspects.
     *
     * @param className the fully-qualified binary class name
     * @return the loaded class
     * @throws ForgeScannerException if the class cannot be loaded
     */
    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException | LinkageError e) {
            throw new ForgeScannerException("Failed to load discovered class: " + className, e);
        }
    }
}
