package it.cinofilo.ratelimit.web;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.*;

/**
 * Ensures the request body can be read more than once.
 *
 * Registered at HIGHEST_PRECEDENCE so it runs before Spring Security and the
 * rate limit interceptor. Wraps every request with RepeatableContentRequestWrapper,
 * which reads the body into a byte array once and serves it from there for all
 * subsequent reads — allowing both the RequestContextFactory (rate limiter) and
 * the @RequestBody deserializer (controller) to each read the full body.
 *
 * Without this filter, the first component to call getInputStream() consumes the
 * stream, leaving nothing for subsequent readers.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestBodyCachingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        chain.doFilter(new RepeatableContentRequestWrapper(request), response);
    }

    /**
     * Reads the body once into a byte array and serves it from there on every
     * subsequent call to getInputStream() or getReader().
     */
    static final class RepeatableContentRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] body;

        RepeatableContentRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream stream = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public boolean isFinished() { return stream.available() == 0; }
                @Override public boolean isReady()    { return true; }
                @Override public void setReadListener(ReadListener rl) {
                    throw new UnsupportedOperationException();
                }
                @Override public int read() throws IOException { return stream.read(); }
                @Override public int read(byte[] b, int off, int len) throws IOException {
                    return stream.read(b, off, len);
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            String charset = getCharacterEncoding();
            InputStreamReader isr = (charset != null)
                    ? new InputStreamReader(getInputStream(), java.nio.charset.Charset.forName(charset))
                    : new InputStreamReader(getInputStream(), java.nio.charset.StandardCharsets.UTF_8);
            return new BufferedReader(isr);
        }
    }
}
