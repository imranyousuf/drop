package drop.drop;

/**
 * Created by Short on 12/9/2014.
 */
public class UserInfo {

    private String number;
    private String name;
    private int picture;
    private boolean selected;

    public UserInfo(String number, String name, int picture) {
        this.number = number;
        this.name = name;
        this.picture = picture;
        selected = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public int getPicture() {
        return picture;
    }

    public void setPicture(int picture) {
        this.picture = picture;
    }


    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
