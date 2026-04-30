package com.traffic.simulator.model;

import java.util.Objects;

public final class Node {

  private final String name;

  public Node(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Node name cannot be blank");
    }
    this.name = name.trim().toUpperCase();
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Node other)) {
      return false;
    }
    return Objects.equals(name, other.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return name;
  }
}
