package io.openleap.starter.core.security.identity;

import io.openleap.starter.core.config.IdentityHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityContextArgumentResolverTest {

    @Mock
    private MethodParameter parameter;

    @Mock
    private NativeWebRequest webRequest;

    private IdentityContextArgumentResolver resolver;

    @BeforeEach
    void setup() {
        IdentityHolder.setTenantId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        IdentityHolder.setUserId(UUID.fromString("660e8400-e29b-41d4-a716-446655441111"));
        IdentityHolder.setRoles(Set.of("ROLE_USER"));
        IdentityHolder.setScopes(Set.of("read", "write"));

        resolver = new IdentityContextArgumentResolver();
    }

    @AfterEach
    void cleanup() {
        IdentityHolder.clear();
    }

    @Test
    @DisplayName("should support parameter when type is identity context and annotation is present")
    void shouldSupportParameterWhenTypeIsIdentityContextAndAnnotationIsPresent() {
        // given
        doReturn(IdentityContext.class).when(parameter).getParameterType();
        when(parameter.hasParameterAnnotation(AuthenticatedIdentity.class)).thenReturn(true);

        // when
        boolean supports = resolver.supportsParameter(parameter);

        // then
        assertThat(supports).isTrue();
    }

    @Test
    @DisplayName("should not support parameter when type is not identity context")
    void shouldNotSupportParameterWhenTypeIsNotIdentityContext() {
        // given
        doReturn(String.class).when(parameter).getParameterType();

        // when
        boolean supports = resolver.supportsParameter(parameter);

        // then
        assertThat(supports).isFalse();
    }

    @Test
    @DisplayName("should not support parameter when annotation is missing")
    void shouldNotSupportParameterWhenAnnotationIsMissing() {
        // given
        doReturn(IdentityContext.class).when(parameter).getParameterType();
        when(parameter.hasParameterAnnotation(AuthenticatedIdentity.class)).thenReturn(false);

        // when
        boolean supports = resolver.supportsParameter(parameter);

        // then
        assertThat(supports).isFalse();
    }

    @Test
    @DisplayName("should resolve identity context when tenant and user id are present")
    void shouldResolveIdentityContextWhenIdentityIsPresent() throws Exception {
        // when
        Object result = resolver.resolveArgument(parameter, null, webRequest, null);

        // then
        assertThat(result)
                .isInstanceOf(IdentityContext.class)
                .hasFieldOrPropertyWithValue("tenantId", UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .hasFieldOrPropertyWithValue("userId", UUID.fromString("660e8400-e29b-41d4-a716-446655441111"))
                .hasFieldOrPropertyWithValue("roles", Set.of("ROLE_USER"))
                .hasFieldOrPropertyWithValue("scopes", Set.of("read", "write"));
    }

}
