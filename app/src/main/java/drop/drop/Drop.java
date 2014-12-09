package drop.drop;

/**
 * Created by kyledillon on 11/22/14.
 */
public class Drop {
    private String image = "";
    private Double lat = 0.0;
    private Double lon = 0.0;
    private String tags = "";
    private String key = "";
    private String text = "";
    private boolean postIsPublic = false;
    private String dropperUID = "";
    private String imageKey = "";
    private long epoch = 0;

    public Drop() {}

    public Drop(String key, String image, Double lat, Double lon, String tags, String text, boolean postIsPublic, String imageKey, long epoch) {
        this.key = key;
        this.image = image;
        this.lat = lat;
        this.lon = lon;
        this.tags = tags;
        this.text= text;
        this.postIsPublic = postIsPublic;
        this.imageKey = imageKey;
        this.epoch = epoch;

    }

    public void setPublic(boolean postIsPublic) {this.postIsPublic = postIsPublic;}
    public void setImage(String image) {this.image = image;}
    public void setLat(Double lat) {
        this.lat = lat;
    }
    public void setLon(Double lon) { this.lon = lon;}
    public void setTags(String tags) { this.tags = tags;}
    public void setText(String text) {
        this.text = text;
    }
    public void setKey(String key) { this.key = key;}
    public void setDropperUID(String dropperUID) { this.dropperUID = dropperUID; }
    public void setImageKey(String imageKey) { this.imageKey = imageKey;}
    public void setEpoch(long epoch) { this.epoch = epoch;}

    public boolean getPostIsPublic() { return postIsPublic; }
    public String getImage() {
        return image;
    }
    public Double getLat() {
        return lat;
    }
    public Double getLon() {
        return lon;
    }
    public String getTags() {
        return tags;
    }
    public String getText() {
        return text;
    }
    public String getKey() { return key; }
    public String getDropperUID() { return dropperUID;}
    public String getImageKey() { return imageKey;}
    public long getEpoch() { return epoch;}
}
