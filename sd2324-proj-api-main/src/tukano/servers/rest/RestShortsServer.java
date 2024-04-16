package tukano.servers.rest;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tukano.discovery.Discovery;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.logging.Logger;

public class RestShortsServer {

    static Discovery discovery = Discovery.getInstance();

//    private static Logger Log = Logger.getLogger(RestUsersServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    public static final int PORT = 4567;
    public static final String SERVICE = "shorts";
    private static final String SERVER_URI_FMT = "http://%s:%s/rest";

    public static void main(String[] args) {
        try {

            ResourceConfig config = new ResourceConfig();
            config.register(RestShortsResource.class);

            String ip = InetAddress.getLocalHost().getHostAddress();
            String serverURI = String.format(SERVER_URI_FMT, ip, PORT);
            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config);

            discovery.announce(SERVICE, serverURI);

            try(var ss = new ServerSocket( PORT )) {
                var cs = ss.accept();
            }

        } catch (Exception e) {
//            Log.severe(e.getMessage());
        }
    }


}
