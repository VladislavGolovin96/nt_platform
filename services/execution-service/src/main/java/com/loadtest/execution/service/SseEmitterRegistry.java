package com.loadtest.execution.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseEmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterRegistry.class);

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(String executionId) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        emitters.computeIfAbsent(executionId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(executionId, emitter));
        emitter.onTimeout(() -> remove(executionId, emitter));
        emitter.onError(e -> remove(executionId, emitter));

        return emitter;
    }

    public void emit(String executionId, String line) {
        List<SseEmitter> list = emitters.get(executionId);
        if (list == null || list.isEmpty()) return;

        list.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event().data(line));
                return false;
            } catch (IOException e) {
                return true; // remove dead emitter
            }
        });
    }

    public void complete(String executionId) {
        List<SseEmitter> list = emitters.remove(executionId);
        if (list == null) return;
        list.forEach(emitter -> {
            try { emitter.complete(); } catch (Exception ignored) {}
        });
    }

    private void remove(String executionId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(executionId);
        if (list != null) {
            list.remove(emitter);
        }
    }
}
