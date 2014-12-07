package drop.drop;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class MakeProfile extends Activity {

    Firebase firebase;
    String email = "";
    String password = "";
    EditText username;
    EditText number;
    String photoString = "";
    TextView selfieText;
    ImageButton profile_picture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        email = intent.getStringExtra("EMAIL");
        password = intent.getStringExtra("PASSWORD");

        firebase = new Firebase("https://dropdatabase.firebaseio.com");

        ActionBar actionBar = getActionBar();
        actionBar.hide();

        setContentView(R.layout.activity_make_profile);

        username = (EditText) findViewById(R.id.username);
        number = (EditText) findViewById(R.id.number);
        profile_picture = (ImageButton) findViewById(R.id.profile_picture);
        selfieText = (TextView) findViewById(R.id.selfie_text);
    }

    static final int REQUEST_TAKE_PHOTO = 1;
    public void profilePicturePressed(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile("drop_profile_photo");
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    String mCurrentPhotoPath;
    private File createImageFile(String name) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = name + "_" + timeStamp;
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);;
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, options);
            selfieText.setVisibility(View.INVISIBLE);
            profile_picture.setImageBitmap(bitmap);
            photoString = ImageHelper.BitMapToString(bitmap);
        }
    }

    public void getStartedPressed(View view) {
        firebase.authWithPassword(email, password, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                System.out.println("User ID: " + authData.getUid() + ", Provider: " + authData.getProvider());

                // Save UID for use throughout the app
                SharedPreferences.Editor editor = getSharedPreferences("drop", MODE_PRIVATE).edit();
                editor.putString("uid", authData.getUid());
                editor.commit();

                /* How to retrieve preferences
                SharedPreferences prefs = getSharedPreferences("drop", MODE_PRIVATE);
                String UID = prefs.getString("uid", null);
                if (restoredText != null) {
                    // Valid
                }
                */

                // TODO: Make sure data is valid before storing it into the database.
                Map<String, String> map = new HashMap<String, String>();
                map.put("username", username.getText().toString());
                map.put("number", number.getText().toString());
                map.put("profile_picture", ImageHelper.BitMapToString(((BitmapDrawable) profile_picture.getDrawable()).getBitmap()));
                firebase.child("users").child(authData.getUid()).setValue(map); // Store with ID as key

                Toast.makeText(getApplicationContext(), "You're good to go!", Toast.LENGTH_LONG).show();

                finish();
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                Toast.makeText(getApplicationContext(), "Something went wrong, please try again :/", Toast.LENGTH_LONG).show();
            }
        });
    }
}
