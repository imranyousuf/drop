package drop.drop;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;


public class Login extends Activity {

    Firebase firebase;
    EditText email_login;
    EditText password_login;
    Button login_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.hide();

        firebase = new Firebase("https://dropdatabase.firebaseio.com");

        setContentView(R.layout.activity_login);

        email_login = (EditText) findViewById(R.id.email_login);
        password_login = (EditText) findViewById(R.id.password_login);
        login_button = (Button) findViewById(R.id.login_button);
    }

    public void loginPressed(View view) {
        login_button.setEnabled(false);
        firebase.authWithPassword(email_login.getText().toString(), password_login.getText().toString(),
                new Firebase.AuthResultHandler() {
                    @Override
                    public void onAuthenticated(AuthData authData) {
                        login_button.setEnabled(true);
                        // Save UID for use throughout the app
                        SharedPreferences.Editor editor = getSharedPreferences("drop", MODE_PRIVATE).edit();
                        editor.putString("uid", authData.getUid());
                        editor.commit();

                        finish();

                        Toast.makeText(getApplicationContext(), "You're logged in!", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onAuthenticationError(FirebaseError error) {
                        login_button.setEnabled(true);
                        Toast.makeText(getApplicationContext(), "Hmm.. not working. Are you registered?", Toast.LENGTH_LONG).show();

                        // TODO: Do better error handle
                        switch (error.getCode()) {
                            case FirebaseError.USER_DOES_NOT_EXIST:
                                // handle a non existing user
                                break;
                            case FirebaseError.INVALID_PASSWORD:
                                // handle an invalid password
                                break;
                            default:
                                // handle other errors
                                break;
                        }
                    }
                });
    }

    public void notRegisteredPressed(View view) {
        Intent intent = new Intent(this, SignUp.class);
        finish();
        startActivity(intent);
    }
}
