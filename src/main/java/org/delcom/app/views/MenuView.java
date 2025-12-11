package org.delcom.app.views;

import org.delcom.app.entities.User;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MenuView {

    @GetMapping("/menu")
    public String menu(Model model) {

        // Ambil user dari session security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Jika belum login → redirect ke logout
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/auth/logout";
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            return "redirect:/auth/logout";
        }

        User authUser = (User) principal;

        // Kirim data user ke UI
        model.addAttribute("auth", authUser);

        // Kembalikan halaman menu
        // Kamu bilang ingin: return to models/home
        return "models/home";
    }
}
