package io.micronaut.http.server.netty.binders;

import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.type.Argument;
import io.micronaut.http.bind.binders.PostponedRequestArgumentBinder;
import io.micronaut.http.bind.binders.TypedRequestArgumentBinder;
import io.micronaut.http.multipart.FileUpload;
import io.micronaut.http.server.netty.NettyHttpRequest;

public class FileUploadBinder implements TypedRequestArgumentBinder<FileUpload>,
    PostponedRequestArgumentBinder, NettyRequestArgumentBinder<FileUpload> {

    private static final Argument<FileUpload> STREAMING_FILE_UPLOAD_ARGUMENT = Argument.of(FileUpload.class);

    @Override
    public BindingResult<FileUpload> bindForNettyRequest(ArgumentConversionContext<FileUpload> context,
                                                         NettyHttpRequest<?> request) {
        return null;
    }

    @Override
    public Argument<FileUpload> argumentType() {
        return STREAMING_FILE_UPLOAD_ARGUMENT;
    }
}
