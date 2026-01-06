package neusta.shortly.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShortLinkTest {

    @Test
    void getRedisKey() {
        final String shortCode = "ABC123";
        final String expectedKey = "links:" + shortCode;
        final String actualKey = ShortLink.getRedisKey(shortCode);
        assertEquals(expectedKey, actualKey);
    }
}
