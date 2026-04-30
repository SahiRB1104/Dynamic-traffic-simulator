package com.traffic.simulator.model;

import com.traffic.simulator.geo.model.Node;
import java.util.List;

public record RouteResponse(List<Node> path, double totalDistance, long duration) {}
