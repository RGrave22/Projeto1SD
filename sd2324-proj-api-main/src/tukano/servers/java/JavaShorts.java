package tukano.servers.java;

import tukano.api.Follower;
import tukano.api.Likes;
import tukano.api.Short;
import tukano.api.User;
import tukano.api.java.Result;
import tukano.api.java.Shorts;

import tukano.clients.rest.RestBlobsClient;
import tukano.clients.rest.RestUsersClient;
import tukano.discovery.Discovery;
import tukano.persistence.Hibernate;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.net.URI;

public class JavaShorts implements Shorts {

    private static final String FILE_PATH = "%s\\BlobFiles\\%s";

    private static final String BLOB_URL = "%s/blobs/%s?verifier=%s";

    Hibernate db = Hibernate.getInstance();
    Discovery discovery = Discovery.getInstance();
    RestUsersClient restUsersServer = new RestUsersClient(discovery.knownUrisOf("users", 1)[0]);

//    Quando hover mais do que um servidor de blobs fazer:
//    E quando for para dar delete a um short ver no vetor de servidoresde blobs qual o servido correspondente ao blobUrl do short
    URI[] blobsUri = discovery.knownUrisOf("blobs", 1);


//    private static Logger Log = Logger.getLogger(JavaShorts.class.getName());

    private URI getBlobUri() {
        int minFiles = Integer.MAX_VALUE;
        URI finalUri = null;

//        Log.info("URI VEC: " + blobsUri.length);

        for (URI uri : blobsUri) {
//            Log.info("GET BLOB URI: " + uri);
            RestBlobsClient restBlobsServer = new RestBlobsClient(uri);

            int numFiles = restBlobsServer.getNumFiles().value();
//            Log.info("NUMBER OF FILES: " + numFiles);
//            if(numFiles == 0) {
//                return uri;
//            }

            if(minFiles > numFiles ){
                minFiles = numFiles;
                finalUri = uri;
            }
        }
        return finalUri;
    }

    @Override
    public Result<Short> createShort(String userId, String password) {

        Result<User> userRes = restUsersServer.getUser(userId, password);
        User user = userRes.value();

        String shortId = UUID.randomUUID().toString();
        String ownerId = user.getUserId();

        URI blobUri = getBlobUri();
        String blobId = "SH" + UUID.randomUUID();
        String verificationToken = UUID.randomUUID().toString();

        String blobUrl = String.format(BLOB_URL, blobUri, blobId, verificationToken);

        Short shortObj = new Short(shortId, ownerId, blobUrl);

        db.persist(shortObj);

        return Result.ok(shortObj);
    }

    @Override
    public Result<Void> deleteShort(String shortId, String password) {
        var resultsShort = db.sql("SELECT * FROM Short s WHERE s.shortId LIKE '" + shortId + "'", Short.class);

        if(resultsShort.isEmpty()) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        Short shortObj = resultsShort.get(0);
        String userId = shortObj.getOwnerId();

        // Obtem o user, se existir, verificando se a pwd esta correta
        Result<User> userResult = restUsersServer.getUser(userId, password);

        if(userResult.error().equals(Result.ErrorCode.NOT_FOUND)) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } else if(userResult.error().equals(Result.ErrorCode.FORBIDDEN)) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        var resultLikes = db.sql("SELECT * FROM Likes l WHERE l.shortId LIKE '" + shortId + "'", Likes.class);
        for(Likes like: resultLikes) {
            db.delete(like);
        }

        String pattern = "/blobs/(.*?)\\?";

        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(shortObj.getBlobUrl());

        URI blobUri = URI.create(shortObj.getBlobUrl().split("/blobs")[0]);
//        Log.info("BLOB URI: " + blobUri);
        RestBlobsClient restBlobsClient = new RestBlobsClient(blobUri);

