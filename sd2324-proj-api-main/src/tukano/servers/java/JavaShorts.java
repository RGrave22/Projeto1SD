package tukano.servers.java;

import tukano.api.Follower;
import tukano.api.Likes;
import tukano.api.Short;
import tukano.api.User;
import tukano.api.java.Blobs;
import tukano.api.java.Result;
import tukano.api.java.Shorts;

import tukano.api.java.Users;
import tukano.discovery.Discovery;
import tukano.persistence.Hibernate;
import tukano.servers.ClientFactory;

import java.io.Console;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.net.URI;

public class JavaShorts implements Shorts {

    private static final String FILE_PATH = "%s\\BlobFiles\\%s";

    private static final String BLOB_URL = "%s/blobs/%s?verifier=%s";

    int lastUsed;

    Hibernate db = Hibernate.getInstance();
//    Discovery discovery = Discovery.getInstance();
//    Users usersClient = ClientFactory.getUsers();

//    Quando hover mais do que um servidor de blobs fazer:
//    E quando for para dar delete a um short ver no vetor de servidoresde blobs qual o servido correspondente ao blobUrl do short


    private static Logger Log = Logger.getLogger(JavaShorts.class.getName());

    private URI getBlobUri() {

//        Log.info("GETBLOBURI");
        URI[] blobsUri = Discovery.getInstance().knownUrisOf("blobs", 1);
        Log.info("BLOBSURI : " + blobsUri.length);

        int minFiles = Integer.MAX_VALUE;
        URI finalUri = null;

        Log.info("URI VEC: " + blobsUri.length);

        List<URI> diffUris = new ArrayList<>();
        for (URI uri : blobsUri) {
            if(!diffUris.contains(uri)){
                diffUris.add(uri);

            }
        }

        for (URI uri : diffUris) {
            Log.info("GET BLOB URI: " + uri);
            Blobs blobsClient = ClientFactory.getBlob(uri);
            Log.info("BLOBCLIENT");
            int numFiles = blobsClient.getNumFiles().value();
            Log.info("NUMBER OF FILES: " + numFiles);
            if(numFiles == 0) {
                return uri;
            }

            if(minFiles > numFiles ){
                minFiles = numFiles;
                finalUri = uri;
            }
        }

        return finalUri;

////        int lastUsed = -1;
//        Random random = new Random();
//        int randomGetted = -1;
//
//        do{
//          //  randomGetted = random.nextInt(diffUris.size());
//            randomGetted = (int) (Math.random() * diffUris.size());
//
//        }while(randomGetted == lastUsed);
//
////        while(randomGetted == lastUsed) {
////            randomGetted = random.nextInt(diffUris.size());
////        }
//
//
//        URI finalUri = diffUris.get(randomGetted);
//        lastUsed = randomGetted;
//        Log.info("RANDOM : " + lastUsed);
//
//        Log.info("RANDOM URI: " + diffUris.size());
//
//
//        return finalUri;

//        Log.info("LAST USED : " + lastUsed);
//        List<URI> diffUris = new ArrayList<>();
//        for (URI uri : blobsUri) {
//            if (!diffUris.contains(uri)) {
//                diffUris.add(uri);
//            }
//        }
//
//        int nextIndex;
//
//        if (lastUsed == -1 || lastUsed == diffUris.size() - 1) {
//            nextIndex = 0;
//        } else {
//            nextIndex = lastUsed + 1;
//        }
//
//        URI finalUri = diffUris.get(nextIndex);
//        lastUsed = nextIndex;
//
//        return finalUri;
    }

    @Override
    public Result<Short> createShort(String userId, String password) {

        Result<User> userRes = ClientFactory.getUsers().getUser(userId, password);

        if(userRes.error().equals(Result.ErrorCode.NOT_FOUND)) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } else if(userRes.error().equals(Result.ErrorCode.FORBIDDEN)) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        User user = userRes.value();

        String shortId = UUID.randomUUID().toString();

        URI blobUri = getBlobUri();
        String blobId = "SH" + UUID.randomUUID();
        String verificationToken = UUID.randomUUID().toString();

        String blobUrl = String.format(BLOB_URL, blobUri, blobId, verificationToken);

        Short shortObj = new Short(shortId, userId, blobUrl);

        Log.info("USERID BEFORE: " + shortObj.getOwnerId());
        db.persist(shortObj);

        Log.info("USERID AFTER: " + shortObj.getOwnerId());

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
        Result<User> userResult = ClientFactory.getUsers().getUser(userId, password);

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

//        Blobs blobsClient = ClientFactory.getBlob(blobUri);

        if (m.find()) {
            String blobId = m.group(1);
//            Log.info("BLOBID DELETE SHORT: " + blobId);
            ClientFactory.getBlob(blobUri).delete(blobId);
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
        Result<List<User>> usersListResult = ClientFactory.getUsers().searchUsers(userId);
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
        Result<User> userResult = ClientFactory.getUsers().getUser(userId1, password);

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
        Result<List<User>> usersListResult = ClientFactory.getUsers().searchUsers(userId2);
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
        Result<User> userResult = ClientFactory.getUsers().getUser(userId, password);

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

        Result<User> userResult = ClientFactory.getUsers().getUser(userId, password);

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

        Result<User> userResult = ClientFactory.getUsers().getUser(userId, password);
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
        Result<User> userResult = ClientFactory.getUsers().getUser(userId, password);
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
            for(Short shortObj: shortsToDelete) {
                String pattern = "/blobs/(.*?)\\?";
                Pattern r = Pattern.compile(pattern);
                Matcher m = r.matcher(shortObj.getBlobUrl());

                URI blobUri = URI.create(shortObj.getBlobUrl().split("/blobs")[0]);
                if (m.find()) {
                    String blobId = m.group(1);
                    ClientFactory.getBlob(blobUri).delete(blobId);
                }
                db.delete(shortObj);
            }
        }

        var followsToRemove = db.sql("SELECT * FROM Follower f WHERE f.followed LIKE '" + userId + "' OR f.follower LIKE '" + userId + "'", Follower.class);

        if(!followsToRemove.isEmpty()) {
            for(Follower followerObj: followsToRemove) {
                db.delete(followerObj);
            }
        }

        var likesToRemove = db.sql("SELECT * FROM Likes l WHERE l.userId LIKE '" + userId + "'", Likes.class);

        if(!likesToRemove.isEmpty()) {
            for(Likes likeObj: likesToRemove) {
                var shortObj = db.sql("SELECT * FROM Short s WHERE s.shortId LIKE '" + likeObj.getShortId() + "'", Short.class);
                if(!shortObj.isEmpty()) {
                    Short sh = shortObj.get(0);
                    sh.removeLike();
                    db.update(sh);
                    db.delete(likeObj);
                }
            }
        }

        return Result.ok();
    }
}