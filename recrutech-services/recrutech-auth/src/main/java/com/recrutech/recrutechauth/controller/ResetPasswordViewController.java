package com.recrutech.recrutechauth.controller;

import com.recrutech.recrutechauth.model.User;
import com.recrutech.recrutechauth.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/api/auth")
public class ResetPasswordViewController {

    private final UserRepository userRepository;

    public ResetPasswordViewController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam("token") String token,
                                    @RequestParam("email") String email,
                                    Model model) {
        model.addAttribute("token", token);
        model.addAttribute("email", email);

        boolean invalid = true;
        if (token != null && !token.isBlank()) {
            User user = userRepository.findByPasswordResetToken(token).orElse(null);
            if (user != null && user.isEnabled() && user.getPasswordResetExpiry() != null
                    && user.getPasswordResetExpiry().isAfter(LocalDateTime.now())) {
                invalid = false;
            }
        }
        model.addAttribute("invalidToken", invalid);
        if (invalid) {
            model.addAttribute("errorMessage", "Der Zurücksetzungslink ist ungültig oder abgelaufen. Fordern Sie bitte einen neuen Link an.");
        }
        return "reset-password-form";
    }
}
