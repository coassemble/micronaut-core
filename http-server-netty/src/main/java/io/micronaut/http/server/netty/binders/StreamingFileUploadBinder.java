package io.micronaut.http.server.netty.binders;

import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.type.Argument;
import io.micronaut.http.bind.binders.PostponedRequestArgumentBinder;
import io.micronaut.http.bind.binders.TypedRequestArgumentBinder;
import io.micronaut.http.multipart.StreamingFileUpload;
import io.micronaut.http.server.netty.NettyHttpRequest;

public class StreamingFileUploadBinder implements TypedRequestArgumentBinder<StreamingFileUpload>,
    PostponedRequestArgumentBinder, NettyRequestArgumentBinder<StreamingFileUpload> {

    private static final Argument<StreamingFileUpload> STREAMING_FILE_UPLOAD_ARGUMENT = Argument.of(StreamingFileUpload.class);

    @Override
    public BindingResult<StreamingFileUpload> bindForNettyRequest(ArgumentConversionContext<StreamingFileUpload> context,
                                                                  NettyHttpRequest<?> request) {
        return null;
    }

    @Override
    public Argument<StreamingFileUpload> argumentType() {
        return STREAMING_FILE_UPLOAD_ARGUMENT;
    }
}
