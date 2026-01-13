package com.example.noamapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class LogIn extends AppCompatActivity implements View.OnClickListener {

    // Firebase and UI Variables
    private FirebaseAuth mAuth;
    private Button btnSubmit;
    private EditText etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_log_in);

        // Handle system bar padding (for Edge-to-Edge display)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Views
        btnSubmit = findViewById(R.id.btnLogInSubmit);
        etEmail = findViewById(R.id.etLIEmail);
        etPassword = findViewById(R.id.etLIPassword);

        // Set Click Listener
        btnSubmit.setOnClickListener(this);

        // Add TextWatchers to clear red background when user starts typing again
        etEmail.addTextChangedListener(new ResetBackgroundTextWatcher(etEmail));
        etPassword.addTextChangedListener(new ResetBackgroundTextWatcher(etPassword));
    }

    @Override
    public void onClick(View v) {
        // 1. Reset UI to default state before validating
        etEmail.setBackgroundColor(Color.TRANSPARENT);
        etPassword.setBackgroundColor(Color.TRANSPARENT);
        etEmail.setError(null);
        etPassword.setError(null);

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 2. Client-side validation: Check if fields are empty
        boolean hasError = false;

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required.");
            etEmail.setBackgroundColor(Color.RED);
            hasError = true;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required.");
            etPassword.setBackgroundColor(Color.RED);
            hasError = true;
        }

        // If client-side validation fails, stop here
        if (hasError) return;

        // 3. Attempt Firebase Login
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // Success!
                Toast.makeText(LogIn.this, "Login successful!", Toast.LENGTH_SHORT).show();
                goToMainMenu();
            } else {
                // Failure: Use try-catch to "sort" the specific error
                try {
                    // This "throws" the exception so we can catch specific types
                    throw task.getException();
                } catch (FirebaseAuthInvalidUserException e) {
                    // CASE: Email does not exist or is disabled
                    etEmail.setError("User account not found.");
                    etEmail.setBackgroundColor(Color.RED);
                    etEmail.requestFocus();
                } catch (FirebaseAuthInvalidCredentialsException e) {
                    // CASE: Wrong password or malformed email
                    etPassword.setError("Incorrect password or invalid email format.");
                    etPassword.setBackgroundColor(Color.RED);
                } catch (Exception e) {
                    // CASE: Network issues or other unexpected errors
                    Log.e("Firebase_Error", e.getMessage());
                    Toast.makeText(LogIn.this, "Authentication failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // Helper method to transition to the Main Menu
    private void goToMainMenu() {
        Intent intent = new Intent(LogIn.this, com.example.noamapp.MainMenu.class);
        startActivity(intent);
        finish();
    }

    /**
     * TextWatcher that listens for changes and resets the field's look
     * once the user starts correcting their mistake.
     */
    private static class ResetBackgroundTextWatcher implements TextWatcher {
        private final EditText editText;

        public ResetBackgroundTextWatcher(EditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Remove red background and error message as soon as user types a character
            editText.setBackgroundColor(Color.TRANSPARENT);
            editText.setError(null);
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }
}