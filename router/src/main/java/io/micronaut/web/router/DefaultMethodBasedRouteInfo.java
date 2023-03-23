package io.micronaut.web.router;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.bind.ArgumentBinder;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.bind.RequestBinderRegistry;
import io.micronaut.inject.MethodExecutionHandle;
import io.micronaut.inject.beans.KotlinExecutableMethodUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultMethodBasedRouteInfo<T, R> extends DefaultRouteInfo<R> implements MethodBasedRouteInfo<T, R> {

    private final MethodExecutionHandle<T, R> targetMethod;
    private final String bodyArgumentName;
    private final Argument<?> bodyArgument;
    private final Map<String, Argument<?>> requiredInputs;
    private final boolean isVoid;

    private ArgumentBinder<?, HttpRequest<?>>[] argumentBinders;

    public DefaultMethodBasedRouteInfo(MethodExecutionHandle<T, R> targetMethod,
                                       @Nullable
                                       Argument<?> bodyArgument,
                                       @Nullable
                                       String bodyArgumentName,
                                       List<MediaType> consumesMediaTypes,
                                       List<MediaType> producesMediaTypes,
                                       boolean isPermitsBody,
                                       boolean isErrorRoute) {
        super(targetMethod, targetMethod.getReturnType(), consumesMediaTypes, producesMediaTypes, targetMethod.getDeclaringType(), isErrorRoute, isPermitsBody);
        this.targetMethod = targetMethod;
        this.bodyArgument = bodyArgument;
        this.bodyArgumentName = bodyArgumentName;

        Argument<?>[] requiredArguments = targetMethod.getArguments();
        if (requiredArguments.length > 0) {
            Map<String, Argument<?>> requiredInputs = CollectionUtils.newLinkedHashMap(requiredArguments.length);
            for (Argument<?> requiredArgument : requiredArguments) {
                String inputName = resolveInputName(requiredArgument);
                requiredInputs.put(inputName, requiredArgument);
            }
            this.requiredInputs = Collections.unmodifiableMap(requiredInputs);
        } else {
            this.requiredInputs = Collections.emptyMap();
        }
        if (returnType.isVoid()) {
            isVoid = true;
        } else if (isSuspended()) {
            isVoid = KotlinExecutableMethodUtils.isKotlinFunctionReturnTypeUnit(targetMethod.getExecutableMethod());
        } else {
            isVoid = false;
        }
    }

    @Override
    public ArgumentBinder<?, HttpRequest<?>>[] resolveArgumentBinders(RequestBinderRegistry requestBinderRegistry) {
        // Allow concurrent access
        if (argumentBinders == null) {
            argumentBinders = resolveArgumentBindersInternal(requestBinderRegistry);
        }
        return argumentBinders;
    }

    private ArgumentBinder<?, HttpRequest<?>>[] resolveArgumentBindersInternal(RequestBinderRegistry requestBinderRegistry) {
        Argument<?>[] arguments = targetMethod.getArguments();
        if (arguments.length == 0) {
            return new ArgumentBinder[0];
        }

        ArgumentBinder<?, HttpRequest<?>>[] binders = new ArgumentBinder[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            Argument<?> argument = arguments[i];
            Optional<? extends ArgumentBinder<?, HttpRequest<?>>> argumentBinder = requestBinderRegistry.findArgumentBinder(argument);
            binders[i] = argumentBinder.orElse(null);
        }
        return binders;
    }

    @Override
    public boolean isVoid() {
        return isVoid;
    }

    /**
     * Resolves the name for an argument.
     *
     * @param argument the argument
     * @return the name
     */
    @Override
    public @NonNull String resolveInputName(@NonNull Argument<?> argument) {
        String inputName = argument.getAnnotationMetadata().stringValue(Bindable.NAME).orElse(null);
        if (StringUtils.isEmpty(inputName)) {
            inputName = argument.getName();
        }
        return inputName;
    }

    @Override
    public MethodExecutionHandle<T, R> getTargetMethod() {
        return targetMethod;
    }

    @Override
    public Argument<?> getBodyArgument() {
        return bodyArgument;
    }

    @Override
    public String getBodyArgumentName() {
        return bodyArgumentName;
    }

    @Override
    public Map<String, Argument<?>> getRequiredInputs() {
        return requiredInputs;
    }

}
