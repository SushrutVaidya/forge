package src.main.java.dev.forge.core.scanner;

import java.net.URL;
import java.util.List;

public final class ForgeScanner {
    public List<Class<?>> scan (String basePackage){
        ClassLoader cls = ForgeScanner.class.getClassLoader();
        String path = basePackage.replace('.','/');
        URL resource = cls.getResource(path);






        return List.of();

    }
}
