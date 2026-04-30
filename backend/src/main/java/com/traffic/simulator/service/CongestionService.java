package com.traffic.simulator.service;

import com.traffic.simulator.geo.model.Edge;
import com.traffic.simulator.model.CongestionEdgeView;
import com.traffic.simulator.service.graph.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class CongestionService {

    private static final Logger log = LoggerFactory.getLogger(CongestionService.class);
    private final GraphService graphService;
    private final Random rnd = new Random();
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public CongestionService(GraphService graphService) {
        this.graphService = graphService;
    }

    public SseEmitter registerEmitter() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(emitter);
        });
        emitter.onError(err -> emitters.remove(emitter));

        return emitter;
    }

    @Scheduled(fixedRateString = "30000") // every 30 seconds
    public void randomizeCongestion() {
        Map<String, List<Edge>> adj = graphService.getAdjacencyList();
        int updates = 0;
        for (List<Edge> edges : adj.values()) {
            for (Edge e : edges) {
                // congestion weight between 0.5 and 2.5
                double cw = 0.5 + rnd.nextDouble() * 2.0;
                e.setCongestionWeight(cw);
                updates++;
            }
        }
        log.info("Randomized congestion for {} edges", updates);
        broadcast(graphService.snapshotCongestion());
    }

    private void broadcast(List<CongestionEdgeView> snapshot) {
        if (emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("traffic-update").data(snapshot));
            } catch (IOException ex) {
                emitter.completeWithError(ex);
                emitters.remove(emitter);
            }
        }
    }
}
