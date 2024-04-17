package tukano.servers.java;

import tukano.api.User;
import tukano.api.java.Result;
import tukano.api.java.Users;
import tukano.clients.rest.RestShortsClient;
import tukano.discovery.Discovery;
import tukano.persistence.Hibernate;

import java.util.List;
import java.util.logging.Logger;

public class JavaUsers implements Users {

    private static Logger Log = Logger.getLogger(JavaUsers.class.getName());

    Hibernate db = Hibernate.getInstance();
    Discovery discovery = Discovery.getInstance();
    RestShortsClient restShortsServer = new RestShortsClient(discovery.knownUrisOf("shorts", 1)[0]);


    @Override
    public Result<String> createUser(User user) {
//        Verificar se o user é valido
        if(user.getUserId() == null || user.getPwd() == null || user.getDisplayName() == null || user.getEmail() == null) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

//        Verificar se o user já existe na BD
        var results = db.sql("SELECT * FROM User u WHERE u.userId LIKE '" + user.getUserId() + "'", User.class);
        if(!results.isEmpty()) {
            return Result.error(Result.ErrorCode.CONFLICT);
        }

        db.persist(user);
        return Result.ok(user.getUserId());
    }

    @Override
    public Result<User> getUser(String userId, String pwd) {

        // Check if user exists
        var results = db.sql("SELECT * FROM User u WHERE u.userId LIKE '" + userId + "'", User.class);

        if(results.isEmpty() ) {
            return Result.error( Result.ErrorCode.NOT_FOUND);
        }

        // Check if userId and pwd is valid
        if(userId == null || pwd == null) {
            return Result.error( Result.ErrorCode.FORBIDDEN);
        }

        //Check if the password is correct
        User user = results.get(0);
        if(!pwd.equals(user.getPwd())) {
            return Result.error( Result.ErrorCode.FORBIDDEN);
        }

        return Result.ok(user);
    }

    @Override
    public Result<User> updateUser(String userId, String pwd, User newUser) {

        // Check if the user tries to update the userId, or if userId, or pwd is valid
        if(newUser.getUserId() != null || userId == null || pwd == null){
            return Result.error( Result.ErrorCode.BAD_REQUEST);
        }

        var results = db.sql("SELECT * FROM User u WHERE u.userId LIKE '" + userId + "'", User.class);

        // Check if the user exists
        if( results.isEmpty() ) {
            return Result.error( Result.ErrorCode.NOT_FOUND);
        }

        User user = results.get(0);

        //Check if the password is correct
        if(!pwd.equals(user.getPwd())) {
            return Result.error( Result.ErrorCode.FORBIDDEN);
        }

        // Updates the pwd
        if(newUser.getPwd() != null) {
            user.setPwd(newUser.getPwd());
        }

        //Updates the email
        if(newUser.getEmail() != null) {
            user.setEmail(newUser.getEmail());
        }

        // Updates the display name
        if(newUser.getDisplayName() != null) {
            user.setDisplayName(newUser.getDisplayName());
        }

        db.update(user);

        return Result.ok(user);
    }

    @Override
    public Result<User> deleteUser(String userId, String pwd) {
        var results = db.sql("SELECT * FROM User u WHERE u.userId LIKE '" + userId + "'", User.class);

        // Check if the user exists
        if( results.isEmpty() ) {
            return Result.error( Result.ErrorCode.NOT_FOUND);
        }

        // Check if userId and pwd is valid
        if(userId == null || pwd == null) {
            return Result.error( Result.ErrorCode.FORBIDDEN);
        }

        User user = results.get(0);
        //Check if the password is correct
        if(!pwd.equals(user.getPwd())) {
            return Result.error( Result.ErrorCode.FORBIDDEN);
        }

//        try {

            Result<List<String>> shortsClient = restShortsServer.getShorts(userId);

            if(shortsClient.error().equals(Result.ErrorCode.OK)) {

                List<String> shortsList = shortsClient.value();

                for(String shortId: shortsList) {
                    restShortsServer.deleteShort(shortId, pwd);
                }
            }

            Result<List<String>> shortsFollowers = restShortsServer.followers(userId, pwd);

            if(shortsFollowers.isOK()) {
                List<String> shortsListFollowers = shortsFollowers.value();

                for (String userId2 : shortsListFollowers) {
                    restShortsServer.follow(userId, userId2, false, pwd);

                    var resultUser = db.sql("SELECT u.pwd FROM User u WHERE u.userId LIKE '" + userId2 + "'", String.class);
                    String pwd2 = resultUser.get(0);
                    restShortsServer.follow(userId2, userId, false, pwd2);
                }
            }

            var allUsers = db.sql("SELECT * FROM User u", User.class);

            for(User u: allUsers) {
                Result<List<String>> shortsFromUserRequest = restShortsServer.getShorts(u.getUserId());
                List<String> shortsFromUser = shortsFromUserRequest.value();
                for(String shortId: shortsFromUser) {
                    restShortsServer.like(shortId, userId, false, pwd);
                }
            }



//        } finally {
            db.delete(user);
//        }
        return Result.ok(user);
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        var results = db.sql("SELECT * FROM User u WHERE LOWER(u.userId) LIKE LOWER('%" + pattern + "%')", User.class);

        return Result.ok(results);
    }
}
