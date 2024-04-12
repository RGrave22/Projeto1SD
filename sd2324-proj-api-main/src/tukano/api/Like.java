package tukano.api;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
public class Like {

    @Id
    String id = this.id = UUID.randomUUID().toString();


    String userId;

    String shortId;


    public Like() {}

    public Like(String userId, String shortId) {
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
