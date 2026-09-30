package dev.forge.core.scanner;
import dev.forge.core.exception.ForgeScannerException;
import java.net.URL;
import java.util.List;
import java.net.URISyntaxException;
import java.nio.file.Path;

public final class ForgeScanner {
    public List<Class<?>> scan (String basePackage){
        if(basePackage == null || basePackage.isEmpty()){
            throw new ForgeScannerException("Base package must not be null" + "Base package must also not be blank");
        }
        String path = basePackage.replace('.','/');
        ClassLoader classLoader = ForgeScanner.class.getClassLoader();
        URL resource = classLoader.getResource(path);
        if(resource == null){
            throw new ForgeScannerException("Base package not found on classpath"+ " : " + basePackage);
        }
        if(!"file".equals(resource.getProtocol())){
            throw new ForgeScannerException("Only filesystem scanning is supported, found protocol '"
                    + resource.getProtocol() + "' for base package : " + basePackage);
        }
        Path basedir;
        try {
            basedir = Path.of(resource.toURI());
        } catch (URISyntaxException e) {
            throw new ForgeScannerException(
                    "Invalid resource URI for base package : " + basePackage, e);
        }
        return List.of();

    }
}

