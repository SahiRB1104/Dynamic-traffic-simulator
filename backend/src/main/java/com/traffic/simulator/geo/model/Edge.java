package com.traffic.simulator.geo.model;

import java.util.Objects;

public class Edge {

    private final Node fromNode;
    private final Node toNode;
    // kilometers
    private double distanceKm;
    // multiplicative congestion weight. 1.0 = no congestion, >1 = slower
    private double congestionWeight;

    public Edge(Node fromNode, Node toNode, double distanceKm, double congestionWeight) {
        if (fromNode == null || toNode == null) {
            throw new IllegalArgumentException("fromNode and toNode are required");
        }
        this.fromNode = fromNode;
        this.toNode = toNode;
        setDistanceKm(distanceKm);
        setCongestionWeight(congestionWeight);
    }

    public Node getFromNode() {
        return fromNode;
    }

    public Node getToNode() {
        return toNode;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        if (distanceKm <= 0) throw new IllegalArgumentException("distanceKm must be > 0");
        this.distanceKm = distanceKm;
    }

    public double getCongestionWeight() {
        return congestionWeight;
    }

    public void setCongestionWeight(double congestionWeight) {
        if (congestionWeight <= 0) throw new IllegalArgumentException("congestionWeight must be > 0");
        this.congestionWeight = congestionWeight;
    }

    public double effectiveCost() {
        return distanceKm * congestionWeight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Edge)) return false;
        Edge edge = (Edge) o;
        return Objects.equals(fromNode, edge.fromNode) && Objects.equals(toNode, edge.toNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromNode, toNode);
    }
}
