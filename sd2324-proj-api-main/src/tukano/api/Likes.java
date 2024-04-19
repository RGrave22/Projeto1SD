package tukano.api;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
public class Likes {

    @Id
    private String id;


    private String userId;

    private String shortId;


    public Likes() {}

    public Likes(String userId, String shortId) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.shortId = shortId;
    }



    public String getId() {
        return id;
    }

    public String getShortId() {
        return shortId;
    }

    public String getUserId() {
        return userId;
    }

}
