package io.micronaut.http.server.netty;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.multipart.StreamingFileUpload;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NettyHttpFileUpload {

    @Nullable
    private List<String> observedStreamingFileUploads;
    @Nullable
    private Map<String, StreamingFileUpload> streamingFileUploads;
    @Nullable
    private List<String> observedFileUploads;
    @Nullable
    private Map<String, StreamingFileUpload> fileUploads;


    public void observeStreamingFileUpload(String argumentName) {
        if (observedStreamingFileUploads == null) {
            observedStreamingFileUploads = new ArrayList<>();
        }
        observedStreamingFileUploads.add(argumentName);
    }

    public void observeFileUploads(String argumentName) {
        if (observedFileUploads == null) {
            observedFileUploads = new ArrayList<>();
        }
        observedFileUploads.add(argumentName);
    }
}
