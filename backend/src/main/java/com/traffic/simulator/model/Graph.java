package com.traffic.simulator.model;

import java.util.*;

public class Graph {
  private final Map<Node, List<Edge>> adjList = new HashMap<>();

  public void addNode(Node node) {
    if (node == null) {
      throw new IllegalArgumentException("Node cannot be null");
    }
    adjList.putIfAbsent(node, new ArrayList<>());
  }

  public void addEdge(Node src, Node dest, int weight) {
    if (src == null || dest == null) {
      throw new IllegalArgumentException("Source and destination are required");
    }
    if (!adjList.containsKey(src)) {
      throw new IllegalArgumentException("Source node does not exist: " + src.getName());
    }
    if (!adjList.containsKey(dest)) {
      throw new IllegalArgumentException("Destination node does not exist: " + dest.getName());
    }

    adjList.get(src).add(new Edge(dest, weight));
  }

  public Map<Node, List<Edge>> getAdjList() {
    return Collections.unmodifiableMap(adjList);
  }
}
