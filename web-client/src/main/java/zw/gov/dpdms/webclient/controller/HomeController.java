package zw.gov.dpdms.webclient.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import zw.gov.dpdms.webclient.util.SessionKeys;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(HttpSession session) {
        return session.getAttribute(SessionKeys.TOKEN) != null
                ? "redirect:/incidents"
                : "redirect:/login";
    }
}
