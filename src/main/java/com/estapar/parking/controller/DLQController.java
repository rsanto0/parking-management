package com.estapar.parking.controller;

import com.estapar.parking.dto.WebhookEvent;
import com.estapar.parking.service.EventQueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dlq")
public class DLQController {

    private final EventQueueService eventQueueService;

    public DLQController(EventQueueService eventQueueService) {
        this.eventQueueService = eventQueueService;
    }

    @GetMapping
    public ResponseEntity<List<WebhookEvent>> getDLQ() {
        List<WebhookEvent> events = eventQueueService.getDeadLetterQueue()
            .stream()
            .collect(Collectors.toList());
        return ResponseEntity.ok(events);
    }

    @GetMapping("/size")
    public ResponseEntity<Integer> getDLQSize() {
        return ResponseEntity.ok(eventQueueService.getDLQSize());
    }
}
