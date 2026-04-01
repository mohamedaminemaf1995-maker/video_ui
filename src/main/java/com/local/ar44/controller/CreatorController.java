package com.local.ar44.controller;

import com.local.ar44.dto.Creator;
import com.local.ar44.service.CreatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/config/creators")
public class CreatorController {
    @Autowired
    private CreatorService creatorService;

    @GetMapping
    public String listCreators(Model model) {
        model.addAttribute("creators", creatorService.findAll());
        return "creators";
    }

    @GetMapping("/add")
    public String addCreatorForm(Model model) {
        model.addAttribute("creator", new Creator());
        return "creator_form";
    }

    @PostMapping("/add")
    public String addCreator(@ModelAttribute Creator creator) {
        creatorService.save(creator);
        return "redirect:/config/creators";
    }

    @GetMapping("/edit/{id}")
    public String editCreatorForm(@PathVariable Long id, Model model) {
        model.addAttribute("creator", creatorService.findById(id).orElseThrow());
        return "creator_form";
    }

    @PostMapping("/edit/{id}")
    public String editCreator(@PathVariable Long id, @ModelAttribute Creator creator) {
        creator.setId(id);
        creatorService.save(creator);
        return "redirect:/config/creators";
    }

    @PostMapping("/delete/{id}")
    public String deleteCreator(@PathVariable Long id) {
        creatorService.deleteById(id);
        return "redirect:/config/creators";
    }
}
