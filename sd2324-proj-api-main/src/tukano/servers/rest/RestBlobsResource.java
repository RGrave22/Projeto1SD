package tukano.servers.rest;

import tukano.api.Blob;
import tukano.api.java.Blobs;
import tukano.api.rest.RestBlobs;

public class RestBlobsResource implements RestBlobs {

    final Blobs impl;

    public RestBlobsResource(Blobs impl) {this.impl = new Blob();}

    @Override
    public void upload(String blobId, byte[] bytes) {

    }

    @Override
    public byte[] download(String blobId) {
        return new byte[0];
    }
}
