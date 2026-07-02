package it.cinofilo.ratelimit;

import it.cinofilo.ratelimit.config.PolicyRegistry;
import it.cinofilo.ratelimit.core.*;
import it.cinofilo.ratelimit.factory.RequestContextFactory;
import it.cinofilo.ratelimit.provider.BucketProvider;
import it.cinofilo.ratelimit.web.RateLimitInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitInterceptor.
 *
 * Uses mocks for all dependencies — no Spring context, no Bucket4j internals.
 * Verifies the orchestration logic: pass-through on disabled/no-policy/allowed,
 * 429 + headers on denied.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock private PolicyRegistry policyRegistry;
    @Mock private RequestContextFactory contextFactory;
    @Mock private BucketProvider bucketProvider;
    @Mock private BucketHandle bucketHandle;

    private RateLimitInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final RequestContext CONTEXT = RequestContext.of(
            "127.0.0.1", "demo", "owner", null, null, null, "/auth/login");

    private static final BucketConfig BUCKET_CONFIG = new BucketConfig(10, 10, 15);
    private static final RateLimitKeyExtractor EXTRACTOR = ctx -> "test-key";
    private static final RateLimitBucketDefinition BUCKET_DEF =
            new RateLimitBucketDefinition(EXTRACTOR, BUCKET_CONFIG);
    private static final RateLimitPolicy LOGIN_POLICY =
            new RateLimitPolicy(RateLimitPolicyType.LOGIN, List.of(BUCKET_DEF));

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor(policyRegistry, contextFactory, bucketProvider);
        request = new MockHttpServletRequest();
        request.setServletPath("/auth/login");
        response = new MockHttpServletResponse();
    }

    @Test
    void shouldPassThroughWhenRateLimitIsDisabled() throws Exception {
        when(policyRegistry.isEnabled()).thenReturn(false);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        verifyNoInteractions(contextFactory, bucketProvider);
    }

    @Test
    void shouldPassThroughWhenNoPolicyConfiguredForPath() throws Exception {
        when(policyRegistry.isEnabled()).thenReturn(true);
        when(policyRegistry.findByPath("/auth/login")).thenReturn(Optional.empty());

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        verifyNoInteractions(contextFactory, bucketProvider);
    }

    @Test
    void shouldAllowRequestWhenBucketHasTokens() throws Exception {
        when(policyRegistry.isEnabled()).thenReturn(true);
        when(policyRegistry.findByPath("/auth/login")).thenReturn(Optional.of(LOGIN_POLICY));
        when(contextFactory.build(request)).thenReturn(CONTEXT);
        when(bucketProvider.getBucket("test-key", BUCKET_CONFIG)).thenReturn(bucketHandle);
        when(bucketHandle.tryConsume()).thenReturn(ConsumeResult.allowed(9L));

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("9");
    }

    @Test
    void shouldDenyRequestWhenBucketIsExhausted() throws Exception {
        when(policyRegistry.isEnabled()).thenReturn(true);
        when(policyRegistry.findByPath("/auth/login")).thenReturn(Optional.of(LOGIN_POLICY));
        when(contextFactory.build(request)).thenReturn(CONTEXT);
        when(bucketProvider.getBucket("test-key", BUCKET_CONFIG)).thenReturn(bucketHandle);
        when(bucketHandle.tryConsume()).thenReturn(ConsumeResult.denied(60_000L));

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNotNull();
        assertThat(Long.parseLong(response.getHeader("Retry-After"))).isGreaterThan(0);
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.getHeader("X-RateLimit-Policy")).isEqualTo("LOGIN");
    }

    @Test
    void shouldDenyOnFirstExhaustedBucketInMultiBucketPolicy() throws Exception {
        BucketHandle handle2 = mock(BucketHandle.class);
        BucketConfig config2 = new BucketConfig(5, 5, 15);
        RateLimitKeyExtractor extractor2 = ctx -> "test-key-2";
        RateLimitBucketDefinition def2 = new RateLimitBucketDefinition(extractor2, config2);
        RateLimitPolicy twoPolicy = new RateLimitPolicy(
                RateLimitPolicyType.LOGIN, List.of(BUCKET_DEF, def2));

        when(policyRegistry.isEnabled()).thenReturn(true);
        when(policyRegistry.findByPath("/auth/login")).thenReturn(Optional.of(twoPolicy));
        when(contextFactory.build(request)).thenReturn(CONTEXT);
        when(bucketProvider.getBucket("test-key", BUCKET_CONFIG)).thenReturn(bucketHandle);
        when(bucketHandle.tryConsume()).thenReturn(ConsumeResult.denied(30_000L));

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        // Second bucket should never be checked after first is exhausted
        verifyNoInteractions(handle2);
        verify(bucketProvider, times(1)).getBucket(any(), any());
    }

    @Test
    void shouldSetRetryAfterToAtLeastOneSecond() throws Exception {
        when(policyRegistry.isEnabled()).thenReturn(true);
        when(policyRegistry.findByPath("/auth/login")).thenReturn(Optional.of(LOGIN_POLICY));
        when(contextFactory.build(request)).thenReturn(CONTEXT);
        when(bucketProvider.getBucket(any(), any())).thenReturn(bucketHandle);
        when(bucketHandle.tryConsume()).thenReturn(ConsumeResult.denied(0L));

        interceptor.preHandle(request, response, new Object());

        long retryAfter = Long.parseLong(response.getHeader("Retry-After"));
        assertThat(retryAfter).isGreaterThanOrEqualTo(1L);
    }
}
