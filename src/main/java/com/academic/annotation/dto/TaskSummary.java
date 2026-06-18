package com.academic.annotation.dto;

import java.time.LocalDate;

public record TaskSummary(Long id, String datasetName, LocalDate deadline, double progress, long size, long completed) {
}
