package com.traffic.simulator.model;

import java.util.List;

public record RouteResult(int distance, List<String> path, String algorithm) {}
