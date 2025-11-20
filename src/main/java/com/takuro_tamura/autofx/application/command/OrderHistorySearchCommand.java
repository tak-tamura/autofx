package com.takuro_tamura.autofx.application.command;

import java.time.LocalDate;

public record OrderHistorySearchCommand(
    int page,
    int size,
    LocalDate startDate,
    LocalDate endDate
) {}
