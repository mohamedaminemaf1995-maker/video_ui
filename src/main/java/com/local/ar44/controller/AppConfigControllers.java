package com.local.ar44.controller;

import com.local.ar44.service.AppConfigService;
import jakarta.servlet.http.HttpSession;
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
    public ResponseEntity<String> setHost(@RequestParam String host, HttpSession session) {
        service.setHost(host);
        session.setAttribute("mediaHost", host);
        return ResponseEntity.ok("Host mis à jour : " + host);
    }

    // 🔹 POST → login
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password, HttpSession session) {
        if (service.login(username, password)) {
            session.setAttribute("authenticated", true);
            session.setAttribute("username", username);
            return ResponseEntity.ok("Authentification réussie");
        }
        return ResponseEntity.status(401).body("Identifiants invalides");
    }

    // 🔹 POST → set credentials (première configuration)
    @PostMapping("/credentials/set")
    public ResponseEntity<String> setCredentials(@RequestParam String username, @RequestParam String password) {
        service.setCredentials(username, password);
        return ResponseEntity.ok("Identifiants configurés");
    }

    // 🔹 GET → vérifier si des identifiants existent
    @GetMapping("/credentials/exists")
    public ResponseEntity<Boolean> credentialsExist() {
        return ResponseEntity.ok(service.hasCredentials());
    }

    // 🔹 GET → vérifier authentification
    @GetMapping("/auth/check")
    public ResponseEntity<Boolean> checkAuth(HttpSession session) {
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        return ResponseEntity.ok(authenticated != null && authenticated);
    }

    // 🔹 GET → logout
    @GetMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Déconnecté");
    }
}