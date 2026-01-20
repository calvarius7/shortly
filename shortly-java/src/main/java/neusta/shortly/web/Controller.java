package neusta.shortly.web;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import neusta.shortly.model.ShortLink;
import neusta.shortly.service.ShortLinkService;
import neusta.shortly.web.dto.ShortLinkDto;
import neusta.shortly.web.dto.StatsDto;
import neusta.shortly.web.validation.ValidShortCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Validated
public class Controller {

    private final ShortLinkService shortLinkService;

    @ApiResponses({
            @ApiResponse(
                    responseCode = "302",
                    description = "Weiterleitung zur Original-URL",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Redirect-Beispiel",
                                    value = "{\"message\":\"Redirect\"}",
                                    description = "Der eigentliche Redirect erfolgt über den Location-Header"))),
            @ApiResponse(responseCode = "404"),
            @ApiResponse(responseCode = "400")})
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> resolveShortCode(@PathVariable
                                                 @ValidShortCode final String shortCode) {
        final Optional<ShortLink> result = shortLinkService.resolveAndTrack(shortCode);
        return result
                .map(ShortLink::getOriginalUrl)
                .map(url -> ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(url))
                        .<Void>build())
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Statistiken erfolgreich abgerufen",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StatsDto.class))),
            @ApiResponse(responseCode = "404"),
            @ApiResponse(responseCode = "400")})
    @GetMapping(value = "/api/stats/{shortCode}", produces = "application/json")
    public ResponseEntity<StatsDto> getStats(@PathVariable
                                             @ValidShortCode final String shortCode) {
        final Optional<ShortLink> result = shortLinkService.findById(shortCode);
        return result
                .map(shortLink -> ResponseEntity.ok(StatsDto.of()
                        .shortCode(shortLink.getShortCode())
                        .clicks(shortLink.getClicks())
                        .build()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ApiResponses({
            @ApiResponse(responseCode = "201"),
            @ApiResponse(responseCode = "400")})
    @PostMapping(value = "/api/shorten", produces = "text/plain")
    @ResponseStatus(HttpStatus.CREATED)
    public String create(@Valid @RequestBody final ShortLinkDto inputDto) {
        return shortLinkService.create(inputDto.url(), inputDto.expiresAt())
                .getShortCode();
    }

    @ApiResponses({
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "404"),
            @ApiResponse(responseCode = "400")})
    @DeleteMapping("/api/{shortCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable
                       @ValidShortCode final String shortCode) {
        shortLinkService.deleteById(shortCode);
    }
}
