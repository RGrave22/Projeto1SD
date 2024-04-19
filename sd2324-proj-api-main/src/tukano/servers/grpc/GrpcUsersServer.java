package tukano.servers.grpc;

import io.grpc.ServerBuilder;
import tukano.api.java.Users;
import tukano.discovery.Discovery;

import java.net.InetAddress;
import java.util.logging.Logger;

public class GrpcUsersServer {
    public static final int PORT = 13456;

    private static final String SERVICE = Users.NAME;
//    static Discovery discovery = Discovery.getInstance();

    private static final String GRPC_CTX = "/grpc";
    private static final String SERVER_BASE_URI = "grpc://%s:%s%s";

    private static Logger Log = Logger.getLogger(GrpcUsersServer.class.getName());

    public static void main(String[] args) throws Exception {

//        Log.info("AQUI1");
        var stub = new GrpcUsersServerStub();
//        Log.info("AQUI2");
        var server = ServerBuilder.forPort(PORT).addService(stub).build();
//        Log.info("AQUI3");
        var serverURI = String.format(SERVER_BASE_URI, InetAddress.getLocalHost().getHostAddress(), PORT, GRPC_CTX);
//        Log.info("AQUI4");


        Log.info(String.format("%s gRPC Server ready @ %s\n", Users.NAME, serverURI));
        Discovery.getInstance().announce(SERVICE, serverURI);
        server.start().awaitTermination();
    }
}
