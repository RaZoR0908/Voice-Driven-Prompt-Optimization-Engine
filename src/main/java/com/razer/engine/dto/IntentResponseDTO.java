package com.razer.engine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IntentResponseDTO(
        @NotBlank String intent,
        @NotBlank String task,
        @NotBlank String domain,
        @NotNull @NotEmpty List<String> constraints,
        @JsonProperty("output_format") @NotBlank String outputFormat,
        @NotBlank String audience
) {
}