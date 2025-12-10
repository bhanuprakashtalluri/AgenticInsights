package com.myteam.agent.core;

import com.myteam.agent.core.dto.AgentRequest;
import com.myteam.agent.core.dto.AgentResponse;

public interface AgentService {

    AgentResponse execute(AgentRequest request);
}
