package com.academic.annotation.controller;

import com.academic.annotation.service.TrainingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/training")
public class TrainingController {

    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @GetMapping
    public String training(Model model) {
        model.addAttribute("trainingRuns", trainingService.history());
        return "admin/training";
    }

    @PostMapping("/launch")
    public String launch(RedirectAttributes redirectAttributes) {
        trainingService.launchTraining();
        redirectAttributes.addFlashAttribute("message", "Training launched. Latest metrics are below.");
        return "redirect:/admin/training";
    }
}
