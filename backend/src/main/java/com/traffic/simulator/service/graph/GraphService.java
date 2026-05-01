package com.traffic.simulator.service.graph;

import com.traffic.simulator.geo.model.Edge;
import com.traffic.simulator.geo.model.Node;
import com.traffic.simulator.entity.EdgeEntity;
import com.traffic.simulator.entity.NodeEntity;
import com.traffic.simulator.repository.EdgeRepository;
import com.traffic.simulator.repository.NodeRepository;
import com.traffic.simulator.model.CongestionEdgeView;
import com.traffic.simulator.model.CongestionLevel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

@Service
public class GraphService {

    private static final Logger log = LoggerFactory.getLogger(GraphService.class);

    private final Map<String, Node> nodes = new HashMap<>();
    private final Map<String, List<Edge>> adj = new HashMap<>();
    private final HttpClient http = HttpClient.newHttpClient();

    private final NodeRepository nodeRepository;
    private final EdgeRepository edgeRepository;

    public GraphService(NodeRepository nodeRepository, EdgeRepository edgeRepository) {
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        // try to load nodes/edges from the database if present
        if (nodeRepository.count() > 0) {
            List<NodeEntity> persisted = nodeRepository.findAll();
            for (NodeEntity ne : persisted) {
                // use DB id as stable string id
                Node n = new Node(String.valueOf(ne.getId()), ne.getName(), ne.getLat(), ne.getLon());
                nodes.putIfAbsent(n.getId(), n);
                adj.putIfAbsent(n.getId(), new ArrayList<>());
            }

            List<EdgeEntity> edges = edgeRepository.findAll();
            for (EdgeEntity ee : edges) {
                String fromId = String.valueOf(ee.getFromNode().getId());
                String toId = String.valueOf(ee.getToNode().getId());
                Node from = nodes.get(fromId);
                Node to = nodes.get(toId);
                if (from != null && to != null) {
                    Edge e = new Edge(from, to, ee.getDistanceKm(), ee.getCongestionWeight());
                    adj.get(fromId).add(e);
                }
            }
            log.info("Graph initialized from DB with {} nodes", nodes.size());
            return;
        }

        // fallback small sample graph to get started
        Node a = addNode(new Node("A", 40.7128, -74.0060)); // example coords
        Node b = addNode(new Node("B", 40.7138, -74.0010));
        Node c = addNode(new Node("C", 40.7150, -74.0030));

        addEdge(a.getId(), b.getId());
        addEdge(b.getId(), c.getId());
        addEdge(a.getId(), c.getId());
        log.info("Graph initialized with {} nodes", nodes.size());
    }

    public Node addNode(Node node) {
        nodes.putIfAbsent(node.getId(), node);
        adj.putIfAbsent(node.getId(), new ArrayList<>());
        return node;
    }

    public void addEdge(String fromId, String toId) {
        Node from = resolveNode(fromId);
        Node to = resolveNode(toId);
        double km = fetchRoadDistanceKm(from, to);
        Edge e = new Edge(from, to, km, 1.0);
        adj.get(fromId).add(e);
    }

    public Node resolveNode(String idOrName) {
        Node n = nodes.get(idOrName);
        if (n != null) return n;
        // fallback: search by name
        for (Node node : nodes.values()) {
            if (node.getName().equalsIgnoreCase(idOrName)) return node;
        }
        throw new IllegalArgumentException("Unknown node: " + idOrName);
    }

    public Node resolveEndpoint(String name, Double latitude, Double longitude) {
        if (latitude != null && longitude != null && Double.isFinite(latitude) && Double.isFinite(longitude)) {
            return findNearestNode(latitude, longitude);
        }
        if (name != null && !name.isBlank()) {
            return resolveNode(name);
        }
        throw new IllegalArgumentException("Route endpoint must include a city name or coordinates");
    }

    private Node findNearestNode(double latitude, double longitude) {
        Node nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;

        for (Node node : nodes.values()) {
            double distance = haversineKm(latitude, longitude, node.getLatitude(), node.getLongitude());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = node;
            }
        }

        if (nearest == null) {
            throw new IllegalArgumentException("No routing nodes are available");
        }

