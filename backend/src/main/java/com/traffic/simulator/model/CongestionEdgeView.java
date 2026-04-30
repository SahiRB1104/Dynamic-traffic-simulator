package com.traffic.simulator.model;

import com.traffic.simulator.geo.model.Node;

public record CongestionEdgeView(
        Node fromNode,
        Node toNode,
        double distanceKm,
        double congestionWeight,
        CongestionLevel level) {}