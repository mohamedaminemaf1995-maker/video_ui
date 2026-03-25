package com.local.ar44.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.Statement;

@Service
public class H2DumpService {

    private final DataSource dataSource;

    public H2DumpService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void dump() throws Exception {
        String path = getPath();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("SCRIPT TO '" + path + "'");
            System.out.println("✔ Dump créé : " + path);
        }
    }

    public void load() throws Exception {
        String path = getPath();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP ALL OBJECTS");

            stmt.execute("RUNSCRIPT FROM '" + path + "'");
            System.out.println("✔ Dump chargé");
        }
    }

    private String getPath() {
        return new File("backup.sql").getAbsolutePath().replace("\\", "/");
    }
}
