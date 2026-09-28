package zw.gov.dpdms.webclient.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import zw.gov.dpdms.webclient.service.GatewayClient;
import zw.gov.dpdms.webclient.util.SessionKeys;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AuthWebController {

    private final GatewayClient gatewayClient;

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute(SessionKeys.TOKEN) != null) {
            return "redirect:/incidents";
        }
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                           @RequestParam String password,
                           HttpSession session,
                           Model model) {
        try {
            Map<String, Object> response = gatewayClient.login(username, password);

            session.setAttribute(SessionKeys.TOKEN, response.get("token"));
            session.setAttribute(SessionKeys.USERNAME, response.get("username"));
            session.setAttribute(SessionKeys.ROLE, response.get("role"));
            session.setAttribute(SessionKeys.HAZARD, response.get("hazard"));
            session.setAttribute(SessionKeys.WARD, response.get("ward"));

            return "redirect:/incidents";
        } catch (HttpStatusCodeException ex) {
            model.addAttribute("error", gatewayClient.extractErrorMessage(ex));
            return "login";
        } catch (Exception ex) {
            model.addAttribute("error", "Could not reach the login service. Is the gateway running?");
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
