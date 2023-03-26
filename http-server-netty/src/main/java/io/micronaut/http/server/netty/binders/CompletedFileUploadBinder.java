package io.micronaut.http.server.netty.binders;

import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.http.bind.binders.PendingRequestBindingResult;
import io.micronaut.http.bind.binders.TypedRequestArgumentBinder;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.http.multipart.FileUpload;
import io.micronaut.http.server.netty.MicronautHttpData;
import io.micronaut.http.server.netty.NettyHttpRequest;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CompletedFileUploadBinder implements TypedRequestArgumentBinder<CompletedFileUpload>, NettyRequestArgumentBinder<CompletedFileUpload> {

    private static final Argument<CompletedFileUpload> STREAMING_FILE_UPLOAD_ARGUMENT = Argument.of(CompletedFileUpload.class);

    private final ConversionService conversionService;

    public CompletedFileUploadBinder(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public List<Class<?>> superTypes() {
        return List.of(FileUpload.class);
    }

    @Override
    public BindingResult<CompletedFileUpload> bindForNettyRequest(ArgumentConversionContext<CompletedFileUpload> context,
                                                                  NettyHttpRequest<?> request) {

        CompletableFuture<CompletedFileUpload> completableFuture = new CompletableFuture<>();

        Argument<CompletedFileUpload> argument = context.getArgument();
        String inputName = argument.getAnnotationMetadata().stringValue(Bindable.NAME).orElse(argument.getName());

        request.getFileUploadObserver().subscribe(new Subscriber<>() {

            Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(MicronautHttpData<?> data) {
                if (data.getName().equals(inputName) && !completableFuture.isDone() && data instanceof io.netty.handler.codec.http.multipart.FileUpload fileUpload) {
                    completableFuture.complete(
                        conversionService.convertRequired(fileUpload, CompletedFileUpload.class)
                    );
                    subscription.cancel();
                } else {
                    subscription.request(1);
                }
            }

            @Override
            public void onError(Throwable t) {
                completableFuture.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
            }
        });

        return new PendingRequestBindingResult<>() {

            @Override
            public boolean isPending() {
                return !completableFuture.isDone();
            }

            @Override
            public Optional<CompletedFileUpload> getValue() {
                return Optional.ofNullable(completableFuture.getNow(null));
            }
        };
    }

    @Override
    public Argument<CompletedFileUpload> argumentType() {
        return STREAMING_FILE_UPLOAD_ARGUMENT;
    }
}
