package org.example.http;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpRequestTest {

    @Test
    void parsesHostAndPortFromHostHeader() {
        HttpRequest request = new HttpRequest(
                "GET",
                "http://example.com:8080/docs",
                "HTTP/1.1",
                Map.of("Host", "example.com:8080"),
                null
        );

        assertEquals("example.com", request.getHost());
        assertEquals(8080, request.getPort());
    }

    @Test
    void convertsAbsoluteUrlToOriginFormPath() {
        HttpRequest request = new HttpRequest(
                "GET",
                "http://example.com/docs?q=1",
                "HTTP/1.1",
                Map.of("Host", "example.com"),
                null
        );

        assertEquals("/docs?q=1", request.getPath());
    }

    @Test
    void keepsOriginFormPathWhenAlreadyNormalized() {
        HttpRequest request = new HttpRequest(
                "GET",
                "/docs?q=1",
                "HTTP/1.1",
                Map.of("Host", "example.com"),
                null
        );

        assertEquals("/docs?q=1", request.getPath());
    }

    @Test
    void throwsWhenHostHeaderIsMissing() {
        HttpRequest request = new HttpRequest(
                "GET",
                "/",
                "HTTP/1.1",
                Map.of(),
                null
        );

        assertThrows(InvalidRequestException.class, request::getHost);
    }
}
