package com.aegisterra.platform.domain.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class WorkflowGraphTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String GRAPH = """
        {
          "initialStep": "START",
          "terminalSteps": ["DONE", "REJECTED"],
          "steps": [
            {"code":"START","name":"Start"},
            {"code":"REVIEW","name":"Review"},
            {"code":"DONE","name":"Done"},
            {"code":"REJECTED","name":"Rejected"}
          ],
          "transitions": [
            {"from":"START","to":"REVIEW","action":"SUBMIT"},
            {"from":"REVIEW","to":"DONE","action":"APPROVE"},
            {"from":"REVIEW","to":"REJECTED","action":"REJECT"}
          ]
        }
        """;

    @Test
    void parsesAndAllowsValidTransition() {
        WorkflowGraph graph = WorkflowGraph.parse(GRAPH, mapper);
        assertEquals("START", graph.initialStep());
        assertTrue(graph.findTransition("START", "submit").isPresent());
        graph.assertCanTransition("REVIEW", "APPROVE");
    }

    @Test
    void rejectsIllegalTransition() {
        WorkflowGraph graph = WorkflowGraph.parse(GRAPH, mapper);
        assertThrows(ResponseStatusException.class, () -> graph.assertCanTransition("START", "APPROVE"));
    }

    @Test
    void rejectsInvalidGraph() {
        assertThrows(ResponseStatusException.class,
            () -> WorkflowGraph.parse("{\"initialStep\":\"X\",\"terminalSteps\":[],\"steps\":[],\"transitions\":[]}", mapper));
    }
}
