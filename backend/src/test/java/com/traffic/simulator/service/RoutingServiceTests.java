package com.traffic.simulator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.traffic.simulator.model.Algorithm;
import com.traffic.simulator.model.RouteResult;
import com.traffic.simulator.model.TrafficUpdateEvent;
import org.junit.jupiter.api.Test;

class RoutingServiceTests {

  private final RoutingService routingService = new RoutingService();

  @Test
  void findsShortestPathWithDijkstra() {
    RouteResult result = routingService.findRoute("A", "D", Algorithm.DIJKSTRA);

    assertEquals(7, result.distance());
    assertEquals("DIJKSTRA", result.algorithm());
    assertEquals("A", result.path().getFirst());
    assertEquals("D", result.path().getLast());
  }

  @Test
  void findsPathWithAStar() {
    RouteResult result = routingService.findRoute("A", "D", Algorithm.ASTAR);

    assertEquals(7, result.distance());
    assertEquals("ASTAR", result.algorithm());
    assertEquals("A", result.path().getFirst());
    assertEquals("D", result.path().getLast());
  }

  @Test
  void exposesTrafficState() {
    TrafficUpdateEvent event = routingService.currentTrafficState();

    assertFalse(event.edges().isEmpty());
  }
}
