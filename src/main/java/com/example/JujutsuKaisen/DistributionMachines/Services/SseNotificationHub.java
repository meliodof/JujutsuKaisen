package com.example.JujutsuKaisen.DistributionMachines.Services;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseNotificationHub {

    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ignored) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    public void broadcast(Map<String, Object> notification) {
        List<SseEmitter> dead = new java.util.ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }
}
