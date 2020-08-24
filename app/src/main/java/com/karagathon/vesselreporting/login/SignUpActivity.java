package com.karagathon.vesselreporting.login;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.karagathon.vesselreporting.R;
import com.karagathon.vesselreporting.helper.FieldVerificationHelper;

import java.util.Objects;


public class SignUpActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private String name;
    private String email;
    private String password;
    private TextInputLayout signUpName;
    private TextInputLayout signUpEmail;
    private TextInputLayout signUpPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        signUpName = findViewById(R.id.sign_up_name);
        signUpEmail = findViewById(R.id.sign_up_email);
        signUpPassword = findViewById(R.id.sign_up_password);
        final Button signUpSubmitButton = findViewById(R.id.sign_up_submit_button);
        final Button goToLoginButton = findViewById(R.id.go_to_login_button);

        progressBar = findViewById(R.id.signup_progressBar);
        progressBar.setVisibility(View.GONE);

        FirebaseAuth auth = FirebaseAuth.getInstance();

        signUpSubmitButton.setOnClickListener(view -> {
            createCredentials(auth);
        });

        goToLoginButton.setOnClickListener(view -> {
            goToLogin();
        });
    }

    private void createCredentials(FirebaseAuth auth) {
        name = signUpName.getEditText().getText().toString();
        email = signUpEmail.getEditText().getText().toString();
        password = signUpPassword.getEditText().getText().toString();

        if (!isSuccessValidation(name, email, password)) return;

        progressBar.setVisibility(View.VISIBLE);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("User");
                        String id = dbRef.push().getKey();

                        dbRef.child(id).child("name").setValue(name);
                        dbRef.child(id).child("email").setValue(email);
                        progressBar.setVisibility(View.GONE);

                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        intent.putExtra("NEW_USER", true);
                        startActivity(intent);
                        finish();

                    } else {
                        progressBar.setVisibility(View.GONE);
                        AlertDialog alertDialog
                                = new AlertDialog.Builder(SignUpActivity.this).create();
                        alertDialog.setTitle("Error");
                        alertDialog.setMessage("Please try again later!");
                        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", ((dialogInterface, i) -> {
                            //
                        }));
                        alertDialog.show();
                    }
                });
    }

    private void goToLogin() {
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private boolean isSuccessValidation(String name, String email, String password) {
        boolean result = true;

        if (Objects.isNull(name) || name.isEmpty()) {
            signUpName.setError("Name must not be blank");
            result = false;
        } else {
            signUpName.setError(null);
            signUpName.setErrorEnabled(false);
        }

        if (Objects.isNull(email)
                || email.isEmpty() || !FieldVerificationHelper.isEmailPatternValid(email)) {
            signUpEmail.setError("Invalid email");
            result = false;
        } else {
            signUpEmail.setError(null);
            signUpEmail.setErrorEnabled(false);
        }

        if (Objects.isNull(password) || password.isEmpty()) {
            signUpPassword.setError("Password must not be blank");
            result = false;
        } else {
            signUpPassword.setError(null);
            signUpPassword.setErrorEnabled(false);
        }
        return result;
    }
}