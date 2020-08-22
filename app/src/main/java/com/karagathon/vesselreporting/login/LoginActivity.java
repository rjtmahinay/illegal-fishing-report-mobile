package com.karagathon.vesselreporting.login;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.karagathon.vesselreporting.R;
import com.karagathon.vesselreporting.constant.AuthProviders;
import com.karagathon.vesselreporting.report.ReportActivity;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private static final int GOOGLE_SIGN_IN = 10;
    private GoogleSignInClient googleSignClient;
    private FirebaseAuth auth;
    private CallbackManager callbackManager;
    private String FACEBOOK_TAG = "Facebook Authentication";
    private AccessTokenTracker accessTokenTracker;
    private ProgressBar progressBar;
    private EditText emailText;
    private EditText passwordText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        auth = FirebaseAuth.getInstance();

        final Button googleSignInButton = findViewById(R.id.google_login);
        callbackManager = CallbackManager.Factory.create();
        final LoginButton facebookSignInButton = findViewById(R.id.facebook_login);
        facebookSignInButton.setPermissions("email", "public_profile");
        final Button goButton = findViewById(R.id.go_button);
        final Button newUserButton = findViewById(R.id.new_user_button);

        emailText = findViewById(R.id.login_email);
        passwordText = findViewById(R.id.login_password);

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        signInUsingEmailPassword(goButton);
        signInUsingGoogle(googleSignInButton);
        signInUsingFacebook(facebookSignInButton);

        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
                if (Objects.isNull(currentAccessToken)) {
                    auth.signOut();
                }
            }
        };

        newUserButton.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), SignUpActivity.class);
            startActivity(intent);
        });
    }

    private void signInUsingEmailPassword(Button goButton) {
        goButton.setOnClickListener(view -> {
            String email = emailText.getText().toString();
            String password = passwordText.getText().toString();
            progressBar.setVisibility(View.VISIBLE);
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    Log.i("LoginActivity", "Email and Pass Success");
                    progressBar.setVisibility(View.GONE);
                    goToReport();
                } else {
                    Log.i("LoginActivity", "Email and Pass Failed");
                    progressBar.setVisibility(View.GONE);
                    AlertDialog alertDialog = new AlertDialog.Builder(LoginActivity.this).create();
                    alertDialog.setTitle("Credentials Incorrect");
                    alertDialog.setMessage("Email or password is incorrect");
                    alertDialog.show();

                    emailText.setText(null);
                    passwordText.setText(null);
                }
            });
        });
    }

    private void signInUsingGoogle(Button googleSignInButton) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignClient = GoogleSignIn.getClient(this, gso);

        googleSignInButton.setOnClickListener(view -> {
            signInToGoogle();
        });

    }

    private void signInUsingFacebook(LoginButton facebookSignInButton) {
        facebookSignInButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.i(FACEBOOK_TAG, "Sucesss " + loginResult);
                fireBaseAuth(loginResult.getAccessToken().getToken(), AuthProviders.FACEBOOK);
                progressBar.setVisibility(View.VISIBLE);
                goToReport();

            }
            @Override
            public void onCancel() {
                Log.i(FACEBOOK_TAG, "Cancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.e(FACEBOOK_TAG, "Error" + error.getMessage());
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("On Start", "On Start Application");
        FirebaseUser user = auth.getCurrentUser();
        Log.i("On Start User", String.valueOf(user));
        if (Objects.nonNull(user)) {
            goToReport();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveTaskToBack(true);
    }

    private void signInToGoogle() {
        Intent signInIntent = googleSignClient.getSignInIntent();
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN);

    }

    private void goToReport() {
        Intent reportIntent = new Intent(getApplicationContext(), ReportActivity.class);
        startActivity(reportIntent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == GOOGLE_SIGN_IN && resultCode == RESULT_OK) {
            Log.i("Google Sign In", "Result Ok");
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("Log Activity Result", "firebaseAuthWithGoogle:" + account.getId());
                fireBaseAuth(account.getIdToken(), AuthProviders.GOOGLE);
                progressBar.setVisibility(View.VISIBLE);
                Intent reportIntent = new Intent(getApplicationContext(), ReportActivity.class);
                startActivity(reportIntent);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.e("Login Activity Result Exception", e.getLocalizedMessage());
                // ...
            }
        }

        callbackManager.onActivityResult(requestCode, resultCode, data);
    }


    private void fireBaseAuth(String idToken, AuthProviders authProvider) {
        AuthCredential credential;
        switch (authProvider) {
            case GOOGLE:
                credential = GoogleAuthProvider.getCredential(idToken, null);
                break;
            case FACEBOOK:
                credential = FacebookAuthProvider.getCredential(idToken);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + authProvider);
        }

        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {
                        Log.d("Firebase Sign In", "signInWithCredential:success");
                        FirebaseUser user = auth.getCurrentUser();

                        Log.i("CURRENT USER", user.getDisplayName());
                    } else {
                        Log.w("Firebase Sign In", "signInWithCredential:failure", task.getException());
                    }
                });
    }

}