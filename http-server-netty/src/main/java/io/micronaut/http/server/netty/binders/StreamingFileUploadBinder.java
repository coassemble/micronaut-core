package io.micronaut.http.server.netty.binders;

import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.http.MediaType;
import io.micronaut.http.bind.binders.PendingRequestBindingResult;
import io.micronaut.http.bind.binders.TypedRequestArgumentBinder;
import io.micronaut.http.multipart.PartData;
import io.micronaut.http.multipart.StreamingFileUpload;
import io.micronaut.http.server.netty.MicronautHttpData;
import io.micronaut.http.server.netty.NettyHttpRequest;
import io.micronaut.http.server.netty.multipart.NettyPartData;
import io.micronaut.http.server.netty.multipart.NettyStreamingFileUpload;
import reactor.core.publisher.Sinks;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class StreamingFileUploadBinder implements TypedRequestArgumentBinder<StreamingFileUpload>, NettyRequestArgumentBinder<StreamingFileUpload> {

    private static final Argument<StreamingFileUpload> STREAMING_FILE_UPLOAD_ARGUMENT = Argument.of(StreamingFileUpload.class);

    private final ConversionService conversionService;
    private final NettyStreamingFileUpload.Factory fileUploadFactory;

    public StreamingFileUploadBinder(ConversionService conversionService, NettyStreamingFileUpload.Factory fileUploadFactory) {
        this.conversionService = conversionService;
        this.fileUploadFactory = fileUploadFactory;
    }

    @Override
    public BindingResult<StreamingFileUpload> bindForNettyRequest(ArgumentConversionContext<StreamingFileUpload> context,
                                                                  NettyHttpRequest<?> request) {

        CompletableFuture<StreamingFileUpload> completableFuture = new CompletableFuture<>();

        Argument<StreamingFileUpload> argument = context.getArgument();
        String inputName = argument.getAnnotationMetadata().stringValue(Bindable.NAME).orElse(argument.getName());
        Sinks.Many<PartData> parts = Sinks.many().multicast().onBackpressureBuffer();

        request.getFileUploadObserver().subscribe(data -> {

            if (!data.getName().equals(inputName)) {
                return;
            }

            MicronautHttpData<?>.Chunk chunk = data.pollChunk();
            NettyPartData part = new NettyPartData(() -> {
                if (data instanceof io.netty.handler.codec.http.multipart.FileUpload fu) {
                    return Optional.of(MediaType.of(fu.getContentType()));
                } else {
                    return Optional.empty();
                }
            }, chunk::claim);

            if (!completableFuture.isDone() && data instanceof io.netty.handler.codec.http.multipart.FileUpload fileUpload) {
                completableFuture.complete(
                    fileUploadFactory.create(fileUpload, parts.asFlux())
                );
            }

            conversionService.convert(part, PartData.class).ifPresent(parts::tryEmitNext);

            if (data.isCompleted()) {
                parts.tryEmitComplete();
            }
        });

        return new PendingRequestBindingResult<>() {

            @Override
            public boolean isPending() {
                return !completableFuture.isDone();
            }

            @Override
            public Optional<StreamingFileUpload> getValue() {
                return Optional.ofNullable(completableFuture.getNow(null));
            }
        };
    }

    @Override
    public Argument<StreamingFileUpload> argumentType() {
        return STREAMING_FILE_UPLOAD_ARGUMENT;
    }
}
