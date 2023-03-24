package io.micronaut.http.server.netty.binders;

import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.bind.binders.RequestArgumentBinder;
import io.micronaut.http.server.netty.NettyHttpRequest;

import java.util.Optional;

/**
 * A version of {@link RequestArgumentBinder} that requires {@link NettyHttpRequest}.
 * @param <T> A type
 * @author Denis Stepanov
 * @since 4.0.0
 */
@Experimental
public interface NettyRequestArgumentBinder<T> extends RequestArgumentBinder<T> {

    @Override
    default BindingResult<T> bind(ArgumentConversionContext<T> context, HttpRequest<?> source) {
        if (source instanceof NettyHttpRequest<?> nettyHttpRequest) {
            return bindForNettyRequest(context, nettyHttpRequest);
        }
        return BindingResult.EMPTY;
    }

    /**
     * Bind the given argument from the given source.
     *
     * @param context             The {@link ArgumentConversionContext}
     * @param nettyHttpRequest    The netty http request
     * @return An {@link Optional} of the value. If no binding was possible {@link Optional#empty()}
     */
    BindingResult<T> bindForNettyRequest(ArgumentConversionContext<T> context, NettyHttpRequest<?> nettyHttpRequest);
}
