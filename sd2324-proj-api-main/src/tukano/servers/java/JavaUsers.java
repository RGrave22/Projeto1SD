package tukano.servers.java;

import tukano.api.User;
import tukano.api.java.Result;
import tukano.api.java.Users;
import tukano.persistence.Hibernate;
import tukano.servers.ClientFactory;

import java.util.List;

public class JavaUsers implements Users {

    private final Hibernate db = Hibernate.getInstance();

    @Override
    public Result<String> createUser(User user) {

        if(user.getUserId() == null || user.getPwd() == null || user.getDisplayName() == null || user.getEmail() == null) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        var results = db.sql("SELECT * FROM User u WHERE u.userId LIKE '" + user.getUserId() + "'", User.class);
        if(!results.isEmpty()) {
            return Result.error(Result.ErrorCode.CONFLICT);
        }

        db.persist(user);

        return Result.ok(user.getUserId());
    }

    @Override
    public Result<User> getUser(String userId, String pwd) {

        var results = db.sql("SELECT * FROM User u WHERE u.userId LIKE '" + userId + "'", User.class);

        if(results.isEmpty() ) {
            return Result.error( Result.ErrorCode.NOT_FOUND);
        }

        if(userId == null || pwd == null) {
            return Result.error( Result.ErrorCode.FORBIDDEN);
        }

        User user = results.get(0);
        if(!pwd.equals(user.getPwd())) {
            return Result.error( Result.ErrorCode.FORBIDDEN);
        }

        return Result.ok(user);
    }

    @Override
    public Result<User> updateUser(String userId, String pwd, User newUser) {

        if(userId == null || pwd == null) {
            return Result.error( Result.ErrorCode.BAD_REQUEST);
        }

        if(newUser.getUserId() != null) {
            if(!newUser.getUserId().equals(userId))
                return Result.error( Result.ErrorCode.BAD_REQUEST);
        }

        var results = db.sql("SELECT * FROM User u WHERE u.userId LIKE '" + userId + "'", User.class);

        if( results.isEmpty() ) {
            return Result.error( Result.ErrorCode.NOT_FOUND);
        }

        User user = results.get(0);

        if(!pwd.equals(user.getPwd())) {
            return Result.error( Result.ErrorCode.FORBIDDEN);
        }

        if(newUser.getPwd() != null) {
            user.setPwd(newUser.getPwd());
        }

        if(newUser.getEmail() != null) {
            user.setEmail(newUser.getEmail());
        }

        if(newUser.getDisplayName() != null) {
            user.setDisplayName(newUser.getDisplayName());
        }

        db.update(user);

        return Result.ok(user);
    }

    @Override
    public Result<User> deleteUser(String userId, String pwd) {
        var results = db.sql("SELECT * FROM User u WHERE u.userId LIKE '" + userId + "'", User.class);

        if( results.isEmpty() ) {
            return Result.error( Result.ErrorCode.NOT_FOUND);
        }

        if(userId == null || pwd == null) {
            return Result.error( Result.ErrorCode.FORBIDDEN);
        }

        User user = results.get(0);

        if(!pwd.equals(user.getPwd())) {
            return Result.error( Result.ErrorCode.FORBIDDEN);
        }

        ClientFactory.getShort().deleteUser(userId);

        db.delete(user);

        return Result.ok(user);
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        var results = db.sql("SELECT * FROM User u WHERE LOWER(u.userId) LIKE LOWER('%" + pattern + "%')", User.class);

        return Result.ok(results);
    }
}
