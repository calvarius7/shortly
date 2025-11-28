package neusta.shortly.web;

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
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class Controller {

    private final ShortLinkService shortLinkService;

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> resolveShortCode(@PathVariable
                                                 @ValidShortCode final String shortCode) {
        final Optional<ShortLink> result = shortLinkService.findById(shortCode);
        return result
                .map(ShortLink::getOriginalUrl)
                .map(url -> ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(url))
                        .<Void>build())
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/stats/{shortCode}")
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

    @PostMapping("/shorten")
    @ResponseStatus(HttpStatus.CREATED)
    public String create(@Valid @RequestBody final ShortLinkDto inputDto) {
        return shortLinkService.create(inputDto.url(), inputDto.expiresAt())
                .getShortCode();
    }

    @DeleteMapping("/{shortCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable
                       @ValidShortCode final String shortCode) {
        shortLinkService.deleteById(shortCode);
    }

}
