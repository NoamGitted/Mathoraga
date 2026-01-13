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
import android.widget.Toast; // Still need Toast for general/non-field-specific errors

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;

public class LogIn extends AppCompatActivity implements View.OnClickListener {
    private FirebaseAuth mAuth;
    Button btnSubmit;
    EditText etEmail;
    EditText etPassword;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_log_in);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        btnSubmit = findViewById(R.id.btnLogInSubmit);
        btnSubmit.setOnClickListener(this);

        etEmail = findViewById(R.id.etLIEmail);
        etPassword = findViewById(R.id.etLIPassword);

        // Corrected instantiation to LogIn.ResetBackgroundTextWatcher
        etEmail.addTextChangedListener(new LogIn.ResetBackgroundTextWatcher(etEmail));
        etPassword.addTextChangedListener(new LogIn.ResetBackgroundTextWatcher(etPassword));
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent main = new Intent(LogIn.this, com.example.noamapp.MainMenu.class);
            startActivity(main);
            finish(); // Finish LogIn activity if user is already logged in
        }
    }

    @Override
    public void onClick(View v) {
        // Reset backgrounds and errors from previous attempts
        etEmail.setBackgroundColor(Color.TRANSPARENT);
        etPassword.setBackgroundColor(Color.TRANSPARENT);
        etEmail.setError(null);
        etPassword.setError(null);

        String email = etEmail.getText().toString().trim(); // Use trim() to remove leading/trailing spaces
        String password = etPassword.getText().toString().trim();

        // Client-side validation: Check for empty fields first
        // Note: The 'if (email.isEmpty() && password.isEmpty())' block was removed
        // because the individual checks handle this more granularly.
        boolean hasError = false; // Flag to track if we've found an error

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required.");
            etEmail.setBackgroundColor(Color.RED); // Turn email field red
            hasError = true;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required.");
            etPassword.setBackgroundColor(Color.RED); // Turn password field red
            hasError = true;
        }

        // If client-side validation found errors, stop here
        if (hasError) {
            return;
        }

        // Attempt to sign in with Firebase
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // Login succeeded
                Toast.makeText(LogIn.this, "Account Logged In successfully!", Toast.LENGTH_SHORT).show();
                Intent transferToMainMenu = new Intent(LogIn.this, com.example.noamapp.MainMenu.class);
                startActivity(transferToMainMenu);
                finish(); // Close LogIn activity after successful login
            } else {
                // Login failed
                Exception exception = task.getException();

                if (exception != null) {
                    Log.e("Firebase Login", "Login failed: " + exception.getMessage(), exception);

                    String toastMessage = "Authentication failed."; // Fallback for errors not tied to a specific field

                    if (exception instanceof FirebaseAuthException) {
                        FirebaseAuthException firebaseAuthException = (FirebaseAuthException) exception;
                        String errorCode = firebaseAuthException.getErrorCode();

                        switch (errorCode) {
                            case "ERROR_INVALID_EMAIL":
                                etEmail.setError("The email address is badly formatted.");
                                etEmail.setBackgroundColor(Color.RED);
                                toastMessage = "Invalid email format."; // Use a more general message for Toast if needed
                                break;
                            case "ERROR_WRONG_PASSWORD":
                                etPassword.setError("Invalid password. Please try again.");
                                etPassword.setBackgroundColor(Color.RED);
                                toastMessage = "Incorrect password.";
                                break;
                            case "ERROR_USER_NOT_FOUND":
                                etEmail.setError("No user found with this email.");
                                etEmail.setBackgroundColor(Color.RED);
                                toastMessage = "User not found.";
                                break;
                            case "ERROR_USER_DISABLED":
                                etEmail.setError("This account has been disabled.");
                                etEmail.setBackgroundColor(Color.RED);
                                toastMessage = "Your account is disabled.";
                                break;
                            case "ERROR_TOO_MANY_REQUESTS":
                                // This error is not specific to one field, so a Toast or Dialog is better
                                toastMessage = "Too many failed login attempts. Please try again later.";
                                Toast.makeText(LogIn.this, toastMessage, Toast.LENGTH_LONG).show();
                                return; // Stop here, no field-specific error needed
                            default:
                                toastMessage = "Authentication failed: " + firebaseAuthException.getMessage();
                                // For unknown FirebaseAuth errors, display a Toast
                                Toast.makeText(LogIn.this, toastMessage, Toast.LENGTH_LONG).show();
                                return; // Stop here
                        }
                        // If we reached here, a field-specific error was set above,
                        // no need for an extra Toast in these cases.
                    } else {
                        // Handle other types of exceptions (e.g., network issues)
                        toastMessage = "An unexpected error occurred: " + exception.getMessage();
                        Toast.makeText(LogIn.this, toastMessage, Toast.LENGTH_LONG).show();
                        return; // Stop here
                    }

                } else {
                    // Unknown error, no exception object
                    Toast.makeText(LogIn.this, "Authentication failed with an unknown error.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Made static so it doesn't hold an implicit reference to the outer class instance
    // and can be instantiated without an instance of LogIn (e.g., new LogIn.ResetBackgroundTextWatcher)
    private static class ResetBackgroundTextWatcher implements TextWatcher {
        private final EditText editTextToWatch;

        public ResetBackgroundTextWatcher(EditText editText) {
            this.editTextToWatch = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Not used */ }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // When text changes, reset the background color and clear error
            editTextToWatch.setBackgroundColor(Color.TRANSPARENT);
            editTextToWatch.setError(null);
        }

        @Override
        public void afterTextChanged(Editable s) { /* Not used */ }
    }
}
