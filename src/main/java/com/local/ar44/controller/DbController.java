package com.local.ar44.controller;

import com.local.ar44.service.H2DumpService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/db")
public class DbController {

    private final H2DumpService dumpService;

    public DbController(H2DumpService dumpService) {
        this.dumpService = dumpService;
    }

    @GetMapping("/save")
    public String save() throws Exception {
        dumpService.dump();
        return "Dump sauvegardé ✔";
    }
}
