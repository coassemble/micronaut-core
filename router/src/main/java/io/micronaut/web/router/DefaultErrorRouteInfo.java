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

public class DefaultErrorRouteInfo<T, R> extends DefaultRequestMatcher<T, R> implements ErrorRouteInfo<T, R> {

    @Nullable
    private final Class<?> originatingType;
    private final Class<? extends Throwable> exceptionType;
    private final ConversionService conversionService;

    public DefaultErrorRouteInfo(@Nullable Class<?> originatingType,
                                 Class<? extends Throwable> exceptionType,
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
        this.exceptionType = exceptionType;
        this.conversionService = conversionService;
    }

    @Override
    public Class<?> originatingType() {
        return originatingType;
    }

    @Override
    public Class<? extends Throwable> exceptionType() {
        return exceptionType;
    }

    @Override
    public Optional<RouteMatch<R>> match(Class<?> originatingClass, Throwable exception) {
        if (originatingClass == originatingType && exceptionType.isInstance(exception)) {
            return Optional.of(new ErrorRouteMatch<>(exception, this, conversionService));
        }
        return Optional.empty();
    }

    @Override
    public Optional<RouteMatch<R>> match(Throwable exception) {
        if (originatingType == null && exceptionType.isInstance(exception)) {
            return Optional.of(new ErrorRouteMatch<>(exception, this, conversionService));
        }
        return Optional.empty();
    }

    @Override
    public HttpStatus findStatus(HttpStatus defaultStatus) {
        return super.findStatus(defaultStatus == null ? HttpStatus.INTERNAL_SERVER_ERROR : defaultStatus);
    }


    @Override
    public int hashCode() {
        return ObjectUtils.hash(super.hashCode(), exceptionType, originatingType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        DefaultErrorRouteInfo that = (DefaultErrorRouteInfo) o;
        return exceptionType.equals(that.exceptionType) &&
                Objects.equals(originatingType, that.originatingType);
    }

    @Override
    public String toString() {
        return new StringBuilder().append(' ')
                .append(exceptionType.getSimpleName())
                .append(" -> ")
                .append(getTargetMethod().getDeclaringType().getSimpleName())
                .append('#')
                .append(getTargetMethod())
                .toString();
    }
}
