package com.takuro_tamura.autofx.presentation.controller.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleRequest {
    private LocalDateTime time;
}
