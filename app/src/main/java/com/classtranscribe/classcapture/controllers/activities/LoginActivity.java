package com.classtranscribe.classcapture.controllers.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.classtranscribe.classcapture.R;
import com.classtranscribe.classcapture.models.User;
import com.classtranscribe.classcapture.services.CustomCB;
import com.classtranscribe.classcapture.services.UserService;
import com.classtranscribe.classcapture.services.UserServiceProvider;

import java.util.regex.Pattern;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import retrofit.Response;
import retrofit.Retrofit;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getName();
    public static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
    );

    private static int MIN_PASSWORD_LENGTH = 5;

    Realm defaultRealm;

    EditText emailEditText;
    EditText passwordEditText;
    Button loginButton;
    TextView registerTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        this.setupDefaultRealm();

        // Set all View Variables
        this.emailEditText    = (EditText) this.findViewById(R.id.emailEditText);
        this.passwordEditText = (EditText) this.findViewById(R.id.passwordEditText);
        this.loginButton      = (Button) this.findViewById(R.id.loginButton);
        this.registerTextView = (TextView) this.findViewById(R.id.registerTextView);

        this.loginButton.setEnabled(false);
        this.setupValidators();

        // If already logged in, progress them into the main activity
        if (this.hasLoggedInUser()) {
            this.goToMainActivity();
        }
    }

    /**
     * Sets up validation logic on email & password edit texts to enable/disable the login button based on whether
     * the credential inputs are valid
     */
    private void setupValidators() {
        TextWatcher validationTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean validCredentials = LoginActivity.this.validCredentials();
                LoginActivity.this.loginButton.setEnabled(validCredentials);
            }
        };

        this.emailEditText.addTextChangedListener(validationTextWatcher);
        this.passwordEditText.addTextChangedListener(validationTextWatcher);
    }

    public boolean validCredentials() {
        String emailInput = this.emailEditText.getText().toString();
        String passwordInput = this.passwordEditText.getText().toString();

        String validEmailDomain = this.getString(R.string.valid_email_domain);

        boolean emailCheck = isValidEmail(emailInput) && emailInput.endsWith(validEmailDomain);
        boolean passwordCheck = passwordInput.length() >= MIN_PASSWORD_LENGTH;

        return emailCheck && passwordCheck;
    }

    public boolean isValidEmail(String email) {
        return EMAIL_ADDRESS_PATTERN.matcher(email).matches();
    }

    public void onLoginClicked(View v) {
        final String email = this.emailEditText.getText().toString().trim();
        final String password = this.passwordEditText.getText().toString().trim();
        final User credsUser = new User(email, password);

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle(getString(R.string.login_dialog_title));
        dialog.setMessage(getString(R.string.login_dialog_message));
        dialog.show();

        UserService userService = UserServiceProvider.getInstance(this);
        userService.login(User.getLoginCreds(credsUser)).enqueue(new CustomCB<User>(this, TAG) {
            @Override
            public void onResponse(Response<User> response, Retrofit retrofit) {
                User loggedInUser = response.body();
                LoginActivity.this.defaultRealm.beginTransaction();
                LoginActivity.this.defaultRealm.copyToRealmOrUpdate(loggedInUser);
                LoginActivity.this.defaultRealm.commitTransaction();
                dialog.dismiss();
                LoginActivity.this.goToMainActivity();
            }

            @Override
            public void onRequestFailure(Throwable t) {
                dialog.dismiss();
                Toast.makeText(LoginActivity.this, getString(R.string.login_error_message), Toast.LENGTH_LONG).show();
                t.printStackTrace();
            }
        });

    }

    public void onRegisterClicked(View v) {
        final String registerUrl = this.getString(R.string.api_base_url) + this.getString(R.string.register_endpoint);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(registerUrl));
        startActivity(browserIntent);
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        this.startActivity(intent);
        this.finish();
    }

    /**
     * When user is logged in, the User table in realm should contain exactly one entry
     * @return whether there is a logged in user
     */
    private boolean hasLoggedInUser() {
        return !this.defaultRealm.getTable(User.class).isEmpty();
    }

    /**
     * Sets Default realm config and sets this.defaultRealm
     */
    private void setupDefaultRealm() {
        RealmConfiguration.Builder configBuilder = new RealmConfiguration.Builder(this);
        RealmConfiguration defaultConfig = configBuilder.build();
        Realm.setDefaultConfiguration(defaultConfig);
        this.defaultRealm = Realm.getDefaultInstance();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.defaultRealm.close();
    }
}
