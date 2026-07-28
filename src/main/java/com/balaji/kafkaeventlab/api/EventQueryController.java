package com.balaji.kafkaeventlab.api;

import com.balaji.kafkaeventlab.order.OrderProjection;
import com.balaji.kafkaeventlab.order.OrderProjectionRepository;
import com.balaji.kafkaeventlab.tracking.TrackedEvent;
import com.balaji.kafkaeventlab.tracking.TrackedEventRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class EventQueryController {

    private final TrackedEventRepository trackedEventRepository;
    private final OrderProjectionRepository orderProjectionRepository;

    public EventQueryController(
            TrackedEventRepository trackedEventRepository,
            OrderProjectionRepository orderProjectionRepository
    ) {
        this.trackedEventRepository = trackedEventRepository;
        this.orderProjectionRepository = orderProjectionRepository;
    }

    @GetMapping("/tracked-events")
    public List<TrackedEvent> trackedEvents() {
        return trackedEventRepository.findAll(Sort.by(Sort.Direction.ASC, "consumedAt"));
    }

    @GetMapping("/order-projections")
    public List<OrderProjection> orderProjections() {
        return orderProjectionRepository.findAll();
    }
}
