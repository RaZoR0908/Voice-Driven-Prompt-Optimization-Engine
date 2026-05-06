package com.razer.engine.memory;

import com.razer.engine.model.Memory;
import com.razer.engine.model.MemoryEdge;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class MemoryGraph {

    public GraphPayload build(List<Memory> memories, List<MemoryEdge> edges) {
        List<Node> nodes = memories.stream()
                .map(memory -> new Node(memory.getId(), memory.getTask(), memory.getDomain(), safeInt(memory.getUseCount())))
                .toList();

        List<Edge> graphEdges = edges.stream()
                .map(edge -> new Edge(edge.getFromMemory() == null ? null : edge.getFromMemory().getId(),
                        edge.getToMemory() == null ? null : edge.getToMemory().getId(),
                        edge.getEdgeType(),
                        edge.getScore()))
                .toList();

        return new GraphPayload(nodes, graphEdges);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    public record GraphPayload(List<Node> nodes, List<Edge> edges) {
    }

    public record Node(UUID id, String label, String domain, int useCount) {
    }

    public record Edge(UUID from, UUID to, String edgeType, Double score) {
    }
}