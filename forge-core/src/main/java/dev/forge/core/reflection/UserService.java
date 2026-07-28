package dev.forge.core.reflection;

import src.main.java.dev.forge.core.annotation.Omnissiah;

@Omnissiah(value = "UserService")
public class UserService {
    private String name = "forge";
    public void save() {
        System.out.println("Saving...");
    }
}
