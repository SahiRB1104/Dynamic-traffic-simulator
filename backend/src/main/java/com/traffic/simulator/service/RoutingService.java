package com.traffic.simulator.service;

import com.traffic.simulator.model.Algorithm;
import com.traffic.simulator.model.Edge;
import com.traffic.simulator.model.Graph;
import com.traffic.simulator.model.Node;
import com.traffic.simulator.model.RouteResult;
import com.traffic.simulator.model.TrafficEdgeState;
import com.traffic.simulator.model.TrafficUpdateEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RoutingService {

	private final Graph graph = new Graph();
	private final Map<String, Node> nodesByName = new HashMap<>();
	private final Random random = new Random();

	public RoutingService() {
		initializeGraph();
	}

	public synchronized RouteResult findRoute(String source, String destination, Algorithm algorithm) {
		Node sourceNode = resolveNode(source);
		Node destinationNode = resolveNode(destination);
		Algorithm selected = algorithm == null ? Algorithm.DIJKSTRA : algorithm;

		return switch (selected) {
			case DIJKSTRA -> runDijkstra(sourceNode, destinationNode, selected);
			case ASTAR -> runAStar(sourceNode, destinationNode, selected);
		};
	}

	public synchronized TrafficUpdateEvent randomizeTraffic() {
		for (Map.Entry<Node, List<Edge>> entry : graph.getAdjList().entrySet()) {
			for (Edge edge : entry.getValue()) {
				int delta = random.nextInt(5) - 2;
				int nextWeight = Math.max(1, edge.getWeight() + delta);
				edge.setWeight(nextWeight);
			}
		}

		return currentTrafficState();
	}

	public synchronized TrafficUpdateEvent currentTrafficState() {
		List<TrafficEdgeState> edgeStates = new ArrayList<>();
		for (Map.Entry<Node, List<Edge>> entry : graph.getAdjList().entrySet()) {
			Node source = entry.getKey();
			for (Edge edge : entry.getValue()) {
				edgeStates.add(
						new TrafficEdgeState(
								source.getName(),
								edge.getDestination().getName(),
								edge.getWeight(),
								congestionLabel(edge.getWeight())));
			}
		}

		return new TrafficUpdateEvent(Instant.now().toString(), edgeStates);
	}

	public Collection<String> getAvailableNodes() {
		return Collections.unmodifiableSet(new HashSet<>(nodesByName.keySet()));
	}

	private String congestionLabel(int weight) {
		if (weight <= 3) {
			return "LOW";
		}
		if (weight <= 6) {
			return "MEDIUM";
		}
		return "HIGH";
	}

	private RouteResult runDijkstra(Node source, Node target, Algorithm algorithm) {
		Map<Node, Integer> dist = new HashMap<>();
		Map<Node, Node> prev = new HashMap<>();
		PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingInt(NodeDistance::distance));

		for (Node node : graph.getAdjList().keySet()) {
			dist.put(node, Integer.MAX_VALUE);
		}

		dist.put(source, 0);
		pq.add(new NodeDistance(source, 0));

		while (!pq.isEmpty()) {
			NodeDistance current = pq.poll();
			if (current.distance() > dist.get(current.node())) {
				continue;
			}

			if (current.node().equals(target)) {
				break;
			}

			for (Edge edge : graph.getAdjList().getOrDefault(current.node(), List.of())) {
				int newDist = dist.get(current.node()) + edge.getWeight();
				if (newDist < dist.getOrDefault(edge.getDestination(), Integer.MAX_VALUE)) {
					dist.put(edge.getDestination(), newDist);
					prev.put(edge.getDestination(), current.node());
					pq.add(new NodeDistance(edge.getDestination(), newDist));
				}
			}
		}

		return buildResult(source, target, dist, prev, algorithm);
	}

	private RouteResult runAStar(Node source, Node target, Algorithm algorithm) {
		Map<Node, Integer> gScore = new HashMap<>();
		Map<Node, Integer> fScore = new HashMap<>();
		Map<Node, Node> prev = new HashMap<>();
		Set<Node> closed = new HashSet<>();
		PriorityQueue<NodeDistance> openSet = new PriorityQueue<>(Comparator.comparingInt(NodeDistance::distance));

		for (Node node : graph.getAdjList().keySet()) {
			gScore.put(node, Integer.MAX_VALUE);
			fScore.put(node, Integer.MAX_VALUE);
		}

		gScore.put(source, 0);
		fScore.put(source, heuristic(source, target));
		openSet.add(new NodeDistance(source, fScore.get(source)));

		while (!openSet.isEmpty()) {
			Node current = openSet.poll().node();
			if (current.equals(target)) {
				break;
			}

			if (!closed.add(current)) {
				continue;
			}

			for (Edge edge : graph.getAdjList().getOrDefault(current, List.of())) {
				Node neighbor = edge.getDestination();
				int tentative = gScore.get(current) + edge.getWeight();
				if (tentative < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
					prev.put(neighbor, current);
					gScore.put(neighbor, tentative);
					int total = tentative + heuristic(neighbor, target);
					fScore.put(neighbor, total);
					openSet.add(new NodeDistance(neighbor, total));
				}
			}
		}

		return buildResult(source, target, gScore, prev, algorithm);
	}

	private int heuristic(Node source, Node destination) {
		return Math.abs(source.getName().charAt(0) - destination.getName().charAt(0));
	}

	private RouteResult buildResult(
			Node source,
			Node target,
			Map<Node, Integer> dist,
			Map<Node, Node> prev,
			Algorithm algorithm) {
		Integer totalDistance = dist.get(target);
		if (totalDistance == null || totalDistance == Integer.MAX_VALUE) {
			throw new IllegalArgumentException(
					"No route found between " + source.getName() + " and " + target.getName());
		}

		List<String> path = new ArrayList<>();
		Node step = target;
		while (step != null) {
			path.add(step.getName());
			step = prev.get(step);
		}
		Collections.reverse(path);

		if (!path.get(0).equals(source.getName())) {
			throw new IllegalArgumentException(
					"No route found between " + source.getName() + " and " + target.getName());
		}

		return new RouteResult(totalDistance, path, algorithm.name());
	}

	private Node resolveNode(String name) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Node name is required");
		}

		Node node = nodesByName.get(name.trim().toUpperCase());
		if (node == null) {
			String supported = nodesByName.keySet().stream().sorted().collect(Collectors.joining(", "));
			throw new IllegalArgumentException("Unknown node '" + name + "'. Supported nodes: " + supported);
		}

		return node;
	}

	private void initializeGraph() {
		Node a = createNode("A");
		Node b = createNode("B");
		Node c = createNode("C");
		Node d = createNode("D");

		graph.addEdge(a, b, 4);
		graph.addEdge(a, c, 2);
		graph.addEdge(c, b, 1);
		graph.addEdge(b, d, 4);
		graph.addEdge(c, d, 8);
	}

	private Node createNode(String name) {
		Node node = new Node(name);
		nodesByName.put(node.getName(), node);
		graph.addNode(node);
		return node;
	}

	private record NodeDistance(Node node, int distance) {}
}
