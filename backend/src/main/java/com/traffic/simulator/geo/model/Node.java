package com.traffic.simulator.geo.model;

import java.util.Objects;
import java.util.UUID;

public final class Node {

    private final String id;
    private final String name;
    private final double latitude;
    private final double longitude;

    public Node(String id, String name, double latitude, double longitude) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        this.name = name.trim();
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Node(String name, double latitude, double longitude) {
        this(null, name, latitude, longitude);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;
        Node node = (Node) o;
        return Objects.equals(id, node.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + "(" + id + ")";
    }
}
