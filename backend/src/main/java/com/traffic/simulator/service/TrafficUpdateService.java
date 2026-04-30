package com.traffic.simulator.service;

import com.traffic.simulator.model.TrafficUpdateEvent;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class TrafficUpdateService {

  private final RoutingService routingService;
  private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

  public TrafficUpdateService(RoutingService routingService) {
    this.routingService = routingService;
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

  @Scheduled(fixedRateString = "${traffic.update-interval-ms:3000}")
  public void publishTrafficUpdates() {
    if (emitters.isEmpty()) {
      return;
    }

    TrafficUpdateEvent update = routingService.randomizeTraffic();
    for (SseEmitter emitter : emitters) {
      try {
        emitter.send(SseEmitter.event().name("traffic-update").data(update));
      } catch (IOException ex) {
        emitter.completeWithError(ex);
        emitters.remove(emitter);
      }
    }
  }
}
