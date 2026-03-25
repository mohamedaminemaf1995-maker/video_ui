package com.local.ar44.controller;

import com.local.ar44.service.XspfImportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final XspfImportService xspfImportService;

    public ImportController(XspfImportService xspfImportService) {
        this.xspfImportService = xspfImportService;
    }

    @GetMapping("/xspf")
    public ResponseEntity<String> importFromFile() {
        try {
            // Lire le fichier depuis resources
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("data.xml");

            if (inputStream == null) {
                return ResponseEntity.badRequest().body("Fichier data.xml introuvable");
            }

            String xmlContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            xspfImportService.importFromXml(xmlContent);

            return ResponseEntity.ok("Import depuis fichier OK");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur import: " + e.getMessage());
        }
    }
}