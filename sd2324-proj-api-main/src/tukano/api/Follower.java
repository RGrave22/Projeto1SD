package tukano.api;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
public class Follower {

//    @Id
//    private String id;
    @Id
    private String follower;
    @Id
    private String followed;

    public Follower() {}

    public Follower(String follower, String followed) {
//        this.id = UUID.randomUUID().toString();
        this.follower = follower;
        this.followed = followed;
    }

    public String getFollower() {
        return follower;
    }

    public String getFollowed() {
        return followed;
    }

    public void setFollower(String follower) {
        this.follower = follower;
    }

    public void setFollowed(String followed) {
        this.followed = followed;
    }
}
