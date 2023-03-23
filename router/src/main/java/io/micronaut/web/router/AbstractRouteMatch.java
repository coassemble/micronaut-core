/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.web.router;

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.bind.ArgumentBinder;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionError;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.exceptions.ConversionErrorException;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.ReturnType;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.bind.RequestBinderRegistry;
import io.micronaut.http.bind.binders.BodyArgumentBinder;
import io.micronaut.http.bind.binders.NonBlockingBodyArgumentBinder;
import io.micronaut.http.bind.binders.RequestBeanAnnotationBinder;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.MethodExecutionHandle;
import io.micronaut.web.router.exceptions.UnsatisfiedRouteException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Abstract implementation of the {@link RouteMatch} interface.
 *
 * @param <T> The target type
 * @param <R> Route Match
 * @author Graeme Rocher
 * @author Denis Stepanov
 * @since 1.0
 */
abstract class AbstractRouteMatch<T, R> implements MethodBasedRouteMatch<T, R> {

    protected final ConversionService conversionService;
    protected final MethodBasedRouteInfo<T, R> routeInfo;
    protected final MethodExecutionHandle<T, R> methodExecutionHandle;
    protected final ExecutableMethod<T, R> executableMethod;

    private final Argument<?>[] arguments;
    private final String[] argumentNames;
    private final Object[] argumentValues;
    private final Supplier<ArgumentBinder.BindingResult<?>>[] lateBinders;
    private final boolean[] fulfilledArguments;
    private boolean fulfilled;
    private boolean bindersApplied;

    /**
     * Constructor.
     *
     * @param routeInfo         The route info
     * @param conversionService The conversion service
     */
    protected AbstractRouteMatch(MethodBasedRouteInfo<T, R> routeInfo, ConversionService conversionService) {
        this.routeInfo = routeInfo;
        this.conversionService = conversionService;
        this.methodExecutionHandle = routeInfo.getTargetMethod();
        this.executableMethod = methodExecutionHandle.getExecutableMethod();
        this.arguments = executableMethod.getArguments();
        this.argumentNames = routeInfo.getRequiredInputs().keySet().toArray(String[]::new);
        int length = arguments.length;
        if (length == 0) {
            fulfilled = true;
            this.argumentValues = null;
            this.fulfilledArguments = null;
            this.lateBinders = null;
        } else {
            this.lateBinders = new Supplier[length];
            this.argumentValues = new Object[length];
            this.fulfilledArguments = new boolean[length];
        }
    }

    @Override
    public RouteInfo<R> getRouteInfo() {
        return routeInfo;
    }

    @Override
    public T getTarget() {
        return routeInfo.getTargetMethod().getTarget();
    }

    @NonNull
    @Override
    public ExecutableMethod<T, R> getExecutableMethod() {
        return executableMethod;
    }

    @Override
    public AnnotationMetadata getAnnotationMetadata() {
        return executableMethod.getAnnotationMetadata();
    }

