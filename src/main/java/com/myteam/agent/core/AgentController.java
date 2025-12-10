package com.myteam.agent.core;

import com.myteam.agent.core.dto.AgentRequest;
import com.myteam.agent.core.dto.AgentResponse;
import org.example.model.User;
import org.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    @Autowired
    private AgentService agentService;

    @Autowired
    private UserService userService;

    @PostMapping("/execute")
    public ResponseEntity<AgentResponse> execute(@RequestBody AgentRequest request, HttpServletRequest httpRequest) {
        // Get authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String username = auth.getName();
        User user = userService.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        request.setActor(username);

        // Extract role
        String role = "EMPLOYEE"; // default
        if (user.getRoles() != null && user.getRoles().length > 0) {
            role = user.getRoles()[0].replaceAll("[{}\" ]", "").toUpperCase();
        }
        request.setRole(role);

        // Set sessionId from session
        request.setSessionId(httpRequest.getSession().getId());

        // Generate correlationId if not set
        if (request.getCorrelationId() == null) {
            request.setCorrelationId(java.util.UUID.randomUUID().toString());
        }

        AgentResponse response = agentService.execute(request);
        return ResponseEntity.ok(response);
    }
}
