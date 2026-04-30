package com.traffic.simulator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "edges")
public class EdgeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_node_id", nullable = false)
    private NodeEntity fromNode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_node_id", nullable = false)
    private NodeEntity toNode;

    @Column(name = "distance_km", nullable = false)
    private double distanceKm;

    @Column(name = "congestion_weight", nullable = false)
    private double congestionWeight = 1.0;

    public EdgeEntity() {
    }

    public EdgeEntity(Long id, NodeEntity fromNode, NodeEntity toNode, double distanceKm, double congestionWeight) {
        this.id = id;
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.distanceKm = distanceKm;
        this.congestionWeight = congestionWeight;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NodeEntity getFromNode() {
        return fromNode;
    }

    public void setFromNode(NodeEntity fromNode) {
        this.fromNode = fromNode;
    }

    public NodeEntity getToNode() {
        return toNode;
    }

    public void setToNode(NodeEntity toNode) {
        this.toNode = toNode;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public double getCongestionWeight() {
        return congestionWeight;
    }

    public void setCongestionWeight(double congestionWeight) {
        this.congestionWeight = congestionWeight;
    }
}
