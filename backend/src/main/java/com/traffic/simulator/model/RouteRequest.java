package com.traffic.simulator.model;

public record RouteRequest(
	Double sourceLat,
	Double sourceLon,
	Double destLat,
	Double destLon,
	String algorithm) {}
