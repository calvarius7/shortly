package neusta.shortly.service;

public final class UrlNormalizer {

    private UrlNormalizer() {
        // Utility class
    }

    /**
     * Normalizes a URL by adding missing protocols.
     *
     * @param url the input URL (e.g. "www.foo.bar" or "foo.bar")
     * @return the normalized URL with scheme (e.g. "https://www.foo.bar")
     */
    public static String normalize(final String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        final var trimmed = url.trim();
        if (hasScheme(trimmed)) {
            return trimmed;
        }
        return "https://" + trimmed;
    }

    /**
     * Checks whether the URL already contains a scheme (e.g. http://, https://).
     *
     * @param url the URL to check
     * @return true if the URL contains a scheme, false otherwise
     */
    public static boolean hasScheme(final String url) {
        return url != null && url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*");
    }
}
