package src.main.java.dev.forge.core.reflection;
import src.main.java.dev.forge.core.annotation.Omnissiah;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionDemo {

    public static void main(String[] args) throws Exception {

        Class<?> clazz = UserService.class;

        Omnissiah annotation = clazz.getAnnotation(Omnissiah.class);

        System.out.println(annotation);
        System.out.println(annotation.value());

    }
}


