/**
 * Fixture package carrying a package-level annotation, which forces javac to emit
 * a {@code package-info.class}. The scanner must skip that file (its name is not a
 * valid class name) rather than fail trying to load it.
 */
@Deprecated
package dev.forge.core.scanner.pkginfo;