        return nearest;
    }

    public Collection<Node> getAllNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public Map<String, List<Edge>> getAdjacencyList() {
        return Collections.unmodifiableMap(adj);
    }

    public List<CongestionEdgeView> snapshotCongestion() {
        List<CongestionEdgeView> snapshot = new ArrayList<>();
        for (List<Edge> edges : adj.values()) {
            for (Edge edge : edges) {
                snapshot.add(new CongestionEdgeView(
                        edge.getFromNode(),
                        edge.getToNode(),
                        edge.getDistanceKm(),
                        edge.getCongestionWeight(),
                        classifyCongestion(edge.getCongestionWeight())));
            }
        }
        return List.copyOf(snapshot);
    }

    // ----- OSRM call -----
    private double fetchRoadDistanceKm(Node a, Node b) {
        // OSRM expects lon,lat order
        String coords = String.format(Locale.ROOT, "%f,%f;%f,%f", a.getLongitude(), a.getLatitude(), b.getLongitude(), b.getLatitude());
        String url = "http://router.project-osrm.org/route/v1/driving/" + coords + "?overview=false";
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        try {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                // extract "distance": <number> from raw JSON without requiring Jackson
                Pattern p = Pattern.compile("\"distance\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
                Matcher m = p.matcher(res.body());
                if (m.find()) {
                    double meters = Double.parseDouble(m.group(1));
                    return meters / 1000.0;
                }
            }
        } catch (IOException | InterruptedException e) {
            log.warn("OSRM call failed: {}", e.toString());
            Thread.currentThread().interrupt();
        }
        // fallback: haversine as estimate
        return haversineKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
    }

    // ----- Dijkstra -----
    public PathResult dijkstra(String srcId, String destId) {
        Node src = resolveNode(srcId);
        Node dest = resolveNode(destId);

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        for (String id : nodes.keySet()) dist.put(id, Double.POSITIVE_INFINITY);
        dist.put(src.getId(), 0.0);

        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingDouble(nd -> nd.distance));
        pq.add(new NodeDistance(src.getId(), 0.0));

        while (!pq.isEmpty()) {
            NodeDistance cur = pq.poll();
            if (cur.distance > dist.get(cur.nodeId)) continue;
            if (cur.nodeId.equals(dest.getId())) break;
            List<Edge> edges = adj.getOrDefault(cur.nodeId, Collections.emptyList());
            for (Edge e : edges) {
                String nid = e.getToNode().getId();
                double alt = dist.get(cur.nodeId) + e.effectiveCost();
                if (alt < dist.get(nid)) {
                    dist.put(nid, alt);
                    prev.put(nid, cur.nodeId);
                    pq.add(new NodeDistance(nid, alt));
                }
            }
        }

        if (dist.get(dest.getId()).isInfinite()) return new PathResult(Double.POSITIVE_INFINITY, List.of());

        List<Node> path = new ArrayList<>();
        String cur = dest.getId();
        while (cur != null) {
            path.add(nodes.get(cur));
            cur = prev.get(cur);
        }
        Collections.reverse(path);
        return new PathResult(dist.get(dest.getId()), path);
    }

    // ----- A* -----
    public PathResult aStar(String srcId, String destId) {
        Node src = resolveNode(srcId);
        Node dest = resolveNode(destId);

        Map<String, Double> gScore = new HashMap<>();
        Map<String, Double> fScore = new HashMap<>();
        Map<String, String> prev = new HashMap<>();

        for (String id : nodes.keySet()) {
            gScore.put(id, Double.POSITIVE_INFINITY);
            fScore.put(id, Double.POSITIVE_INFINITY);
        }
        gScore.put(src.getId(), 0.0);
        fScore.put(src.getId(), heuristicKm(src, dest));

        PriorityQueue<NodeDistance> open = new PriorityQueue<>(Comparator.comparingDouble(nd -> nd.distance));
        open.add(new NodeDistance(src.getId(), fScore.get(src.getId())));

        while (!open.isEmpty()) {
            NodeDistance cur = open.poll();
            if (cur.nodeId.equals(dest.getId())) break;
            List<Edge> edges = adj.getOrDefault(cur.nodeId, Collections.emptyList());
            for (Edge e : edges) {
                String nid = e.getToNode().getId();
                double tentativeG = gScore.get(cur.nodeId) + e.effectiveCost();
                if (tentativeG < gScore.get(nid)) {
                    prev.put(nid, cur.nodeId);
                    gScore.put(nid, tentativeG);
                    double f = tentativeG + heuristicKm(nodes.get(nid), dest);
                    fScore.put(nid, f);
                    open.add(new NodeDistance(nid, f));
                }
            }
        }

        if (gScore.get(dest.getId()).isInfinite()) return new PathResult(Double.POSITIVE_INFINITY, List.of());

        List<Node> path = new ArrayList<>();
        String cur = dest.getId();
        while (cur != null) {
            path.add(nodes.get(cur));
            cur = prev.get(cur);
        }
        Collections.reverse(path);
        return new PathResult(gScore.get(dest.getId()), path);
    }

    private double heuristicKm(Node a, Node b) {
        return haversineKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double la1 = Math.toRadians(lat1);
        double la2 = Math.toRadians(lat2);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(la1) * Math.cos(la2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private CongestionLevel classifyCongestion(double congestionWeight) {
        if (congestionWeight < 1.25) {
            return CongestionLevel.LOW;
        }
        if (congestionWeight < 1.75) {
            return CongestionLevel.MEDIUM;
        }
        return CongestionLevel.HIGH;
    }

    // small helpers / DTOs
    private static final class NodeDistance {
        final String nodeId;
        final double distance;

        NodeDistance(String nodeId, double distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }
    }

    public static final class PathResult {
        private final double totalCost;
        private final List<Node> path;

        public PathResult(double totalCost, List<Node> path) {
            this.totalCost = totalCost;
            this.path = List.copyOf(path);
        }

        public double getTotalCost() {
            return totalCost;
        }

        public List<Node> getPath() {
            return path;
        }
    }
}
