package com.academic.annotation.controller;

import com.academic.annotation.model.TaskType;
import com.academic.annotation.service.AssignmentService;
import com.academic.annotation.service.DatasetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class DatasetController {

    private final DatasetService datasetService;
    private final AssignmentService assignmentService;

    public DatasetController(DatasetService datasetService, AssignmentService assignmentService) {
        this.datasetService = datasetService;
        this.assignmentService = assignmentService;
    }

    @GetMapping("/import")
    public String importPage(Model model) {
        model.addAttribute("taskTypes", TaskType.values());
        model.addAttribute("items", datasetService.findAllItems());
        return "admin/import";
    }

    @PostMapping("/import")
    public String importCsv(@RequestParam MultipartFile file,
                            @RequestParam TaskType taskType,
                            RedirectAttributes redirectAttributes) {
        try {
            int imported = datasetService.importCsv(file, taskType);
            int assignments = assignmentService.assignAllItemsToAnnotators(3);
            redirectAttributes.addFlashAttribute("message", imported + " rows imported; " + assignments + " assignments created.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/import";
    }

    @GetMapping("/labels")
    public String labels(Model model) {
        model.addAttribute("labels", datasetService.findAllLabels());
        model.addAttribute("taskTypes", TaskType.values());
        return "admin/labels";
    }

    @PostMapping("/labels")
    public String createLabel(@RequestParam String name,
                              @RequestParam TaskType taskType,
                              RedirectAttributes redirectAttributes) {
        datasetService.createLabel(name, taskType);
        redirectAttributes.addFlashAttribute("message", "Label saved.");
        return "redirect:/admin/labels";
    }

    @PostMapping("/labels/{id}/delete")
    public String deleteLabel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            datasetService.deleteLabel(id);
            redirectAttributes.addFlashAttribute("message", "Label deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "This label is already used by annotations and cannot be deleted.");
        }
        return "redirect:/admin/labels";
    }
}
