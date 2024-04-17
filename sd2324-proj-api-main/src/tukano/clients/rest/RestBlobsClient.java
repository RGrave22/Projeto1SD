package tukano.clients.rest;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import tukano.api.java.Blobs;
import tukano.api.java.Result;
import tukano.api.rest.RestBlobs;

import java.net.URI;

public class RestBlobsClient extends RestClient implements Blobs {

    protected static final int READ_TIMEOUT = 10000;
    protected static final int CONNECT_TIMEOUT = 10000;

    protected static final int MAX_RETRIES = 10;
    protected static final int RETRY_SLEEP = 5000;

//    private static Logger Log = Logger.getLogger(RestBlobsClient.class.getName());

    final URI serverURI;
    final Client client;
    final ClientConfig config;

    final WebTarget target;

    public RestBlobsClient(URI serverURI ) {
        this.serverURI = serverURI;

        this.config = new ClientConfig();


        config.property( ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
        config.property( ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);


        this.client = ClientBuilder.newClient(config);

        target = client.target( serverURI ).path( RestBlobs.PATH );
    }

    public Result<Void> clt_upload(String blobId, byte[] bytes) {
        Entity<byte[]> entity = Entity.entity(bytes, MediaType.APPLICATION_OCTET_STREAM);
        return super.toJavaResult(
                target.path(blobId)
                        .request()
                        .accept(MediaType.APPLICATION_OCTET_STREAM)
                        .post(entity) , Void.class);

    }

    public Result<byte[]> clt_download(String blobId) {
        return super.toJavaResult(
                target.path(blobId)
                        .request()
                        .accept(MediaType.APPLICATION_OCTET_STREAM)
                        .get() , byte[].class);

    }

    public Result<Void> clt_delete(String blobId) {
        return super.toJavaResult(
                target.path(blobId)
                        .request()
                        .accept(MediaType.APPLICATION_OCTET_STREAM)
                        .delete() , Void.class);
    }

    public Result<Integer> clt_getNumFiles() {
        return super.toJavaResult(
                target.request()
                        .accept(MediaType.APPLICATION_JSON)
                        .get() , Integer.class);
    }

    @Override
    public Result<Void> upload(String blobId, byte[] bytes) {
        return super.reTry( () -> clt_upload(blobId, bytes));
    }

    @Override
    public Result<byte[]> download(String blobId) {
        return super.reTry( () -> clt_download(blobId));
    }

    @Override
    public Result<Void> delete(String blobId) {
        return super.reTry( () -> clt_delete(blobId));
    }

    @Override
    public Result<Integer> getNumFiles() {
        return super.reTry( () -> clt_getNumFiles());
    }


}
