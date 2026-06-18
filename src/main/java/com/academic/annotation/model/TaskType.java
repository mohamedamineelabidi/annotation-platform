package com.academic.annotation.model;

public enum TaskType {
    TEXT_CLASSIFICATION("Simple classification"),
    TEXT_PAIR("Text pair similarity"),
    NLI("Natural language inference");

    private final String displayName;

    TaskType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
