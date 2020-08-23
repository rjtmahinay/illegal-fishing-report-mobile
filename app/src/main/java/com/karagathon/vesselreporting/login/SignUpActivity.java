package com.karagathon.vesselreporting.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.karagathon.vesselreporting.R;
import com.karagathon.vesselreporting.helper.FieldVerificationHelper;

import java.util.Objects;


public class SignUpActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView emailErrorTextView;
    private TextView passwordErrorTextView;
    private TextView nameErrorTextView;
    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        final EditText signUpName = findViewById(R.id.sign_up_name);
        final EditText signUpEmail = findViewById(R.id.sign_up_email);
        final EditText signUpPassword = findViewById(R.id.sign_up_password);
        final Button signUpSubmitButton = findViewById(R.id.sign_up_submit_button);
        final Button goToLoginButton = findViewById(R.id.go_to_login_button);
        emailErrorTextView = findViewById(R.id.sign_up_email_error);
        passwordErrorTextView = findViewById(R.id.sign_up_password_error);
        nameErrorTextView = findViewById(R.id.sign_up_name_error);

        progressBar = findViewById(R.id.signup_progressBar);
        progressBar.setVisibility(View.GONE);

        FirebaseAuth auth = FirebaseAuth.getInstance();

        signUpSubmitButton.setOnClickListener(view -> {
            String email = signUpEmail.getText().toString();
            String password = signUpPassword.getText().toString();
            createCredentials(auth, signUpName, email, password);
        });

        goToLoginButton.setOnClickListener(view -> {
            goToLogin();
        });
    }

    private void createCredentials(FirebaseAuth auth, EditText signUpName, String email, String password) {
        name = signUpName.getText().toString();
        if (!isSuccessValidation(name, email, password)) return;
        progressBar.setVisibility(View.VISIBLE);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("User");
                        String id = dbRef.push().getKey();

                        dbRef.child(id).child("name").setValue(name);
                        progressBar.setVisibility(View.GONE);
                        goToLogin();
                    } else {
                        progressBar.setVisibility(View.GONE);
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
            nameErrorTextView.setHint("Name must not be blank");
            result = false;
        } else {
            nameErrorTextView.setHint(null);
        }

        if (Objects.isNull(email)
                || email.isEmpty() || !FieldVerificationHelper.isEmailPatternValid(email)) {
            emailErrorTextView.setHint("Invalid email");
            result = false;
        } else {
            emailErrorTextView.setHint(null);
        }

        if (Objects.isNull(password) || password.isEmpty()) {
            passwordErrorTextView.setHint("Password must not be blank");
            result = false;
        } else {
            passwordErrorTextView.setHint(null);
        }
        return result;
    }
}