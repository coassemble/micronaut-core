package io.micronaut.http.server.netty.binders;

import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.bind.binders.RequestArgumentBinder;
import io.micronaut.http.netty.stream.StreamedHttpRequest;
import io.micronaut.http.server.netty.NettyHttpRequest;

import java.util.Optional;

/**
 * A version of {@link RequestArgumentBinder} that requires {@link NettyHttpRequest} and {@link StreamedHttpRequest}.
 *
 * @param <T> A type
 * @author Denis Stepanov
 * @since 4.0.0
 */
@Experimental
public interface StreamedNettyRequestArgumentBinder<T> extends RequestArgumentBinder<T> {

    @Override
    default BindingResult<T> bind(ArgumentConversionContext<T> context, HttpRequest<?> source) {
        if (source instanceof NettyHttpRequest<?> nettyHttpRequest) {
            io.netty.handler.codec.http.HttpRequest nativeRequest = nettyHttpRequest.getNativeRequest();
            if (nativeRequest instanceof StreamedHttpRequest streamedHttpRequest) {
                return bindForStreamedNettyRequest(context, streamedHttpRequest, nettyHttpRequest);
            }
        }
        return BindingResult.EMPTY;
    }

    /**
     * Bind the given argument from the given source.
     *
     * @param context             The {@link ArgumentConversionContext}
     * @param streamedHttpRequest The streamed HTTP request
     * @param nettyHttpRequest    The netty http request
     * @return An {@link Optional} of the value. If no binding was possible {@link Optional#empty()}
     */
    BindingResult<T> bindForStreamedNettyRequest(ArgumentConversionContext<T> context,
                                                 StreamedHttpRequest streamedHttpRequest,
                                                 NettyHttpRequest<?> nettyHttpRequest);
}
