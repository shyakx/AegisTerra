package com.aegisterra.platform.domain.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Immutable parsed workflow graph from a published definition version.
 */
public final class WorkflowGraph {

    private final String initialStep;
    private final Set<String> terminalSteps;
    private final Map<String, StepDefinition> steps;
    private final List<TransitionEdge> transitions;

    private WorkflowGraph(
        String initialStep,
        Set<String> terminalSteps,
        Map<String, StepDefinition> steps,
        List<TransitionEdge> transitions
    ) {
        this.initialStep = initialStep;
        this.terminalSteps = Set.copyOf(terminalSteps);
        this.steps = Map.copyOf(steps);
        this.transitions = List.copyOf(transitions);
    }

    public static WorkflowGraph parse(String graphJson, ObjectMapper mapper) {
        try {
            JsonNode root = mapper.readTree(graphJson);
            String initial = requiredText(root, "initialStep");
            Set<String> terminals = new HashSet<>();
            for (JsonNode n : root.path("terminalSteps")) {
                terminals.add(n.asText());
            }
            if (terminals.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "terminalSteps required");
            }
            Map<String, StepDefinition> stepMap = new HashMap<>();
            for (JsonNode step : root.path("steps")) {
                String code = requiredText(step, "code");
                String name = step.path("name").asText(code);
                TaskConfig task = parseTask(step.path("task"));
                stepMap.put(code, new StepDefinition(code, name, task));
            }
            if (!stepMap.containsKey(initial)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "initialStep not declared in steps");
            }
            for (String terminal : terminals) {
                if (!stepMap.containsKey(terminal)) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "terminal step not declared: " + terminal);
                }
            }
            List<TransitionEdge> edges = new ArrayList<>();
            for (JsonNode t : root.path("transitions")) {
                String from = requiredText(t, "from");
                String to = requiredText(t, "to");
                String action = requiredText(t, "action");
                if (!stepMap.containsKey(from) || !stepMap.containsKey(to)) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "transition references unknown step: " + from + " -> " + to);
                }
                edges.add(new TransitionEdge(from, to, action.toUpperCase()));
            }
            return new WorkflowGraph(initial, terminals, stepMap, edges);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid workflow graph_json: " + ex.getMessage());
        }
    }

    private static TaskConfig parseTask(JsonNode taskNode) {
        if (taskNode == null || taskNode.isMissingNode() || taskNode.isNull()) {
            return null;
        }
        if (taskNode.has("enabled") && !taskNode.path("enabled").asBoolean(true)) {
            return null;
        }
        String taskType = taskNode.path("taskType").asText("GENERIC");
        String title = taskNode.path("title").asText("Workflow task");
        String description = taskNode.path("description").asText(null);
        List<AssigneeRule> assignees = new ArrayList<>();
        for (JsonNode a : taskNode.path("assignees")) {
            assignees.add(new AssigneeRule(
                AssignmentStrategyCode.parse(requiredText(a, "strategy")),
                requiredText(a, "value")
            ));
        }
        List<String> allowedDecisions = new ArrayList<>();
        for (JsonNode d : taskNode.path("allowedDecisions")) {
            if (!d.asText().isBlank()) {
                allowedDecisions.add(d.asText().trim().toUpperCase());
            }
        }
        return new TaskConfig(
            WorkflowTaskType.parse(taskType),
            title,
            description,
            List.copyOf(assignees),
            List.copyOf(allowedDecisions)
        );
    }

    public String initialStep() {
        return initialStep;
    }

    public boolean isTerminal(String stepCode) {
        return terminalSteps.contains(stepCode);
    }

    public String stepName(String stepCode) {
        StepDefinition step = steps.get(stepCode);
        return step == null ? stepCode : step.name();
    }

    public Optional<TaskConfig> taskConfig(String stepCode) {
        StepDefinition step = steps.get(stepCode);
        return step == null ? Optional.empty() : Optional.ofNullable(step.task());
    }

    public Optional<TransitionEdge> findTransition(String fromStep, String actionCode) {
        String action = actionCode.trim().toUpperCase();
        return transitions.stream()
            .filter(t -> t.from().equals(fromStep) && t.action().equals(action))
            .findFirst();
    }

    public List<String> actionsFrom(String fromStep) {
        return transitions.stream()
            .filter(t -> t.from().equals(fromStep))
            .map(TransitionEdge::action)
            .toList();
    }

    public void assertCanTransition(String fromStep, String actionCode) {
        if (findTransition(fromStep, actionCode).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Illegal workflow transition from step " + fromStep + " with action " + actionCode);
        }
    }

    private static String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.asText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Missing graph field: " + field);
        }
        return value.asText().trim();
    }

    public record TransitionEdge(String from, String to, String action) {}

    public record StepDefinition(String code, String name, TaskConfig task) {}

    public record TaskConfig(
        WorkflowTaskType taskType,
        String title,
        String description,
        List<AssigneeRule> assignees,
        List<String> allowedDecisions
    ) {}

    public record AssigneeRule(AssignmentStrategyCode strategy, String value) {}
}
