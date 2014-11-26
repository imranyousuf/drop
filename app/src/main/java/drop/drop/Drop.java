package drop.drop;

/**
 * Created by kyledillon on 11/22/14.
 */
public class Drop {
    private String image;
    private Double lat;
    private Double lon;
    private String tags;
    private String key;
    private String text;

    public Drop() {}

    public Drop(String key, String image, Double lat, Double lon, String tags, String text) {
        this.key = key;
        this.image = image;
        this.lat = lat;
        this.lon = lon;
        this.tags = tags;
        this.text= text;
    }

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
}
