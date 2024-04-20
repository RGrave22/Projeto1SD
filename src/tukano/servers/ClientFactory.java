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

import java.net.URI;

public class ClientFactory {

    private static final String SERVER_URI = "rest";
    private static final String SERVICE_NAME_USERS = "users";
    private static final String SERVICE_NAME_SHORTS = "shorts";
    private static final int MIN_REPLIES = 1;

    private static final Discovery discovery = Discovery.getInstance();


    public static Users getUsers() {

        var serverURI = discovery.knownUrisOf(SERVICE_NAME_USERS,MIN_REPLIES)[0];
        var serverURIString = String.valueOf(serverURI);

        if( serverURIString.endsWith(SERVER_URI))
            return new RestUsersClient( serverURI );
        else
            return new GrpcUsersClient( serverURI );
    }

    public static Shorts getShort() {
        var serverURI = discovery.knownUrisOf(SERVICE_NAME_SHORTS,MIN_REPLIES)[0];
        var serverURIString = String.valueOf(serverURI);

        if( serverURIString.endsWith(SERVER_URI))
            return new RestShortsClient( serverURI );
        else
            return new GrpcShortsClient( serverURI );
    }

    public static Blobs getBlob(URI serverURI) {

        var serverURIString = String.valueOf(serverURI);

        if( serverURIString.endsWith(SERVER_URI))
            return new RestBlobsClient( serverURI );
        else
            return new GrpcBlobsClient( serverURI );
    }
}
