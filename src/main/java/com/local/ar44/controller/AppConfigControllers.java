package com.local.ar44.controller;

import com.local.ar44.service.AppConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
public class AppConfigControllers {

    private final AppConfigService service;

    public AppConfigControllers(AppConfigService service) {
        this.service = service;
    }

    // 🔹 GET → récupérer le host
    @GetMapping("/host")
    public ResponseEntity<String> getHost() {
        String host = service.getHost();
        return ResponseEntity.ok(host != null ? host : "non défini");
    }

    // 🔹 POST → modifier le host
    @GetMapping("/host/set")
    public ResponseEntity<String> setHost(@RequestParam String host) {
        service.setHost(host);
        return ResponseEntity.ok("Host mis à jour : " + host);
    }
}