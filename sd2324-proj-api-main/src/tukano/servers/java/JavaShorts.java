package tukano.servers.java;


import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import tukano.api.Follower;
import tukano.api.Like;
import tukano.api.Short;
import tukano.api.User;
import tukano.api.java.Result;
import tukano.api.java.Shorts;
import tukano.clients.rest.RestUsersClient;
import tukano.discovery.Discovery;
import tukano.persistence.Hibernate;


import java.util.List;
import java.util.UUID;

public class JavaShorts implements Shorts {

    Hibernate db = Hibernate.getInstance();
    Discovery discovery = Discovery.getInstance();
    RestUsersClient restUsersServer = new RestUsersClient(discovery.knownUrisOf("users", 1)[0]);



    @Override
    public Result<Short> createShort(String userId, String password) {

        Result<User> userRes = restUsersServer.getUser(userId, password);
        User user = userRes.value();

        String shortId = UUID.randomUUID().toString();
        String ownerId = user.getUserId();
        String blobUrl = "null";
        long timestamp = System.currentTimeMillis();
        int totalLikes = 0;

        Short shortObj = new Short(shortId, ownerId, blobUrl, timestamp, totalLikes);

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
        var resultsUser = db.sql("SELECT * FROM User u WHERE u.userId LIKE '" + userId + "'", User.class);

        User user = resultsUser.get(0);

        if(user.getPwd().equals(password)) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

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
        var result = db.sql("SELECT * FROM Short s WHERE s.userId LIKE '" + userId + "'", String.class);

        if(result.isEmpty()) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        return Result.ok(result);
    }

    @Override
    public Result<Void> follow(String userId1, String userId2, boolean isFollowing, String password) {

        // Obtem o user, se existir, verificando se a psw esta correta
        Result<User> userResult = restUsersServer.getUser(userId1, password);

        if(userResult.equals(Result.error(Result.ErrorCode.NOT_FOUND))) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } else if(userResult.equals(Result.error(Result.ErrorCode.FORBIDDEN))) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        // Obtem todos os users que tenham uma pattern igual a userId2
        Result<List<User>> usersListResult = restUsersServer.searchUsers(userId2);
        List<User> usersList = usersListResult.value();



        // Se nenhum user tiver o userId igual ao userId2 return NOT_FOUND
        for(User followed : usersList) {
            if(followed.getUserId().equals(userId2)) {
                User follower = userResult.value();
                Follower followerRelationship = new Follower(follower.getUserId(), followed.getUserId());
                if(isFollowing) {
                    // Ao user1 adicionar à lista de followers o user2
                    db.persist(followerRelationship);
                } else {
                    // Ao user1 remover da lista de followers o user2
                    db.delete(followerRelationship);
                }
            } else {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
        }

        //Se nenhum user tiver o userId igual ao userId2 return NOT_FOUND
//        for(User followed : usersList) {
//            if(followed.getUserId().equals(userId2)) {
//                User follower = userResult.value();
//                Follower followerRelationship = new Follower(follower, followed);
//                if(isFollowing) {
//                    // Ao user1 adicionar ah lista de followers o user2
//                    db.persist(followerRelationship);
//                } else {
//                    // Ao user1 remover da lista de followers o user2
//                    db.delete(followerRelationship);
//                }
//            } else {
//                return Result.error(Result.ErrorCode.NOT_FOUND);
//            }
//        }

        return Result.ok();
    }

    @Override
    public Result<List<String>> followers(String userId, String password) {

        // Obtem o user, se existir, verificando se a psw esta correta
        Result<User> userResult = restUsersServer.getUser(userId, password);

        if(userResult.equals(Result.error(Result.ErrorCode.NOT_FOUND))) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } else if(userResult.equals(Result.error(Result.ErrorCode.FORBIDDEN))) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        User user = userResult.value();




        return null;
    }

    @Override
    public Result<Void> like(String shortId, String userId, boolean isLiked, String password) {
        var result = db.sql("SELECT * FROM Shorts s WHERE s.shortID LIKE '" + shortId + "'", Short.class);

        if(result.isEmpty()) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        Result<User> userResult = restUsersServer.getUser(userId, password);

        if(userResult.equals(Result.error(Result.ErrorCode.NOT_FOUND))) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } else if(userResult.equals(Result.error(Result.ErrorCode.FORBIDDEN))) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }


        var result2 = db.sql("SELECT * FROM Likes l WHERE l.userId LIKE '" + userId + "' AND l.shortId LIKE '" + shortId + "'", Like.class);
        if(isLiked){

            if(!result2.isEmpty()) {
                return Result.error(Result.ErrorCode.CONFLICT);
            }

            Like like = new Like(userId,shortId);
            Short shortObj = result.get(0);

            shortObj.addLike();

            db.persist(like);

        } else {

            if (result2.isEmpty()) {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }

            Like like = result2.get(0);
            Short shortObj = result.get(0);

            shortObj.removeLike();

            db.delete(like);
        }
        return Result.ok();
    }

    @Override
    public Result<List<String>> likes(String shortId, String password) {
        var result = db.sql("SELECT * FROM Shorts s WHERE s.shortID LIKE '" + shortId + "'", Short.class);

        if(result.isEmpty()) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        Short shortObj = result.get(0);
        String userId = shortObj.getOwnerId();

        Result<User> userResult = restUsersServer.getUser(userId, password);
        if(userResult.equals(Result.error(Result.ErrorCode.NOT_FOUND))) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } else if(userResult.equals(Result.error(Result.ErrorCode.FORBIDDEN))) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        var likes = db.sql("SELECT * FROM Likes l WHERE l.shortId LIKE '" + shortId + "'", Like.class);

        //supostamente é para retornar uma lista de Strings vazia??
        return Result.ok(likes);
    }

    @Override
    public Result<List<String>> getFeed(String userId, String password) {
        return null;
    }




}