    @Override
    public Optional<Argument<?>> getBodyArgument() {
        Argument<?> arg = routeInfo.getBodyArgument();
        if (arg != null) {
            return Optional.of(arg);
        }
        String bodyArgument = routeInfo.getBodyArgumentName();
        if (bodyArgument != null) {
            return Optional.ofNullable(routeInfo.getRequiredInputs().get(bodyArgument));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Argument<?>> getRequiredInput(String name) {
        for (int i = 0; i < argumentNames.length; i++) {
            String argumentName = argumentNames[i];
            if (name.equals(argumentName)) {
                return Optional.of(arguments[i]);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isFulfilled() {
        return fulfilled;
    }

    @Override
    public boolean isSatisfied(String name) {
        for (int i = 0; i < argumentNames.length; i++) {
            String argumentName = argumentNames[i];
            if (name.equals(argumentName)) {
                return fulfilledArguments[i];
            }
        }
        return false;
    }

    @Override
    public Method getTargetMethod() {
        return routeInfo.getTargetMethod().getTargetMethod();
    }

    @Override
    public String getMethodName() {
        return executableMethod.getMethodName();
    }

    @Override
    public Class getDeclaringType() {
        return executableMethod.getDeclaringType();
    }

    @Override
    public Argument<?>[] getArguments() {
        return executableMethod.getArguments();
    }

    @Override
    public ReturnType<R> getReturnType() {
        return executableMethod.getReturnType();
    }

    @Override
    public R invoke(Object... arguments) {
        Argument<?>[] targetArguments = getArguments();
        if (targetArguments.length == 0) {
            return methodExecutionHandle.invoke();
        } else {
            List<Object> argumentList = new ArrayList<>(arguments.length);
            Map<String, Object> variables = getVariableValues();
            Iterator<Object> valueIterator = variables.values().iterator();
            int i = 0;
            for (Argument<?> targetArgument : targetArguments) {
                String name = targetArgument.getName();
                Object value = variables.get(name);
                if (value != null) {
                    Optional<?> result = conversionService.convert(value, targetArgument.getType());
                    argumentList.add(result.orElseThrow(() -> new IllegalArgumentException("Wrong argument types to method: " + executableMethod)));
                } else if (valueIterator.hasNext()) {
                    Optional<?> result = conversionService.convert(valueIterator.next(), targetArgument.getType());
                    argumentList.add(result.orElseThrow(() -> new IllegalArgumentException("Wrong argument types to method: " + executableMethod)));
                } else if (i < arguments.length) {
                    Optional<?> result = conversionService.convert(arguments[i++], targetArgument.getType());
                    argumentList.add(result.orElseThrow(() -> new IllegalArgumentException("Wrong argument types to method: " + executableMethod)));
                } else {
                    throw new IllegalArgumentException("Wrong number of arguments to method: " + executableMethod);
                }
            }
            return methodExecutionHandle.invoke(argumentList.toArray());
        }
    }

    @Override
    public R execute() {
        Argument<?>[] targetArguments = getArguments();
        if (targetArguments.length == 0) {
            return methodExecutionHandle.invoke();
        }
        if (fulfilled) {
            return methodExecutionHandle.invoke(argumentValues);
        }
        if (!bindersApplied) {
            throw new IllegalStateException("Argument binders not processed!");
        }
        for (int i = 0; i < arguments.length; i++) {
            if (fulfilledArguments[i]) {
                continue;
            }
            Supplier<ArgumentBinder.BindingResult<?>> lateValueSupplier = lateBinders[i];
            if (lateValueSupplier != null) {
                Argument<?> argument = arguments[i];
                ArgumentBinder.BindingResult<?> bindingResult = lateValueSupplier.get();
                setBindingResultOfFail(i, argument, bindingResult);
            }
            if (!fulfilledArguments[i]) {
                Object value = getVariableValues().get(argumentNames[i]);
                if (value != null) {
                    setValue(i, arguments[i], value);
                }
            }
            if (!fulfilledArguments[i]) {
                throw UnsatisfiedRouteException.create(arguments[i]);
            }
        }
        return methodExecutionHandle.invoke(argumentValues);
    }

    @Override
    public void fulfill(Map<String, Object> newValues) {
        if (fulfilled) {
            return;
        }
        for (int i = 0; i < argumentNames.length; i++) {
            if (fulfilledArguments[i]) {
                continue;
            }
            String argumentName = argumentNames[i];
            Object value = newValues.get(argumentName);
            if (value != null) {
                setValue(i, arguments[i], value);
            }
        }
        checkIfFulfilled();
    }

    @Override
    public void fulfillOnExecute(String argumentName, Supplier<Object> argumentValueSupplier) {
        for (int i = 0; i < argumentNames.length; i++) {
            if (fulfilledArguments[i]) {
                continue;
            }
            if (argumentNames[i].equals(argumentName)) {
                lateBinders[i] = () -> () -> Optional.ofNullable(argumentValueSupplier.get());
                return;
            }
        }
    }

    @Override
    public void fulfill(RequestBinderRegistry requestBinderRegistry, HttpRequest<?> request) {
        if (fulfilled) {
            return;
        }
        if (bindersApplied) {
            throw new IllegalStateException("Argument binders already processed!");
        }
        ArgumentBinder<?, HttpRequest<?>>[] argumentBinders = routeInfo.resolveArgumentBinders(requestBinderRegistry);
        for (int i = 0; i < arguments.length; i++) {
            if (fulfilledArguments[i] || lateBinders[i] != null) {
                continue;
            }
            Argument<Object> argument = (Argument<Object>) arguments[i];
            Object value = getVariableValues().get(argumentNames[i]);
            if (value != null) {
                setValue(i, argument, value);
                continue;
            }
            ArgumentBinder<Object, HttpRequest<?>> argumentBinder = (ArgumentBinder<Object, HttpRequest<?>>) argumentBinders[i];
            if (argumentBinder != null) {
                fulfillValue(
                    i,
                    argumentBinder,
                    argument,
                    request
                );
            } else if (argument.isNullable()) {
                setValue(i, argument, null);
            }
        }
        checkIfFulfilled();
        bindersApplied = true;
    }

    private <E> void fulfillValue(int index,
                                  ArgumentBinder<E, HttpRequest<?>> argumentBinder,
                                  Argument<E> argument,
                                  HttpRequest<?> request) {
        ArgumentConversionContext<E> conversionContext = ConversionContext.of(
            argument,
            request.getLocale().orElse(null),
            request.getCharacterEncoding()
        );

        ArgumentBinder.BindingResult<E> bindingResult;
        if (argumentBinder instanceof BodyArgumentBinder) {
            if (argumentBinder instanceof NonBlockingBodyArgumentBinder) {
                bindingResult = argumentBinder.bind(conversionContext, request);
                setBindingResult(index, argument, bindingResult);
            } else {
                // Blocking needs complete request
                lateBinders[index] = () -> argumentBinder.bind(conversionContext, request);
                return;
            }
        } else if (argumentBinder instanceof RequestBeanAnnotationBinder) {
            lateBinders[index] = () -> argumentBinder.bind(conversionContext, request);
            return;
        } else {
            bindingResult = argumentBinder.bind(conversionContext, request);
        }
        boolean isSet;
        if (conversionContext.hasErrors()) {
            isSet = false;
        } else {
            isSet = setBindingResult(index, argument, bindingResult);
        }
        if (!isSet) {
            // If the value is null / Optional.empty / conversion error -> try to bind it later
            lateBinders[index] = () -> {
                ArgumentBinder.BindingResult<E> result = argumentBinder.bind(conversionContext, request);
                Optional<ConversionError> lastError = conversionContext.getLastError();
                if (lastError.isPresent()) {
                    return (ArgumentBinder.BindingResult) () -> lastError;
                }
                return result;
            };
        }
    }

    private void setBindingResultOfFail(int index, Argument<?> argument, ArgumentBinder.BindingResult<?> bindingResult) {
        boolean isSet = setBindingResult(index, argument, bindingResult);
        if (isSet) {
            return;
        }
        if (argument.isNullable()) {
            setValue(index, argument, null);
            return;
        }
        if (argument.isOptional()) {
            setValue(index, argument, Optional.empty());
            return;
        }
        List<ConversionError> conversionErrors = bindingResult.getConversionErrors();
        if (!conversionErrors.isEmpty()) {
            // should support multiple errors
            ConversionError conversionError = conversionErrors.iterator().next();
            throw new ConversionErrorException(argument, conversionError);
        }
        throw UnsatisfiedRouteException.create(argument);
    }

    private boolean setBindingResult(int index, Argument<?> argument, ArgumentBinder.BindingResult<?> bindingResult) {
        if (!bindingResult.isSatisfied()) {
            return false;
        }
        Object value;
        if (argument.getType() == Optional.class) {
            Optional<?> optionalValue = bindingResult.getValue();
            if (optionalValue.isPresent()) {
                value = optionalValue.get();
            } else {
                return false;
            }
        } else if (bindingResult.isPresentAndSatisfied()) {
            value = bindingResult.get();
        } else {
            return false;
        }
        setValue(index, argument, value);
        return true;
    }

    private void setValue(int index, Argument<?> argument, Object value) {
        if (value != null) {
            argumentValues[index] = convertValue(conversionService, argument, value);
        }
        fulfilledArguments[index] = true;
    }

    private void checkIfFulfilled() {
        if (fulfilled) {
            return;
        }
        for (boolean isFulfilled : fulfilledArguments) {
            if (!isFulfilled) {
                return;
            }
        }
        fulfilled = true;
    }

    private Object convertValue(ConversionService conversionService, Argument<?> argument, Object value) {
        if (value instanceof ConversionError conversionError) {
            throw new ConversionErrorException(argument, conversionError);
        }
        Class<?> argumentType = argument.getType();
        if (argumentType.isInstance(value)) {
            if (argument.isContainerType()) {
                if (argument.hasTypeVariables()) {
                    ConversionContext conversionContext = ConversionContext.of(argument);
                    Optional<?> result = conversionService.convert(value, argumentType, conversionContext);
                    return resolveValueOrError(argument, conversionContext, result);
                }
            }
            return value;
        } else {
            ConversionContext conversionContext = ConversionContext.of(argument);
            Optional<?> result = conversionService.convert(value, argumentType, conversionContext);
            return resolveValueOrError(argument, conversionContext, result);
        }
    }

    private Object resolveValueOrError(Argument<?> argument, ConversionContext conversionContext, Optional<?> result) {
        if (result.isEmpty()) {
            Optional<ConversionError> lastError = conversionContext.getLastError();
            if (lastError.isEmpty() && argument.isDeclaredNullable()) {
                return null;
            }
            throw lastError.map(conversionError ->
                (RuntimeException) new ConversionErrorException(argument, conversionError)).orElseGet(() -> UnsatisfiedRouteException.create(argument)
            );
        }
        return result.get();
    }

}
