package com.example.xapi.api;

import com.example.xapi.dto.AgentAskRequest;
import com.example.xapi.dto.AgentAskResponse;
import com.example.xapi.service.AgentProxyService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentProxyService agentProxyService;

    public AgentController(AgentProxyService agentProxyService) {
        this.agentProxyService = agentProxyService;
    }

    @PostMapping("/ask")
    public AgentAskResponse ask(@RequestBody AgentAskRequest request) {
        return agentProxyService.ask(request);
    }
}
