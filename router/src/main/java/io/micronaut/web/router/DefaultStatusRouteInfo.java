package io.micronaut.web.router;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.ObjectUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.inject.MethodExecutionHandle;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class DefaultStatusRouteInfo<T, R> extends DefaultRequestMatcher<T, R> implements StatusRouteInfo<T, R> {

    private final Class<?> originatingType;
    private final HttpStatus status;
    private final ConversionService conversionService;

    public DefaultStatusRouteInfo(Class<?> originatingType,
                                  HttpStatus status,
                                  MethodExecutionHandle<T, R> targetMethod,
                                  @Nullable
                                  String bodyArgumentName,
                                  @Nullable
                                  Argument<?> bodyArgument,
                                  List<MediaType> consumesMediaTypes,
                                  List<MediaType> producesMediaTypes,
                                  List<Predicate<HttpRequest<?>>> predicates,
                                  ConversionService conversionService) {
        super(targetMethod, bodyArgument, bodyArgumentName, consumesMediaTypes, producesMediaTypes, true, true, predicates);
        this.originatingType = originatingType;
        this.status = status;
        this.conversionService = conversionService;
    }

    @Override
    public Class<?> originatingType() {
        return originatingType;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public HttpStatus findStatus(HttpStatus defaultStatus) {
        return super.findStatus(status);
    }

    @Override
    public Optional<RouteMatch<R>> match(Class<?> originatingClass, HttpStatus status) {
        if (originatingClass == this.originatingType && this.status == status) {
            return Optional.of(new StatusRouteMatch<>(this, conversionService));
        }
        return Optional.empty();
    }

    @Override
    public Optional<RouteMatch<R>> match(HttpStatus status) {
        if (this.originatingType == null && this.status == status) {
            return Optional.of(new StatusRouteMatch<>(this, conversionService));
        }
        return Optional.empty();
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(super.hashCode(), status, originatingType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefaultStatusRouteInfo<?, ?> that)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        return status == that.status &&
                Objects.equals(originatingType, that.originatingType);
    }

    @Override
    public String toString() {
        return new StringBuilder().append(' ')
                .append(status)
                .append(" -> ")
                .append(getTargetMethod().getDeclaringType().getSimpleName())
                .append('#')
                .append(getTargetMethod())
                .toString();
    }
}
