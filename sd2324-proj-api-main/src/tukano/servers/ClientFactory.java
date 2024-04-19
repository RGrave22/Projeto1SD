package tukano.servers;

import tukano.api.java.Blobs;
import tukano.api.java.Shorts;
import tukano.api.java.Users;
import tukano.clients.grpc.GrpcBlobsClient;
import tukano.clients.grpc.GrpcShortsClient;
import tukano.clients.grpc.GrpcUsersClient;
import tukano.clients.rest.RestBlobsClient;
import tukano.clients.rest.RestShortsClient;
import tukano.clients.rest.RestUsersClient;
import tukano.discovery.Discovery;
import tukano.servers.grpc.GrpcBlobsServer;

import java.net.URI;
import java.util.logging.Logger;

public class ClientFactory {

    private static final Discovery discovery = Discovery.getInstance();
    private static Logger Log = Logger.getLogger(ClientFactory.class.getName());


    public static Users getUsers() {

        var serverURI = discovery.knownUrisOf("users",1)[0];
        var serverURIString = String.valueOf(serverURI);

        if( serverURIString.endsWith("rest"))
            return new RestUsersClient( serverURI );
        else
            return new GrpcUsersClient( serverURI );
    }

    public static Shorts getShort() {
        var serverURI = discovery.knownUrisOf("shorts",1)[0];
        var serverURIString = String.valueOf(serverURI);

        if( serverURIString.endsWith("rest"))
            return new RestShortsClient( serverURI );
        else
            return new GrpcShortsClient( serverURI );
    }

    public static Blobs getBlob() {

        var serverURI = discovery.knownUrisOf("blobs",1)[0];
        var serverURIString = String.valueOf(serverURI);

        if( serverURIString.endsWith("rest"))
            return new RestBlobsClient( serverURI );
        else
            return new GrpcBlobsClient( serverURI );
    }

    public static Blobs getBlob(URI serverURI) {

        var serverURIString = String.valueOf(serverURI);

        if( serverURIString.endsWith("rest"))
            return new RestBlobsClient( serverURI );
        else
            return new GrpcBlobsClient( serverURI );
    }
}
