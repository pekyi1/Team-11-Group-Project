package com.servicehub.event;

import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ServiceRequestEvent extends ApplicationEvent {
    private final ServiceRequest serviceRequest;
    private final String eventType;
    private final User actor;
    private final String comment;
    private final User previousAgent; // for transfers

    public ServiceRequestEvent(Object source, ServiceRequest serviceRequest, String eventType,
                                User actor, String comment, User previousAgent) {
        super(source);
        this.serviceRequest = serviceRequest;
        this.eventType = eventType;
        this.actor = actor;
        this.comment = comment;
        this.previousAgent = previousAgent;
    }

    public ServiceRequestEvent(Object source, ServiceRequest serviceRequest, String eventType,
                                User actor, String comment) {
        this(source, serviceRequest, eventType, actor, comment, null);
    }
}
