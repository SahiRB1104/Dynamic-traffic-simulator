package com.traffic.simulator.controller;

import com.traffic.simulator.model.HeatmapPoint;
import com.traffic.simulator.service.graph.GraphService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HeatmapController {

    private final GraphService graphService;

    public HeatmapController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/heatmap")
    public List<HeatmapPoint> getHeatmap() {
        return graphService.snapshotCongestion()
                .stream()
                .map(edge -> {
                    // Calculate midpoint of edge
                    double midLat = (edge.fromNode().getLatitude() + edge.toNode().getLatitude()) / 2.0;
                    double midLon = (edge.fromNode().getLongitude() + edge.toNode().getLongitude()) / 2.0;
                    
                    // Normalize intensity: (congestionWeight - 1.0) / 3.0
                    // This assumes congestionWeight ranges from ~1.0 to ~4.0
                    // clamped to 0.0-1.0
                    double intensity = Math.max(0.0, Math.min(1.0, (edge.congestionWeight() - 1.0) / 3.0));
                    
                    return new HeatmapPoint(midLat, midLon, intensity);
                })
                .toList();
    }
}
