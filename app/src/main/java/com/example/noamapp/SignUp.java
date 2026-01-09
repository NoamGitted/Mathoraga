package com.example.noamapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class SignUp extends AppCompatActivity implements View.OnClickListener {



    private FirebaseAuth mAuth;
    Button btnSubmit;
    EditText etEmail;
    EditText etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initializing
        mAuth = FirebaseAuth.getInstance();

        btnSubmit = findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(this);


        etEmail = findViewById(R.id.username);
        etPassword = findViewById(R.id.password);

        etEmail.addTextChangedListener(new ResetBackgroundTextWatcher(etEmail));
        etPassword.addTextChangedListener(new ResetBackgroundTextWatcher(etPassword));
    }

    @Override
    public void onClick(View v) {
        //Setting the Strings
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Client-side validation
        if (email.isEmpty()) {

            Toast.makeText(SignUp.this, "Email cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty()) {

            Toast.makeText(SignUp.this, "Password cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SignUp.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                        Intent transferToMainMenu = new Intent(SignUp.this, com.example.noamapp.MainMenu.class);
                        startActivity(transferToMainMenu);
                        // Navigate to another activity

                    } else {
                        Exception exception = task.getException();
                        if (exception instanceof FirebaseAuthWeakPasswordException) {
                            etPassword.setBackgroundColor(Color.RED);
                            Toast.makeText(SignUp.this, "Password too weak (min 6 characters).", Toast.LENGTH_LONG).show();
                        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                            etEmail.setBackgroundColor(Color.RED);
                            Toast.makeText(SignUp.this, "Invalid email address format.", Toast.LENGTH_LONG).show();
                        } else if (exception instanceof FirebaseAuthUserCollisionException) {
                            etEmail.setBackgroundColor(Color.RED);
                            Toast.makeText(SignUp.this, "Account with this email already exists.", Toast.LENGTH_LONG).show();
                        } else if (exception != null) {
                            // Generic error handler
                            Toast.makeText(SignUp.this, "Authentication failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                            etEmail.setBackgroundColor(Color.TRANSPARENT);
                            etPassword.setBackgroundColor(Color.TRANSPARENT);
                        } else {
                            // This case should ideally not happen if task.isSuccessful() is false
                            Toast.makeText(SignUp.this, "Authentication failed for an unknown reason.", Toast.LENGTH_LONG).show();

                        }
                    }
                });
    }
    // Resets the colors when interacted
    private class ResetBackgroundTextWatcher implements TextWatcher {
        private final EditText editTextToWatch;

        public ResetBackgroundTextWatcher(EditText editText) {
            this.editTextToWatch = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Not used but must be implemented for TextWatcher */ }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            editTextToWatch.setBackgroundColor(Color.TRANSPARENT); // Reset the specific EditText's color
        }

        @Override
        public void afterTextChanged(Editable s) { /* Not used but must be implemented for TextWatcher */ }
    }


}
