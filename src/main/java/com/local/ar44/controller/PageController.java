package com.local.ar44.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;
import jakarta.servlet.http.HttpSession;

@Controller
public class PageController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public Object index(HttpSession session) {
        if (session.getAttribute("authenticated") == null || !(boolean) session.getAttribute("authenticated")) {
            return new RedirectView("/login", true);
        }
        return "index";
    }

    @GetMapping("/favorites")
    public Object favorites(HttpSession session) {
        if (session.getAttribute("authenticated") == null || !(boolean) session.getAttribute("authenticated")) {
            return new RedirectView("/login", true);
        }
        return "favorites";
    }
    @GetMapping("/recently")
    public Object recently(HttpSession session) {
        if (session.getAttribute("authenticated") == null || !(boolean) session.getAttribute("authenticated")) {
            return new RedirectView("/login", true);
        }
        return "recently";
    }

    @GetMapping("/settings")
    public Object settings(HttpSession session) {
        if (session.getAttribute("authenticated") == null || !(boolean) session.getAttribute("authenticated")) {
            return new RedirectView("/login", true);
        }
        return "settings";
    }
}
