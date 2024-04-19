package tukano.servers.grpc;

import io.grpc.ServerBuilder;
import tukano.api.java.Shorts;
import tukano.discovery.Discovery;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.logging.Logger;

public class GrpcShortsServer {
    public static final int PORT = 14567;

    private static final String SERVICE = Shorts.NAME;
//    static Discovery discovery = Discovery.getInstance();

    private static final String GRPC_CTX = "/grpc";
    private static final String SERVER_BASE_URI = "grpc://%s:%s%s";

    private static Logger Log = Logger.getLogger(GrpcShortsServer.class.getName());

    public static void main(String[] args) throws Exception {

//        Log.info("AQUI1");
        var stub = new GrpcShortsServerStub();
//        Log.info("AQUI2");
        var server = ServerBuilder.forPort(PORT).addService(stub).build();
//        Log.info("AQUI3");
        var serverURI = String.format(SERVER_BASE_URI, InetAddress.getLocalHost().getHostAddress(), PORT, GRPC_CTX);
//        Log.info("AQUI4");

        Discovery.getInstance().announce(SERVICE, serverURI);

        Log.info(String.format("%s gRPC Server ready @ %s\n", Shorts.NAME, serverURI));
        server.start().awaitTermination();
    }
}