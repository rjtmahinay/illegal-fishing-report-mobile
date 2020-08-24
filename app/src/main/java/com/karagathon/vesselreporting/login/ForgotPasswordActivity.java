package com.karagathon.vesselreporting.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.karagathon.vesselreporting.R;
import com.karagathon.vesselreporting.helper.FieldVerificationHelper;

import java.util.Objects;

public class ForgotPasswordActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private TextInputLayout emailText;
    private Button resetButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailText = findViewById(R.id.email_reset);
        resetButton = findViewById(R.id.reset_submit_button);
        progressBar = findViewById(R.id.reset_password_progressBar);
        progressBar.setVisibility(View.GONE);

        auth = FirebaseAuth.getInstance();

        resetButton.setOnClickListener(view -> {
            String email = emailText.getEditText().getText().toString();
            if (!isEmailValid(email)) return;

            progressBar.setVisibility(View.VISIBLE);
            auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    progressBar.setVisibility(View.GONE);

                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    intent.putExtra("RESET_PASSWORD_SUCCESS", true);
                    startActivity(intent);
                    finish();
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            });
        });
    }

    private boolean isEmailValid(String email) {
        boolean result = true;
        if (Objects.isNull(email)
                || email.isEmpty() || !FieldVerificationHelper.isEmailPatternValid(email)) {
            emailText.setError("Invalid email");
            result = false;
        } else {
            emailText.setError(null);
            emailText.setErrorEnabled(false);
        }
        return result;
    }

}