package com.academic.annotation.dto;

public record AssignedAnnotator(Long taskId, Long annotatorId, String username, String firstName, String lastName,
                                long size, double progress) {
}
