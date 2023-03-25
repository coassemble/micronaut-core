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
package io.micronaut.http.server.netty.binders;

import io.micronaut.core.async.subscriber.CompletionAwareSubscriber;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionError;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.exceptions.ConversionErrorException;
import io.micronaut.core.type.Argument;
import io.micronaut.http.bind.binders.NonBlockingBodyArgumentBinder;
import io.micronaut.http.netty.stream.StreamedHttpRequest;
import io.micronaut.http.server.netty.HttpContentProcessor;
import io.micronaut.http.server.netty.HttpContentProcessorAsReactiveProcessor;
import io.micronaut.http.server.netty.HttpContentProcessorResolver;
import io.micronaut.http.server.netty.NettyHttpRequest;
import io.micronaut.http.server.netty.NettyHttpServer;
import io.micronaut.web.router.exceptions.UnsatisfiedRouteException;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.EmptyByteBuf;
import io.netty.util.ReferenceCounted;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * A {@link io.micronaut.http.annotation.Body} argument binder for a reactive streams {@link Publisher}.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@Singleton
public class PublisherBodyBinder implements NonBlockingBodyArgumentBinder<Publisher<?>>,
    StreamedNettyRequestArgumentBinder<Publisher<?>> {

    private static final Logger LOG = LoggerFactory.getLogger(NettyHttpServer.class);
    private static final Argument<Publisher<?>> TYPE = (Argument) Argument.of(Publisher.class);

    private final HttpContentProcessorResolver httpContentProcessorResolver;
    private final ConversionService conversionService;

    /**
     * @param conversionService            The conversion service
     * @param httpContentProcessorResolver The http content processor resolver
     */
    public PublisherBodyBinder(ConversionService conversionService,
                               HttpContentProcessorResolver httpContentProcessorResolver) {
        this.conversionService = conversionService;
        this.httpContentProcessorResolver = httpContentProcessorResolver;
    }

    @Override
    public Argument<Publisher<?>> argumentType() {
        return TYPE;
    }


    @Override
    public BindingResult<Publisher<?>> bindForStreamedNettyRequest(ArgumentConversionContext<Publisher<?>> context,
                                                                   StreamedHttpRequest streamedHttpRequest,
                                                                   NettyHttpRequest<?> nettyHttpRequest) {
        Argument<?> targetType = context.getFirstTypeVariable().orElse(Argument.OBJECT_ARGUMENT);

        nettyHttpRequest.setUsesHttpContentProcessor();
        HttpContentProcessor processor = httpContentProcessorResolver.resolve(nettyHttpRequest, targetType);

        return () -> Optional.of(subscriber -> HttpContentProcessorAsReactiveProcessor.asPublisher(processor.resultType(context.getArgument()), nettyHttpRequest).subscribe(new CompletionAwareSubscriber<>() {

            Subscription s;

            @Override
            protected void doOnSubscribe(Subscription subscription) {
                this.s = subscription;
                subscriber.onSubscribe(subscription);
            }

            @Override
            protected void doOnNext(Object message) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Server received streaming message for argument [{}]: {}", context.getArgument(), message);
                }
                if (message instanceof ByteBufHolder byteBufHolder) {
                    message = byteBufHolder.content();
                    if (message instanceof EmptyByteBuf) {
                        s.request(1);
                        return;
                    }
                }

                ArgumentConversionContext<?> conversionContext = context.with(targetType);
                Optional<?> converted = conversionService.convert(message, conversionContext);

                if (converted.isPresent()) {
                    subscriber.onNext(converted.get());
                } else {

                    try {
                        Optional<ConversionError> lastError = conversionContext.getLastError();
                        if (lastError.isPresent()) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Cannot convert message for argument [{}] and value: {}", context.getArgument(), message, lastError.get().getCause());
                            }
                            subscriber.onError(new ConversionErrorException(context.getArgument(), lastError.get()));
                        } else {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Cannot convert message for argument [{}] and value: {}", context.getArgument(), message);
                            }
                            subscriber.onError(UnsatisfiedRouteException.create(context.getArgument()));
                        }
                    } finally {
                        s.cancel();
                    }
                }

                if (message instanceof ReferenceCounted referenceCounted) {
                    referenceCounted.release();
                }
            }

            @Override
            protected void doOnError(Throwable t) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Server received error for argument [" + context.getArgument() + "]: " + t.getMessage(), t);
                }
                try {
                    subscriber.onError(t);
                } finally {
                    s.cancel();
                }
            }

            @Override
            protected void doOnComplete() {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Done receiving messages for argument: {}", context.getArgument());
                }
                subscriber.onComplete();
            }

        }));
    }

}
