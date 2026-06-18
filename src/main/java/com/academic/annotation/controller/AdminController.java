package com.academic.annotation.controller;

import com.academic.annotation.service.AssignmentService;
import com.academic.annotation.service.SpamDetectionService;
import com.academic.annotation.service.StatsService;
import com.academic.annotation.service.TrainingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final StatsService statsService;
    private final AssignmentService assignmentService;
    private final SpamDetectionService spamDetectionService;
    private final TrainingService trainingService;

    public AdminController(StatsService statsService,
                           AssignmentService assignmentService,
                           SpamDetectionService spamDetectionService,
                           TrainingService trainingService) {
        this.statsService = statsService;
        this.assignmentService = assignmentService;
        this.spamDetectionService = spamDetectionService;
        this.trainingService = trainingService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", statsService.globalStats());
        model.addAttribute("progressByUser", assignmentService.progressByUser());
        model.addAttribute("suspiciousUsers", spamDetectionService.detectSuspiciousUsers());
        model.addAttribute("trainingRuns", trainingService.history().stream().limit(5).toList());
        return "admin/dashboard";
    }

    @GetMapping("/assignments")
    public String assignments(Model model) {
        model.addAttribute("progressByUser", assignmentService.progressByUser());
        return "admin/assignments";
    }

    @PostMapping("/assignments/auto")
    public String autoAssign(RedirectAttributes redirectAttributes) {
        int created = assignmentService.assignAllItemsToAnnotators(3);
        redirectAttributes.addFlashAttribute("message", created + " new assignments created.");
        return "redirect:/admin/assignments";
    }

    @GetMapping("/stats")
    public String stats(Model model) {
        model.addAttribute("stats", statsService.globalStats());
        model.addAttribute("progressByUser", assignmentService.progressByUser());
        model.addAttribute("suspiciousUsers", spamDetectionService.detectSuspiciousUsers());
        return "admin/stats";
    }
}
