package tukano.servers.rest;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tukano.discovery.Discovery;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;

public class RestUsersServer {

    static Discovery discovery = Discovery.getInstance();

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    public static final int PORT = 8080;
    public static final String SERVICE = "users";
    private static final String SERVER_URI_FMT = "http://%s:%s/rest";

    public static void main(String[] args) {
        try {

            ResourceConfig config = new ResourceConfig();
            config.register(RestUsersResource.class);

            String ip = InetAddress.getLocalHost().getHostAddress();
            String serverURI = String.format(SERVER_URI_FMT, ip, PORT);
            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config);

            discovery.announce(SERVICE, serverURI);

            try(var ss = new ServerSocket( PORT )) {
                var cs = ss.accept();
                System.err.println("Accepting connections at: " + cs.getLocalSocketAddress());
            }

        } catch (Exception e) {
//            Log.severe(e.getMessage());
        }
    }


}
