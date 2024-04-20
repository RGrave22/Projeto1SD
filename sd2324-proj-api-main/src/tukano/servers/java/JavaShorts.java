package tukano.servers.java;

import tukano.api.Follower;
import tukano.api.Likes;
import tukano.api.Short;
import tukano.api.User;
import tukano.api.java.Blobs;
import tukano.api.java.Result;
import tukano.api.java.Shorts;

import tukano.discovery.Discovery;
import tukano.persistence.Hibernate;
import tukano.servers.ClientFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.net.URI;

public class JavaShorts implements Shorts {

    private static final String BLOB_URL = "%s/blobs/%s?verifier=%s";
    private static final String SERVICE_NAME = "blobs";
    private static final int MIN_REPLIES = 1;
    private static final String PATTERN = "/blobs/(.*?)\\?";
    private static final String BLOB_REGEX = "/blobs";

    Hibernate db = Hibernate.getInstance();


    private URI getBlobUri() {

        URI[] blobsUri = Discovery.getInstance().knownUrisOf(SERVICE_NAME, MIN_REPLIES);

        int minFiles = Integer.MAX_VALUE;
        URI finalUri = null;

        List<URI> diffUris = new ArrayList<>();
        for (URI uri : blobsUri) {
            if(!diffUris.contains(uri)){
                diffUris.add(uri);
            }
        }

        for (URI uri : diffUris) {
            Blobs blobsClient = ClientFactory.getBlob(uri);
            int numFiles = blobsClient.getNumFiles().value();

            if(minFiles > numFiles ){
                minFiles = numFiles;
                finalUri = uri;
            }
        }

        return finalUri;
    }

    @Override
    public Result<Short> createShort(String userId, String password) {

        Result<User> resultsUser = ClientFactory.getUsers().getUser(userId, password);

        if(!resultsUser.isOK()) {
            return Result.error(resultsUser.error());
        }

//        if(userRes.error().equals(Result.ErrorCode.NOT_FOUND)) {
//            return Result.error(Result.ErrorCode.NOT_FOUND);
//        } else if(userRes.error().equals(Result.ErrorCode.FORBIDDEN)) {
//            return Result.error(Result.ErrorCode.FORBIDDEN);
//        }

        String shortId = UUID.randomUUID().toString();
        URI blobUri = getBlobUri();
        String blobId = "SH" + UUID.randomUUID();
        String verificationToken = UUID.randomUUID().toString();

        String blobUrl = String.format(BLOB_URL, blobUri, blobId, verificationToken);

        Short shortObj = new Short(shortId, userId, blobUrl);

        db.persist(shortObj);

        return Result.ok(shortObj);
    }

    @Override
    public Result<Void> deleteShort(String shortId, String password) {

        var shortsList = db.sql("SELECT * FROM Short s WHERE s.shortId LIKE '" + shortId + "'", Short.class);

        if(shortsList.isEmpty()) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        Short shortObj = shortsList.get(0);
        String userId = shortObj.getOwnerId();

        Result<User> resultsUser = ClientFactory.getUsers().getUser(userId, password);

        if(!resultsUser.isOK()) {
            return Result.error(resultsUser.error());
        }

//        if(userResult.error().equals(Result.ErrorCode.NOT_FOUND)) {
//            return Result.error(Result.ErrorCode.NOT_FOUND);
//        } else if(userResult.error().equals(Result.ErrorCode.FORBIDDEN)) {
//            return Result.error(Result.ErrorCode.FORBIDDEN);
//        }

        var likesList = db.sql("SELECT * FROM Likes l WHERE l.shortId LIKE '" + shortId + "'", Likes.class);
        for(Likes like: likesList) {
            db.delete(like);
        }


        Pattern r = Pattern.compile(PATTERN);
        Matcher m = r.matcher(shortObj.getBlobUrl());

        URI blobUri = URI.create(shortObj.getBlobUrl().split(BLOB_REGEX)[0]);

        if (m.find()) {
            String blobId = m.group(1);
            ClientFactory.getBlob(blobUri).delete(blobId);
        }

        db.delete(shortObj);

        return Result.ok();
    }

