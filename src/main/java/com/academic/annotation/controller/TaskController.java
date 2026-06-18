package com.academic.annotation.controller;

import com.academic.annotation.model.Annotation;
import com.academic.annotation.model.Label;
import com.academic.annotation.model.Task;
import com.academic.annotation.model.TaskItem;
import com.academic.annotation.model.User;
import com.academic.annotation.repository.AnnotationRepository;
import com.academic.annotation.service.DatasetService;
import com.academic.annotation.service.TaskService;
import com.academic.annotation.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/annotator/tasks")
public class TaskController {

    private final TaskService taskService;
    private final DatasetService datasetService;
    private final UserService userService;
    private final AnnotationRepository annotationRepository;

    public TaskController(TaskService taskService,
                          DatasetService datasetService,
                          UserService userService,
                          AnnotationRepository annotationRepository) {
        this.taskService = taskService;
        this.datasetService = datasetService;
        this.userService = userService;
        this.annotationRepository = annotationRepository;
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("tasks", taskService.tasksFor(user));
        return "annotator/tasks";
    }

    @GetMapping("/{id}/work")
    public String work(@PathVariable Long id,
                       @RequestParam(defaultValue = "0") int index,
                       Principal principal,
                       Model model) {
        Task task = taskService.getTask(id);
        if (!task.getAnnotator().getUsername().equals(principal.getName())) {
            return "redirect:/annotator/tasks";
        }
        List<TaskItem> items = taskService.itemsOf(task);
        if (items.isEmpty()) {
            model.addAttribute("task", task);
            model.addAttribute("empty", true);
            return "annotator/task-work";
        }
        int current = Math.max(0, Math.min(index, items.size() - 1));
        TaskItem taskItem = items.get(current);

        String existingLabel = annotationRepository
                .findByDatasetItemAndAnnotator(taskItem.getDatasetItem(), task.getAnnotator())
                .map(Annotation::getLabel)
                .map(Label::getName)
                .orElse(null);

        model.addAttribute("task", task);
        model.addAttribute("taskItem", taskItem);
        model.addAttribute("item", taskItem.getDatasetItem());
        model.addAttribute("labels", datasetService.classesOf(task.getDataset()));
        model.addAttribute("existingLabel", existingLabel);
        model.addAttribute("index", current);
        model.addAttribute("total", items.size());
        model.addAttribute("previousIndex", current > 0 ? current - 1 : null);
        model.addAttribute("nextIndex", current < items.size() - 1 ? current + 1 : null);
        return "annotator/task-work";
    }

    @PostMapping("/{id}/work")
    public String save(@PathVariable Long id,
                       @RequestParam Long taskItemId,
                       @RequestParam String label,
                       @RequestParam int index,
                       Principal principal,
                       RedirectAttributes redirectAttributes) {
        Task task = taskService.getTask(id);
        if (!task.getAnnotator().getUsername().equals(principal.getName())) {
            return "redirect:/annotator/tasks";
        }
        try {
            taskService.annotate(task, taskItemId, label);
            int total = (int) taskService.sizeOf(task);
            int next = index < total - 1 ? index + 1 : index;
            return "redirect:/annotator/tasks/" + id + "/work?index=" + next;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/annotator/tasks/" + id + "/work?index=" + index;
        }
    }
}
