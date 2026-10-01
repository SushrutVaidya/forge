package dev.forge.core.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.forge.core.exception.ForgeScannerException;
import dev.forge.core.scanner.fixtures.AlphaComponent;
import dev.forge.core.scanner.fixtures.BetaComponent;
import dev.forge.core.scanner.fixtures.NotAComponent;
import dev.forge.core.scanner.fixtures.nested.NestedComponent;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ForgeScannerTest {

    private static final String FIXTURES = "dev.forge.core.scanner.fixtures";
    private static final String EMPTIES = "dev.forge.core.scanner.empties";

    /** Uses the test classloader, which sees the compiled fixture classes. */
    private final ForgeScanner scanner = new ForgeScanner(getClass().getClassLoader());

    @Nested
    @DisplayName("argument validation")
    class Validation {

        @Test
        @DisplayName("null base package fails fast")
        void nullPackage() {
            ForgeScannerException ex =
                    assertThrows(ForgeScannerException.class, () -> scanner.scan(null));
            assertTrue(ex.getMessage().toLowerCase().contains("null"), ex.getMessage());
        }

        @Test
        @DisplayName("blank base package fails fast")
        void blankPackage() {
            ForgeScannerException ex =
                    assertThrows(ForgeScannerException.class, () -> scanner.scan("   "));
            assertTrue(ex.getMessage().toLowerCase().contains("blank"), ex.getMessage());
        }

        @Test
        @DisplayName("empty string is treated as blank, not the classpath root")
        void emptyStringRejected() {
            assertThrows(ForgeScannerException.class, () -> scanner.scan(""));
        }

        @Test
        @DisplayName("null classloader is rejected at construction")
        void nullClassLoader() {
            assertThrows(ForgeScannerException.class, () -> new ForgeScanner(null));
        }
    }

    @Nested
    @DisplayName("resource resolution")
    class Resolution {

        @Test
        @DisplayName("absent package fails fast and names the package")
        void absentPackage() {
            String missing = "dev.forge.core.this.does.not.exist";
            ForgeScannerException ex =
                    assertThrows(ForgeScannerException.class, () -> scanner.scan(missing));
            assertTrue(ex.getMessage().contains(missing), ex.getMessage());
        }
    }

    @Nested
    @DisplayName("discovery")
    class Discovery {

        @Test
        @DisplayName("finds annotated classes, recursing into subpackages")
        void findsComponentsRecursively() {
            Set<Class<?>> found = Set.copyOf(scanner.scan(FIXTURES));
            assertTrue(found.contains(AlphaComponent.class), "root component");
            assertTrue(found.contains(BetaComponent.class), "explicitly-named component");
            assertTrue(found.contains(NestedComponent.class), "nested component (recursion)");
        }

        @Test
        @DisplayName("ignores classes that are not annotated")
        void ignoresPlainClasses() {
            assertTrue(!scanner.scan(FIXTURES).contains(NotAComponent.class));
        }

        @Test
        @DisplayName("finds exactly the three annotated fixtures and nothing else")
        void findsExactlyTheComponents() {
            Set<String> names = scanner.scan(FIXTURES).stream()
                    .map(Class::getSimpleName)
                    .collect(Collectors.toSet());
            assertEquals(Set.of("AlphaComponent", "BetaComponent", "NestedComponent"), names);
        }

        @Test
        @DisplayName("a valid package with no components returns an empty list")
        void emptyPackageReturnsEmptyList() {
            assertEquals(List.of(), scanner.scan(EMPTIES));
        }

        @Test
        @DisplayName("the default constructor's classloader also discovers components")
        void defaultConstructorWorks() {
            Set<Class<?>> found = Set.copyOf(new ForgeScanner().scan(FIXTURES));
            assertTrue(found.contains(AlphaComponent.class));
        }

        @Test
        @DisplayName("skips package-info/module-info class files")
        void skipsPackageInfo() {
            // The package contains a package-info.class; the scanner must skip it
            // (loading "...package-info" would throw) and return only the component.
            Set<String> names = scanner.scan("dev.forge.core.scanner.pkginfo").stream()
                    .map(Class::getSimpleName)
                    .collect(Collectors.toSet());
            assertEquals(Set.of("PkgComponent"), names);
        }
    }

    @Nested
    @DisplayName("return contract")
    class ReturnContract {

        @Test
        @DisplayName("the returned list is immutable")
        void resultIsImmutable() {
            List<Class<?>> result = scanner.scan(FIXTURES);
            assertThrows(UnsupportedOperationException.class, () -> result.add(String.class));
        }
    }
}
