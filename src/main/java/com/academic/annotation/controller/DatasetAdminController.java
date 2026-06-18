package com.academic.annotation.controller;

import com.academic.annotation.model.Dataset;
import com.academic.annotation.model.DatasetItem;
import com.academic.annotation.model.TaskType;
import com.academic.annotation.model.User;
import com.academic.annotation.service.DatasetService;
import com.academic.annotation.service.MetricsService;
import com.academic.annotation.service.TaskService;
import com.academic.annotation.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/datasets")
public class DatasetAdminController {

    private static final int PAGE_SIZE = 20;

    private final DatasetService datasetService;
    private final TaskService taskService;
    private final MetricsService metricsService;
    private final UserService userService;

    public DatasetAdminController(DatasetService datasetService,
                                  TaskService taskService,
                                  MetricsService metricsService,
                                  UserService userService) {
        this.datasetService = datasetService;
        this.taskService = taskService;
        this.metricsService = metricsService;
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("datasets", datasetService.listDatasetSummaries());
        return "admin/datasets";
    }

    @GetMapping("/new")
    public String newDataset(Model model) {
        model.addAttribute("taskTypes", TaskType.values());
        return "admin/dataset-new";
    }

    @PostMapping
    public String create(@RequestParam("file") MultipartFile file,
                         @RequestParam String name,
                         @RequestParam String classes,
                         @RequestParam(required = false) String description,
                         @RequestParam TaskType taskType,
                         Principal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Un fichier CSV est requis.");
            }
            Dataset dataset = datasetService.createDataset(file, name, classes, description, taskType,
                    principal.getName());
            redirectAttributes.addFlashAttribute("message", "Dataset « " + dataset.getName() + " » créé.");
            return "redirect:/admin/datasets/" + dataset.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/datasets/new";
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id,
                          @RequestParam(defaultValue = "0") int page,
                          Model model) {
        Dataset dataset = datasetService.getDataset(id);
        List<DatasetItem> items = datasetService.itemsOf(dataset);
        long size = items.size();
        int totalPages = Math.max(1, (int) Math.ceil(size / (double) PAGE_SIZE));
        int current = Math.max(0, Math.min(page, totalPages - 1));
        int from = current * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, items.size());
        List<DatasetItem> pageItems = from >= items.size() ? List.of() : items.subList(from, to);

        model.addAttribute("dataset", dataset);
        model.addAttribute("size", size);
        model.addAttribute("progress", datasetService.progressOf(dataset));
        model.addAttribute("classes", datasetService.classesOf(dataset));
        model.addAttribute("items", pageItems);
        model.addAttribute("annotators", taskService.assignedAnnotators(dataset));
        model.addAttribute("page", current);
        model.addAttribute("totalPages", totalPages);
        return "admin/dataset-details";
    }

    @GetMapping("/{id}/assign")
    public String assignForm(@PathVariable Long id, Model model) {
        Dataset dataset = datasetService.getDataset(id);
        List<Long> assignedIds = new ArrayList<>();
        taskService.assignedAnnotators(dataset).forEach(a -> assignedIds.add(a.annotatorId()));
        List<User> annotators = userService.findAnnotators().stream()
                .filter(User::isEnabled)
                .toList();
        model.addAttribute("dataset", dataset);
        model.addAttribute("annotators", annotators);
        model.addAttribute("assignedIds", assignedIds);
        return "admin/dataset-assign";
    }

    @PostMapping("/{id}/assign")
    public String assign(@PathVariable Long id,
                         @RequestParam(required = false) List<Long> annotatorIds,
                         @RequestParam(required = false) String deadline,
                         RedirectAttributes redirectAttributes) {
        try {
            Dataset dataset = datasetService.getDataset(id);
            if (annotatorIds == null || annotatorIds.isEmpty()) {
                throw new IllegalArgumentException("Sélectionnez au moins un annotateur.");
            }
            LocalDate parsedDeadline = parseDeadline(deadline);
            List<User> annotators = new ArrayList<>();
            for (Long annotatorId : annotatorIds) {
                annotators.add(userService.findById(annotatorId));
            }
            taskService.assignAnnotators(dataset, annotators, parsedDeadline);
            redirectAttributes.addFlashAttribute("message", "Annotateurs affectés.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/datasets/" + id;
    }

    @PostMapping("/{id}/annotators/{annotatorId}/remove")
    public String removeAnnotator(@PathVariable Long id,
                                  @PathVariable Long annotatorId,
                                  RedirectAttributes redirectAttributes) {
        try {
            Dataset dataset = datasetService.getDataset(id);
            User annotator = userService.findById(annotatorId);
            taskService.removeAnnotator(dataset, annotator);
            redirectAttributes.addFlashAttribute("message",
                    "Annotateur retiré. Les annotations déjà réalisées sont conservées.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/datasets/" + id;
    }

    @GetMapping("/{id}/metrics")
    public String metrics(@PathVariable Long id, Model model) {
        Dataset dataset = datasetService.getDataset(id);
        model.addAttribute("dataset", dataset);
        model.addAttribute("metrics", metricsService.metricsFor(dataset));
        return "admin/dataset-metrics";
    }

    private LocalDate parseDeadline(String deadline) {
        if (deadline == null || deadline.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(deadline.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date limite invalide (format attendu : AAAA-MM-JJ).");
        }
    }
}
