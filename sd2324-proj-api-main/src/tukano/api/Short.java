package tukano.api;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;


/**
 * Represents a Short video uploaded by an user.
 * 
 * A short has an unique shortId and is owned by a given user; 
 * Comprises of a short video, stored as a binary blob at some bloburl;.
 * A post also has a number of likes, which can increase or decrease over time. It is the only piece of information that is mutable.
 * A short is timestamped when it is created.
 *
 */
@Entity
public class Short {
	
	@Id
	String shortId;
	String ownerId;
	String blobUrl;
	long timestamp;
	int totalLikes;

	public Short() {}
	
	public Short(String shortId, String ownerId, String blobUrl, long timestamp, int totalLikes) {
		super();
		this.shortId = shortId;
		this.ownerId = ownerId;
		this.blobUrl = blobUrl;
		this.timestamp = timestamp;
		this.totalLikes = totalLikes;
	}

	public Short(String shortId, String ownerId, String blobUrl) {
		this( shortId, ownerId, blobUrl, System.currentTimeMillis(), 0);
	}
	
	public void setShortId(String shortId) {
		this.shortId = shortId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public void setBlobUrl(String blobUrl) {
		this.blobUrl = blobUrl;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public void setTotalLikes(int totalLikes) {
		this.totalLikes = totalLikes;
	}

	public String getShortId() {
		return shortId;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public String getBlobUrl() {
		return blobUrl;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public int getTotalLikes() {
		return totalLikes;
	}

	public Short copyWith( long totLikes ) {
		return new Short( shortId, ownerId, blobUrl, timestamp, (int)totLikes);
	}

	public void addLike() {
		totalLikes += 1;
	}

	public void removeLike() {
		totalLikes -= 1;
	}

	@Override
	public String toString() {
		return "Short [shortId=" + shortId + ", ownerId=" + ownerId + ", blobUrl=" + blobUrl + ", timestamp="
				+ timestamp + ", totalLikes=" + totalLikes + "]";
	}

}