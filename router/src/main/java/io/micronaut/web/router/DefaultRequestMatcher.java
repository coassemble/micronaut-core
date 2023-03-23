package io.micronaut.web.router;

import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.inject.MethodExecutionHandle;

import java.util.List;
import java.util.function.Predicate;

public class DefaultRequestMatcher<T, R> extends DefaultMethodBasedRouteInfo<T, R> implements RequestMatcher {

    private final List<Predicate<HttpRequest<?>>> predicates;

    public DefaultRequestMatcher(MethodExecutionHandle<T, R> targetMethod,
                                 Argument<?> bodyArgument,
                                 String bodyArgumentName,
                                 List<MediaType> producesMediaTypes,
                                 List<MediaType> consumesMediaTypes,
                                 boolean isPermitsBody,
                                 boolean isErrorRoute,
                                 List<Predicate<HttpRequest<?>>> predicates) {
        super(targetMethod, bodyArgument, bodyArgumentName, producesMediaTypes, consumesMediaTypes, isPermitsBody, isErrorRoute);
        this.predicates = predicates;
    }

    @Override
    public boolean matching(HttpRequest<?> httpRequest) {
        if (predicates.isEmpty()) {
            return true;
        }
        for (Predicate<HttpRequest<?>> predicate : predicates) {
            if (!predicate.test(httpRequest)) {
                return false;
            }
        }
        return true;
    }
}
