package neusta.shortly.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class Base62Generator implements ShortCodeGenerator {

    private static final String CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    @Override
    public String generate() {
        final var random = ThreadLocalRandom.current();
        final var builder = new StringBuilder(LENGTH);

        for (int i = 0; i < LENGTH; i++) {
            builder.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }

        return builder.toString();
    }
}
