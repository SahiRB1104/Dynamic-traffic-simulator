package com.traffic.simulator.model;

public class Edge {

  private final Node destination;
  private int weight;

  public Edge(Node destination, int weight) {
    if (destination == null) {
      throw new IllegalArgumentException("Edge destination is required");
    }
    validateWeight(weight);
    this.destination = destination;
    this.weight = weight;
  }

  public Node getDestination() {
    return destination;
  }

  public int getWeight() {
    return weight;
  }

  public void setWeight(int weight) {
    validateWeight(weight);
    this.weight = weight;
  }

  private void validateWeight(int weight) {
    if (weight < 1) {
      throw new IllegalArgumentException("Edge weight must be >= 1");
    }
  }
}
