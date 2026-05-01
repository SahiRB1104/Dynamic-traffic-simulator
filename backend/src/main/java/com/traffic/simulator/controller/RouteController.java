package com.traffic.simulator.controller;

import com.traffic.simulator.geo.model.Node;
import com.traffic.simulator.model.CongestionEdgeView;
import com.traffic.simulator.model.CongestionLevel;
import com.traffic.simulator.model.RouteRequest;
import com.traffic.simulator.model.RouteResponse;
import com.traffic.simulator.service.CongestionService;
import com.traffic.simulator.service.graph.GraphService;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class RouteController {

    private final GraphService graphService;
    private final CongestionService congestionService;

    public RouteController(GraphService graphService, CongestionService congestionService) {
        this.graphService = graphService;
        this.congestionService = congestionService;
    }

    @PostMapping("/route")
    public RouteResponse route(@RequestBody RouteRequest request) {
        long startTime = System.nanoTime();
        GraphService.PathResult result = runAlgorithm(request);
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        return new RouteResponse(result.getPath(), result.getTotalCost(), durationMs);
    }

    @GetMapping("/congestion")
    public List<CongestionEdgeView> congestion() {
        return graphService.snapshotCongestion();
    }

    @GetMapping(value = "/stream/traffic", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTraffic() {
        SseEmitter emitter = congestionService.registerEmitter();
        try {
            emitter.send(SseEmitter.event().name("traffic-init").data(graphService.snapshotCongestion()));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    private GraphService.PathResult runAlgorithm(RouteRequest request) {
        Node source = graphService.resolveEndpoint(request.source(), request.sourceLatitude(), request.sourceLongitude());
        Node destination = graphService.resolveEndpoint(
            request.destination(), request.destinationLatitude(), request.destinationLongitude());
        String algorithm = request.algorithm() == null ? "DIJKSTRA" : request.algorithm().trim().toUpperCase(Locale.ROOT);

        return switch (algorithm) {
            case "ASTAR", "A*" -> graphService.aStar(source.getId(), destination.getId());
            case "DIJKSTRA" -> graphService.dijkstra(source.getId(), destination.getId());
            default -> throw new IllegalArgumentException("Unsupported algorithm: " + request.algorithm());
        };
    }
}
