package com.academic.annotation.dto;

public record DatasetSummary(Long id, String name, String taskType, long size, double progress, int annotators) {
}