        if (m.find()) {
            String blobId = m.group(1);
//            Log.info("BLOBID DELETE SHORT: " + blobId);
            restBlobsClient.delete(blobId);
        }

        db.delete(shortObj);

        return Result.ok();
    }

    @Override
    public Result<Short> getShort(String shortId) {
        var results = db.sql("SELECT * FROM Short s WHERE s.shortId LIKE '" + shortId + "'", Short.class);

        if(results.isEmpty()) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        Short shortObj = results.get(0);

        return Result.ok(shortObj);
    }

    @Override
    public Result<List<String>> getShorts(String userId) {
        Result<List<User>> usersListResult = restUsersServer.searchUsers(userId);
        List<User> usersList = usersListResult.value();

        // Se nenhum user tiver o userId igual ao userId2 return NOT_FOUND
        for(User user : usersList) {
            if (user.getUserId().equals(userId)) {
                var result = db.sql("SELECT s.shortId FROM Short s WHERE s.ownerId LIKE '" + userId + "'", String.class);
                return Result.ok(result);
            }
        }
        return Result.error(Result.ErrorCode.NOT_FOUND);
    }

    // PERGUNTAR: Devemos colocar a informacao dov      follow na BD do server Users ou na do server Shorts
    @Override
    public Result<Void> follow(String userId1, String userId2, boolean isFollowing, String password) {

        // Obtem o user, se existir, verificando se a pwd esta correta
        Result<User> userResult = restUsersServer.getUser(userId1, password);

        if(userResult.error().equals(Result.ErrorCode.NOT_FOUND)) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } else if(userResult.error().equals(Result.ErrorCode.FORBIDDEN)) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        var result = db.sql("SELECT * FROM Follower f WHERE f.follower LIKE '" + userId1 + "' AND f.followed LIKE '" + userId2 + "'", Follower.class);

        if(!result.isEmpty() && isFollowing) {
            return Result.error(Result.ErrorCode.CONFLICT);
        }

        if(result.isEmpty() && !isFollowing) {
            return Result.ok();
        }

        // Obtem todos os users que tenham uma pattern igual a userId2
        Result<List<User>> usersListResult = restUsersServer.searchUsers(userId2);
        List<User> usersList = usersListResult.value();

        // Se nenhum user tiver o userId igual ao userId2 return NOT_FOUND
        for(User followed : usersList) {
            if(followed.getUserId().equals(userId2)) {
                if(isFollowing) {
                    User follower = userResult.value();
                    Follower followerRelationship = new Follower(follower.getUserId(), followed.getUserId());

                    db.persist(followerRelationship);
                } else {
                    Follower followerRelationship = result.get(0);
                    db.delete(followerRelationship);
                }
            } else {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
        }

        return Result.ok();
    }

    @Override
    public Result<List<String>> followers(String userId, String password) {

        // Obtem o user, se existir, verificando se a psw esta correta
        Result<User> userResult = restUsersServer.getUser(userId, password);

        if(userResult.error().equals(Result.ErrorCode.NOT_FOUND)) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } else if(userResult.error().equals(Result.ErrorCode.FORBIDDEN)) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        // Gets all the users that follow another user
        var result = db.sql("SELECT f.follower FROM Follower f WHERE f.followed LIKE '" + userId + "'", String.class);

        return Result.ok(result);
    }

    @Override
    public Result<Void> like(String shortId, String userId, boolean isLiked, String password) {
        var result = db.sql("SELECT * FROM Short s WHERE s.shortId LIKE '" + shortId + "'", Short.class);

        if(result.isEmpty()) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        Result<User> userResult = restUsersServer.getUser(userId, password);

        if(userResult.error().equals(Result.ErrorCode.NOT_FOUND)) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } else if(userResult.error().equals(Result.ErrorCode.FORBIDDEN)) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }


        var result2 = db.sql("SELECT * FROM Likes l WHERE l.userId LIKE '" + userId + "' AND l.shortId LIKE '" + shortId + "'", Likes.class);

        if(isLiked){

            if(!result2.isEmpty()) {
                return Result.error(Result.ErrorCode.CONFLICT);
            }

            Likes likes = new Likes(userId,shortId);
            Short shortObj = result.get(0);

            db.persist(likes);
            shortObj.addLike();
            db.update(shortObj);


        } else {

            if (result2.isEmpty()) {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }

            Likes likes = result2.get(0);
            Short shortObj = result.get(0);

            db.delete(likes);
            shortObj.removeLike();
            db.update(shortObj);

        }
        return Result.ok();
    }

    @Override
    public Result<List<String>> likes(String shortId, String password) {
        var result = db.sql("SELECT * FROM Short s WHERE s.shortID LIKE '" + shortId + "'", Short.class);

        if(result.isEmpty()) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        Short shortObj = result.get(0);
        String userId = shortObj.getOwnerId();

        Result<User> userResult = restUsersServer.getUser(userId, password);
        if(userResult.error().equals(Result.ErrorCode.NOT_FOUND)) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } else if(userResult.error().equals(Result.ErrorCode.FORBIDDEN)) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        var likes = db.sql("SELECT * FROM Likes l WHERE l.shortId LIKE '" + shortId + "'", Likes.class);

        List<String> usersThatLiked = new ArrayList<>();
        for(Likes like: likes) {
            usersThatLiked.add(like.getUserId());
        }

        //supostamente é para retornar uma lista de Strings vazia??
        return Result.ok(usersThatLiked);
    }

    @Override
    public Result<List<String>> getFeed(String userId, String password) {
        Result<User> userResult = restUsersServer.getUser(userId, password);
//        Log.info("GET FEED: " + userResult.toString());

        if(userResult.error().equals(Result.ErrorCode.NOT_FOUND)) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } else if(userResult.error().equals(Result.ErrorCode.FORBIDDEN)) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        var followedUsers = db.sql("SELECT f.followed FROM Follower f WHERE f.follower LIKE '" + userId + "'", String.class);
        //o proprio user tambem faz parte do feed
        followedUsers.add(userId);

        List<Short> feed = new ArrayList<>();
        for(String followedUser: followedUsers) {
            feed.addAll(db.sql("SELECT * FROM Short s WHERE s.ownerId LIKE '" + followedUser + "'", Short.class));
        }


        Comparator<Short> comparator = new Comparator<Short>() {
            @Override
            public int compare(Short o1, Short o2) {
                long timestamp1 = o1.getTimestamp();
                long timestamp2 = o2.getTimestamp();
                return Long.compare(timestamp2, timestamp1);
            }
        };

        feed.sort(comparator);

        List<String> feedIds = new ArrayList<>();
        for(Short shortObj: feed){
            feedIds.add(shortObj.getShortId());
        }

        return Result.ok(feedIds);
    }

    @Override
    public Result<Void> deleteUser(String userId) {

        var shortsToDelete = db.sql("SELECT * FROM Short s WHERE s.ownerId LIKE '" + userId + "'", Short.class);

        if(!shortsToDelete.isEmpty()) {
            db.delete(shortsToDelete);
        }

        var unfollowFollowers = db.sql("SELECT * FROM Follower f WHERE f.followed LIKE '" + userId + "'", Follower.class);

        if(!unfollowFollowers.isEmpty()) {
            db.delete(unfollowFollowers);
        }

        var followersUnfollow = db.sql("SELECT * FROM Follower f WHERE f.follower LIKE '" + userId + "'", Follower.class);

        if(!followersUnfollow.isEmpty()) {
            db.delete(followersUnfollow);
        }

        var likesToRemove = db.sql("SELECT * FROM Likes l WHERE l.userId LIKE '" + userId + "'", Likes.class);

        if(!likesToRemove.isEmpty()) {
            db.delete(likesToRemove);
        }

        return Result.ok();
    }


}
