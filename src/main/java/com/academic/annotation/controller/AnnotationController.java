package com.academic.annotation.controller;

import com.academic.annotation.model.Assignment;
import com.academic.annotation.model.User;
import com.academic.annotation.service.AnnotationService;
import com.academic.annotation.service.DatasetService;
import com.academic.annotation.service.TaskService;
import com.academic.annotation.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/annotator")
public class AnnotationController {

    private final AnnotationService annotationService;
    private final DatasetService datasetService;
    private final UserService userService;
    private final TaskService taskService;

    public AnnotationController(AnnotationService annotationService,
                                DatasetService datasetService,
                                UserService userService,
                                TaskService taskService) {
        this.annotationService = annotationService;
        this.datasetService = datasetService;
        this.userService = userService;
        this.taskService = taskService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("stats", taskService.personalStats(user));
        model.addAttribute("nextTask", taskService.firstIncompleteTask(user).orElse(null));
        return "annotator/dashboard";
    }

    @GetMapping("/annotate")
    public String annotate(@RequestParam(required = false) Long itemId,
                           Principal principal,
                           Model model) {
        String username = principal.getName();
        List<Assignment> assignments = annotationService.assignedTo(username);
        Optional<Assignment> selected = itemId == null
                ? annotationService.firstPending(username)
                : annotationService.assignmentFor(username, itemId);
        if (selected.isEmpty() && !assignments.isEmpty()) {
            selected = Optional.of(assignments.get(0));
        }
        if (selected.isEmpty()) {
            model.addAttribute("assignments", assignments);
            return "annotator/annotate";
        }
        Assignment current = selected.get();
        int index = -1;
        for (int i = 0; i < assignments.size(); i++) {
            if (assignments.get(i).getDatasetItem().getId().equals(current.getDatasetItem().getId())) {
                index = i;
                break;
            }
        }
        model.addAttribute("assignment", current);
        model.addAttribute("assignments", assignments);
        model.addAttribute("labels", datasetService.findLabelsFor(current.getDatasetItem().getTaskType()));
        model.addAttribute("existingAnnotation", annotationService.existingAnnotation(username, current.getDatasetItem()).orElse(null));
        model.addAttribute("previousItemId", index > 0 ? assignments.get(index - 1).getDatasetItem().getId() : null);
        model.addAttribute("nextItemId", index >= 0 && index < assignments.size() - 1 ? assignments.get(index + 1).getDatasetItem().getId() : null);
        model.addAttribute("startedAtMillis", System.currentTimeMillis());
        return "annotator/annotate";
    }

    @PostMapping("/annotate")
    public String save(@RequestParam Long itemId,
                       @RequestParam Long labelId,
                       @RequestParam long startedAtMillis,
                       Principal principal,
                       RedirectAttributes redirectAttributes) {
        try {
            annotationService.saveAnnotation(principal.getName(), itemId, labelId, startedAtMillis);
            redirectAttributes.addFlashAttribute("message", "Annotation saved.");
            Optional<Assignment> next = annotationService.firstPending(principal.getName());
            return next.map(assignment -> "redirect:/annotator/annotate?itemId=" + assignment.getDatasetItem().getId())
                    .orElse("redirect:/annotator/dashboard");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/annotator/annotate?itemId=" + itemId;
        }
    }
}
