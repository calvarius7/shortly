package neusta.shortly.web.dto;

import lombok.Builder;

@Builder(builderMethodName = "of")
public record StatsDto(String shortCode, int clicks) {
}
