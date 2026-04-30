package com.traffic.simulator.model;

public record TrafficEdgeState(String source, String destination, int weight, String congestion) {}
