package drop.drop;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.Base64;

/**
 * A class to store user information to populate our Friends List
 * Created by Short on 12/9/2014.
 */
public class UserInfo {

    private String number;
    private String name;
    private Bitmap picture;
    private boolean selected;
    private int imagePixels = 144;


    public UserInfo(String number, String name, String encodedString) {
        this.number = number;
        this.name = name;
        //picture = ImageHelper.StringToBitMap(encodedString);
        picture = formatPictureForListView(encodedString);
        selected = false;

    }

    public Bitmap formatPictureForListView(String encodedString) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inDither = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        try {
            byte[] encodeByte = Base64.decode(encodedString, Base64.DEFAULT);


            Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            bitmap = Bitmap.createScaledBitmap(bitmap, imagePixels, imagePixels, false);
            bitmap = getRoundedRectBitmap(bitmap);
            return bitmap;
        } catch (Exception e) {
            e.getMessage();
            return null;
        }
    }

    public Bitmap getRoundedRectBitmap(Bitmap bitmap) {
        Bitmap roundBitmap = null;
        try {
            roundBitmap = Bitmap.createBitmap(imagePixels, imagePixels, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(roundBitmap);

            int color = 0xff424242;
            Paint paint = new Paint();
            Rect rect = new Rect(0, 0, imagePixels, imagePixels);

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(color);
            canvas.drawCircle(72, 72 , 72, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bitmap, rect, rect, paint);

        } catch (NullPointerException e) {
        } catch (OutOfMemoryError o) {
        }
        return roundBitmap;
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

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Bitmap getPicture() {
        return picture;
    }

    public void setPicture(Bitmap picture) {
        this.picture = picture;
    }
}
