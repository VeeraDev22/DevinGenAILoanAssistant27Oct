package com.example.loanassistant.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        boolean staff = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_OFFICER") || a.equals("ROLE_ADMIN"));
        return staff ? "redirect:/officer/queue" : "redirect:/applications";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
