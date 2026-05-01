package com.traffic.simulator.model;

public record RouteRequest(
	String source,
	Double sourceLatitude,
	Double sourceLongitude,
	String destination,
	Double destinationLatitude,
	Double destinationLongitude,
	String algorithm) {}
