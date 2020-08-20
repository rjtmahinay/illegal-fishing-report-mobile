package com.karagathon.vesselreporting.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
    //    private LoginViewModel loginViewModel;
    private GoogleSignInClient googleSignClient;
    private FirebaseAuth auth;
    private CallbackManager callbackManager;
    private String FACEBOOK_TAG = "Facebook Authentication";
    private AccessTokenTracker accessTokenTracker;
    private ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        auth = FirebaseAuth.getInstance();

        final Button googleSignInButton = findViewById(R.id.google_login);
        callbackManager = CallbackManager.Factory.create();
        final LoginButton facebookSignInButton = findViewById(R.id.facebook_login);
        facebookSignInButton.setPermissions("email", "public_profile");

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);


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
//        final Button loginButton = findViewById(R.id.login);
//        final ProgressBar loadingProgressBar = findViewById(R.id.loading);


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

    // to be deleted
//    private void signInUsingEmail(Button emailButton) {
//        emailButton.setOnClickListener(view -> {
//            Intent emailIntent = new Intent(getApplicationContext(), SignUpActivity.class);
//            startActivity(emailIntent);
//        });
//    }

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
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("Firebase Sign In", "signInWithCredential:success");
                        FirebaseUser user = auth.getCurrentUser();

                        Log.i("CURRENT USER", user.getDisplayName());

//                        updateUI(user);
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("Firebase Sign In", "signInWithCredential:failure", task.getException());
//                        Snackbar.make(mBinding.mainLayout, "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
//                        updateUI(null);
                    }
                });
    }

}