    @Override
    public Result<Short> getShort(String shortId) {
        var shortsList = db.sql("SELECT * FROM Short s WHERE s.shortId LIKE '" + shortId + "'", Short.class);

        if(shortsList.isEmpty()) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        Short shortObj = shortsList.get(0);

        return Result.ok(shortObj);
    }

    @Override
    public Result<List<String>> getShorts(String userId) {
        List<User> usersList = ClientFactory.getUsers().searchUsers(userId).value();

        for(User user : usersList) {
            if (user.getUserId().equals(userId)) {
                var shortsList = db.sql("SELECT s.shortId FROM Short s WHERE s.ownerId LIKE '" + userId + "'", String.class);
                return Result.ok(shortsList);
            }
        }
        return Result.error(Result.ErrorCode.NOT_FOUND);
    }

    @Override
    public Result<Void> follow(String userId1, String userId2, boolean isFollowing, String password) {

        Result<User> resultsUser = ClientFactory.getUsers().getUser(userId1, password);

        if(!resultsUser.isOK()) {
            return Result.error(resultsUser.error());
        }

//        if(userResult.error().equals(Result.ErrorCode.NOT_FOUND)) {
//            return Result.error(Result.ErrorCode.NOT_FOUND);
//        } else if(userResult.error().equals(Result.ErrorCode.FORBIDDEN)) {
//            return Result.error(Result.ErrorCode.FORBIDDEN);
//        }

        var followList = db.sql("SELECT * FROM Follower f WHERE f.follower LIKE '" + userId1 + "' AND f.followed LIKE '" + userId2 + "'", Follower.class);

        if(!followList.isEmpty() && isFollowing) {
            return Result.error(Result.ErrorCode.CONFLICT);
        }

        if(followList.isEmpty() && !isFollowing) {
            return Result.ok();
        }

        List<User> usersList = ClientFactory.getUsers().searchUsers(userId2).value();

        for(User followed : usersList) {
            if(followed.getUserId().equals(userId2)) {
                if(isFollowing) {
                    User follower = resultsUser.value();
                    Follower followerRelationship = new Follower(follower.getUserId(), followed.getUserId());

                    db.persist(followerRelationship);
                } else {
                    Follower followerRelationship = followList.get(0);
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

        Result<User> resultsUser = ClientFactory.getUsers().getUser(userId, password);

        if(!resultsUser.isOK()) {
            return Result.error(resultsUser.error());
        }
//        if(userResult.error().equals(Result.ErrorCode.NOT_FOUND)) {
//            return Result.error(Result.ErrorCode.NOT_FOUND);
//        } else if(userResult.error().equals(Result.ErrorCode.FORBIDDEN)) {
//            return Result.error(Result.ErrorCode.FORBIDDEN);
//        }

        var followersList = db.sql("SELECT f.follower FROM Follower f WHERE f.followed LIKE '" + userId + "'", String.class);

        return Result.ok(followersList);
    }

    @Override
    public Result<Void> like(String shortId, String userId, boolean isLiked, String password) {
        var shortsList = db.sql("SELECT * FROM Short s WHERE s.shortId LIKE '" + shortId + "'", Short.class);

        if(shortsList.isEmpty()) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        Result<User> resultsUser = ClientFactory.getUsers().getUser(userId, password);

        if(!resultsUser.isOK()) {
            return Result.error(resultsUser.error());
        }

//        if(userResult.error().equals(Result.ErrorCode.NOT_FOUND)) {
//            return Result.error(Result.ErrorCode.NOT_FOUND);
//        } else if(userResult.error().equals(Result.ErrorCode.FORBIDDEN)) {
//            return Result.error(Result.ErrorCode.FORBIDDEN);
//        }

        var likesList = db.sql("SELECT * FROM Likes l WHERE l.userId LIKE '" + userId + "' AND l.shortId LIKE '" + shortId + "'", Likes.class);

        if(isLiked){
            if(!likesList.isEmpty()) {
                return Result.error(Result.ErrorCode.CONFLICT);
            }

            Likes likes = new Likes(userId,shortId);
            Short shortObj = shortsList.get(0);

            db.persist(likes);
            shortObj.addLike();
            db.update(shortObj);
        } else {
            if (likesList.isEmpty()) {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }

            Likes likes = likesList.get(0);
            Short shortObj = shortsList.get(0);

            db.delete(likes);
            shortObj.removeLike();
            db.update(shortObj);
        }
        return Result.ok();
    }

    @Override
    public Result<List<String>> likes(String shortId, String password) {
        var shortsList = db.sql("SELECT * FROM Short s WHERE s.shortID LIKE '" + shortId + "'", Short.class);

        if(shortsList.isEmpty()) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        Short shortObj = shortsList.get(0);
        String userId = shortObj.getOwnerId();

        Result<User> resultsUser = ClientFactory.getUsers().getUser(userId, password);

        if(!resultsUser.isOK()) {
            return Result.error(resultsUser.error());
        }

//        if(userResult.error().equals(Result.ErrorCode.NOT_FOUND)) {
//            return Result.error(Result.ErrorCode.NOT_FOUND);
//        } else if(userResult.error().equals(Result.ErrorCode.FORBIDDEN)) {
//            return Result.error(Result.ErrorCode.FORBIDDEN);
//        }

        var likesList = db.sql("SELECT * FROM Likes l WHERE l.shortId LIKE '" + shortId + "'", Likes.class);

        List<String> usersThatLiked = new ArrayList<>();
        for(Likes like: likesList) {
            usersThatLiked.add(like.getUserId());
        }

        return Result.ok(usersThatLiked);
    }

    @Override
    public Result<List<String>> getFeed(String userId, String password) {
        Result<User> resultsUser = ClientFactory.getUsers().getUser(userId, password);

        if(!resultsUser.isOK()) {
            return Result.error(resultsUser.error());
        }

//        if(userResult.error().equals(Result.ErrorCode.NOT_FOUND)) {
//            return Result.error(Result.ErrorCode.NOT_FOUND);
//        } else if(userResult.error().equals(Result.ErrorCode.FORBIDDEN)) {
//            return Result.error(Result.ErrorCode.FORBIDDEN);
//        }

        var followedUsers = db.sql("SELECT f.followed FROM Follower f WHERE f.follower LIKE '" + userId + "'", String.class);

        followedUsers.add(userId);

        List<Short> feed = new ArrayList<>();
        for(String followedUser: followedUsers) {
            feed.addAll(db.sql("SELECT * FROM Short s WHERE s.ownerId LIKE '" + followedUser + "'", Short.class));
        }

        Comparator<Short> comparator = (o1, o2) -> {
            long timestamp1 = o1.getTimestamp();
            long timestamp2 = o2.getTimestamp();
            return Long.compare(timestamp2, timestamp1);
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
        var likesToRemove = db.sql("SELECT * FROM Likes l WHERE l.userId LIKE '" + userId + "'", Likes.class);

        if(!likesToRemove.isEmpty()) {
            for(Likes likeObj: likesToRemove) {
                var shortObj = db.sql("SELECT * FROM Short s WHERE s.shortId LIKE '" + likeObj.getShortId() + "'", Short.class);
//                if(!shortObj.isEmpty()) {
                Short sh = shortObj.get(0);
                sh.removeLike();
                db.update(sh);
                db.delete(likeObj);
//                }
            }
        }

        var shortsToDelete = db.sql("SELECT * FROM Short s WHERE s.ownerId LIKE '" + userId + "'", Short.class);

        if(!shortsToDelete.isEmpty()) {
            for(Short shortObj: shortsToDelete) {
                Pattern r = Pattern.compile(PATTERN);
                Matcher m = r.matcher(shortObj.getBlobUrl());

                URI blobUri = URI.create(shortObj.getBlobUrl().split(BLOB_REGEX)[0]);
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

//        var likesToRemove = db.sql("SELECT * FROM Likes l WHERE l.userId LIKE '" + userId + "'", Likes.class);
//
//        if(!likesToRemove.isEmpty()) {
//            for(Likes likeObj: likesToRemove) {
//                var shortObj = db.sql("SELECT * FROM Short s WHERE s.shortId LIKE '" + likeObj.getShortId() + "'", Short.class);
//                if(!shortObj.isEmpty()) {
//                    Short sh = shortObj.get(0);
//                    sh.removeLike();
//                    db.update(sh);
//                    db.delete(likeObj);
//                }
//            }
//        }

        return Result.ok();
    }
}