package tukano.api;

import tukano.api.java.Blobs;
import tukano.api.java.Result;

public class Blob implements Blobs {
    @Override
    public Result<Void> upload(String blobId, byte[] bytes) {
        return null;
    }

    @Override
    public Result<byte[]> download(String blobId) {
        return null;
    }
}
