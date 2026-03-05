package com.servicehub.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationPreferencesRequest {
    private Boolean requestCreated;
    private Boolean statusUpdates;
    // Critical notifications (ticketAssigned, slaWarning, slaBreach, transferred) cannot be disabled
}
