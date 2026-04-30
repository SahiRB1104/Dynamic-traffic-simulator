package com.traffic.simulator.model;

import java.util.List;

public record TrafficUpdateEvent(String timestamp, List<TrafficEdgeState> edges) {}
