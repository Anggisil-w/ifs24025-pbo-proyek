package org.delcom.app.views;

import org.delcom.app.entities.User;
import org.delcom.app.utils.ConstUtil;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeView {


    @GetMapping("/")
    public String home(Model model) {
        // Autentikasi
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if ((authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/auth/login";
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            return "redirect:/auth/login";
        }

        User authUser = (User) principal;
        model.addAttribute("auth", authUser);

        
        return ConstUtil.TEMPLATE_PAGES_HOME;
    }
}
