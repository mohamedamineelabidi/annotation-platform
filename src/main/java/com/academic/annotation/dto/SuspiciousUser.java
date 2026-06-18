package com.academic.annotation.dto;

public record SuspiciousUser(String username, String reason, double averageTimeSeconds, double dominantLabelRate) {
}
