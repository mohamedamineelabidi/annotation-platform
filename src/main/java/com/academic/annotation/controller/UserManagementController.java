package com.academic.annotation.controller;

import com.academic.annotation.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class UserManagementController {

    private final UserService userService;

    public UserManagementController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String users(Model model) {
        model.addAttribute("users", userService.findAnnotators());
        return "admin/users";
    }

    @PostMapping
    public String create(@RequestParam String username,
                         @RequestParam String password,
                         @RequestParam(defaultValue = "false") boolean enabled,
                         RedirectAttributes redirectAttributes) {
        try {
            userService.saveAnnotator(null, username, password, enabled);
            redirectAttributes.addFlashAttribute("message", "Annotator created.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String username,
                         @RequestParam(required = false) String password,
                         @RequestParam(defaultValue = "false") boolean enabled,
                         RedirectAttributes redirectAttributes) {
        try {
            userService.saveAnnotator(id, username, password, enabled);
            redirectAttributes.addFlashAttribute("message", "Annotator updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteAnnotator(id);
            redirectAttributes.addFlashAttribute("message", "Annotator deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
