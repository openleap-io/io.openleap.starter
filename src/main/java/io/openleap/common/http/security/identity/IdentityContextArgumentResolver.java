package io.openleap.common.http.security.identity;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

// TODO (itaseski): Add Spring integration tests to verify the argument resolver works as expected.
@Component
public class IdentityContextArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(IdentityContext.class) &&
                parameter.hasParameterAnnotation(AuthenticatedIdentity.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        UUID tenantId = IdentityHolder.getTenantId();
        UUID userId = IdentityHolder.getUserId();

        // TODO (itaseski): Consider throwing for roles and scopes.
        if (tenantId == null || userId == null) {
            throw new IllegalStateException("tenantId and userId can't be null");
        }

        return new IdentityContext(
                IdentityHolder.getTenantId(),
                IdentityHolder.getUserId(),
                IdentityHolder.getRoles(),
                IdentityHolder.getScopes()
        );
    }
}
