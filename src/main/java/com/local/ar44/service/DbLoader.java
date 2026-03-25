package com.local.ar44.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class DbLoader implements CommandLineRunner {

    private final H2DumpService dumpService;

    public DbLoader(H2DumpService dumpService) {
        this.dumpService = dumpService;
    }

    @Override
    public void run(String... args) {
        try {
            File file = new File("./backup.sql");
            if (file.exists() && file.length() > 0) {
                dumpService.load();
            }

        } catch (Exception e) {
            System.out.println("❌ Erreur chargement dump: " + e.getMessage());
        }
    }
}
