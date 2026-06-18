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
    public String create(@RequestParam(required = false) String firstName,
                         @RequestParam(required = false) String lastName,
                         @RequestParam String username,
                         RedirectAttributes redirectAttributes) {
        try {
            var account = userService.createAnnotatorWithGeneratedPassword(firstName, lastName, username);
            redirectAttributes.addFlashAttribute("message",
                    "Annotateur créé. Identifiant : " + account.login()
                            + " — Mot de passe : " + account.password()
                            + " (à communiquer maintenant, il ne sera plus affiché).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String username,
                         @RequestParam(required = false) String firstName,
                         @RequestParam(required = false) String lastName,
                         @RequestParam(required = false) String password,
                         @RequestParam(defaultValue = "false") boolean enabled,
                         RedirectAttributes redirectAttributes) {
        try {
            userService.saveAnnotator(id, username, firstName, lastName, password, enabled);
            redirectAttributes.addFlashAttribute("message", "Annotateur mis à jour.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deactivateAnnotator(id);
            redirectAttributes.addFlashAttribute("message",
                    "Annotateur désactivé. Ses annotations sont conservées.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/reactivate")
    public String reactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.reactivateAnnotator(id);
            redirectAttributes.addFlashAttribute("message", "Annotateur réactivé.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
