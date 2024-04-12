package tukano.api;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import tukano.api.rest.RestFollower;

import java.util.UUID;

@Entity
public class Follower implements RestFollower {

    @Id
    private String id;


    private String follower;
    private String followed;


//    private User follower;
//
//
//    private User followed;


    public Follower() {}

    public Follower(String follower, String followed) {
        this.id = UUID.randomUUID().toString();
        this.follower = follower;
        this.followed = followed;
    }

    public String bgetFollower() {
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
