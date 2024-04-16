package tukano.clients.rest;

import com.google.gson.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.hsqldb.persist.Log;
import tukano.api.Short;
import tukano.api.java.Result;
import tukano.api.java.Shorts;
import tukano.api.rest.RestShorts;
import tukano.api.rest.RestUsers;
import tukano.servers.java.JavaUsers;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

public class RestShortsClient extends RestClient implements Shorts {

    protected static final int READ_TIMEOUT = 10000;
    protected static final int CONNECT_TIMEOUT = 10000;

    protected static final int MAX_RETRIES = 10;
    protected static final int RETRY_SLEEP = 5000;

    private static Logger Log = Logger.getLogger(RestShortsClient.class.getName());



    final URI serverURI;
    final Client client;
    final ClientConfig config;

    final WebTarget target;

    public RestShortsClient( URI serverURI ) {
        this.serverURI = serverURI;

        this.config = new ClientConfig();


        config.property( ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
        config.property( ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);


        this.client = ClientBuilder.newClient(config);

        target = client.target( serverURI ).path( RestShorts.PATH );
    }

    private Result<Short> clt_createShort(String userId, String password) {
        JsonObject requestData = new JsonObject();
        requestData.addProperty("ownerId", userId);
        requestData.addProperty("password", password);

        return super.toJavaResult(
                target.path(userId)
                        .queryParam(RestShorts.PWD, password)
                        .request()
                        .accept(MediaType.APPLICATION_JSON)
                        .post(Entity.entity(requestData, MediaType.APPLICATION_JSON)), Short.class);
    }

    private Result<Void> clt_deleteShort(String shortId, String pwd) {
        return super.toJavaResult(
                target.path(shortId)
                        .queryParam(RestShorts.PWD, pwd)
                        .request()
                        .accept(MediaType.APPLICATION_JSON)
                        .delete(), Void.class );
    }

    private Result<Short> clt_getShort(String shortId) {
        return super.toJavaResult(
                target.path(shortId)
                        .request()
                        .accept(MediaType.APPLICATION_JSON)
                        .get(), Short.class );
    }

    private Result<List<String>> clt_getShorts(String userId) {
        return super.toJavaResult(
                target.path(userId)
                        .path("/shorts")
                        .request()
                        .accept(MediaType.APPLICATION_JSON)
                        .get(), new GenericType<List<String>>() {});
    }

    private Result<Void> clt_follow(String userId1, String userId2, boolean isFollowing, String password) {
        JsonObject requestData = new JsonObject();
        requestData.addProperty("ownerId1", userId1);
        requestData.addProperty("ownerId2", userId2);
        requestData.addProperty("isFollowing", isFollowing);
        requestData.addProperty("password", password);

        return super.toJavaResult(
                target.path(userId1)
                        .path(userId2)
                        .path("/followers")
                        .queryParam(RestShorts.PWD, password)
                        .request()
                        .accept(MediaType.APPLICATION_JSON)
                        .post(Entity.entity(requestData, MediaType.APPLICATION_JSON)), Void.class);
    }

    private Result<List<String>> clt_followers(String userId, String password) {
        return super.toJavaResult(
                target.path(userId)
                        .path("/followers")
                        .queryParam(RestShorts.PWD, password)
                        .request()
                        .accept(MediaType.APPLICATION_JSON)
                        .get(), new GenericType<List<String>>() {});
    }

    private Result<Void> clt_like(String shortId, String userId, boolean isLiked, String password) {
        JsonObject requestData = new JsonObject();
        requestData.addProperty("shortId", shortId);
        requestData.addProperty("userId", userId);
        requestData.addProperty("isLiked", isLiked);
        requestData.addProperty("password", password);

        return super.toJavaResult(
                target.path(userId)
                        .queryParam(RestShorts.PWD, password)
                        .request()
                        .accept(MediaType.APPLICATION_JSON)
                        .post(Entity.entity(requestData, MediaType.APPLICATION_JSON)), Void.class);
    }

    private Result<List<String>> clt_likes(String shortId, String password) {
        return super.toJavaResult(
                target.path(shortId)
                        .queryParam(RestShorts.PWD, password)
                        .request()
                        .accept(MediaType.APPLICATION_JSON)
                        .get(), new GenericType<List<String>>() {});
    }

    private Result<List<String>> clt_getFeed(String userId, String password) {
        return super.toJavaResult(
                target.path(userId)
                        .queryParam(RestShorts.PWD, password)
                        .request()
                        .accept(MediaType.APPLICATION_JSON)
                        .get(), new GenericType<List<String>>() {});
    }

    @Override
    public Result<Short> createShort(String userId, String password) {
        return super.reTry( () -> clt_createShort(userId, password));
    }

    @Override
    public Result<Void> deleteShort(String shortId, String password) {
        return super.reTry( () -> clt_deleteShort(shortId, password));
    }

    @Override
    public Result<Short> getShort(String shortId) {
        return super.reTry( () -> clt_getShort(shortId));
    }

    @Override
    public Result<List<String>> getShorts(String userId) {
        return super.reTry( () -> clt_getShorts(userId));
    }

    @Override
    public Result<Void> follow(String userId1, String userId2, boolean isFollowing, String password) {
        return super.reTry( () -> clt_follow(userId1, userId2, isFollowing, password));
    }

    @Override
    public Result<List<String>> followers(String userId, String password) {
        return super.reTry( () -> clt_followers(userId, password));
    }

    @Override
    public Result<Void> like(String shortId, String userId, boolean isLiked, String password) {
        return super.reTry( () -> clt_like(shortId, userId, isLiked, password));
    }

    @Override
    public Result<List<String>> likes(String shortId, String password) {
        return super.reTry( () -> clt_likes(shortId, password));
    }

    @Override
    public Result<List<String>> getFeed(String userId, String password) {
        return super.reTry( () -> clt_getFeed(userId, password));
    }
}
