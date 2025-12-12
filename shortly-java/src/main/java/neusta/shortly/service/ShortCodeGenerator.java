package neusta.shortly.service;

@FunctionalInterface
public interface ShortCodeGenerator {
    int LENGTH = 6;

    String generate();
}
