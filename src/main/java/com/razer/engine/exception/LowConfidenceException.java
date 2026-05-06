package com.razer.engine.exception;

public class LowConfidenceException extends RuntimeException {

    private final double confidence;

    public LowConfidenceException(double confidence, String message) {
        super(message);
        this.confidence = confidence;
    }

    public double getConfidence() {
        return confidence;
    }
}