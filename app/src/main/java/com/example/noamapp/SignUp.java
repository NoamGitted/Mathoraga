package com.example.noamapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUp extends AppCompatActivity implements View.OnClickListener {

    private FirebaseAuth mAuth;
    private FirebaseFirestore dbz;
    private Button btnSubmit;
    private EditText etEmail, etPassword, etUserName;

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

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
dbz = FirebaseFirestore.getInstance();
        // Initialize UI Elements
        btnSubmit = findViewById(R.id.btnSubmit);
        etEmail = findViewById(R.id.gmail);
        etPassword = findViewById(R.id.password);
        etUserName = findViewById(R.id.username);
        btnSubmit.setOnClickListener(this);

        // Attach listeners to reset the red background when user starts typing
        etEmail.addTextChangedListener(new ResetBackgroundTextWatcher(etEmail));
        etPassword.addTextChangedListener(new ResetBackgroundTextWatcher(etPassword));
        etUserName.addTextChangedListener(new ResetBackgroundTextWatcher(etUserName));
    }

    @Override
    public void onClick(View v) {
        // 1. Reset UI: Clear previous errors and red backgrounds
        etEmail.setBackgroundColor(Color.TRANSPARENT);
        etPassword.setBackgroundColor(Color.TRANSPARENT);
        etEmail.setError(null);
        etPassword.setError(null);

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String username = etUserName.getText().toString().trim();
        // 2. Client-side validation: Check for empty fields using TextUtils
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
        if(TextUtils.isEmpty(username)){
            etUserName.setError("Username is required.");
            etUserName.setBackgroundColor(Color.RED);
            hasError = true;
        }
        if (hasError) return;

        // 3. Firebase Registration
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Success!
                        Toast.makeText(SignUp.this, "Account created successfully!", Toast.LENGTH_SHORT).show();

                        Map<String, Object> user = new HashMap<>();
                        user.put("username", username);
                        user.put("numberOfWins", 0);

                        dbz.collection("users").document(mAuth.getUid()).set(user).addOnSuccessListener(workpls -> {
                            Intent transferToMainMenu = new Intent(SignUp.this, com.example.noamapp.MainMenu.class);
                            startActivity(transferToMainMenu);
                            finish();
                        }).addOnFailureListener(didntwork ->{
                            mAuth.getCurrentUser().delete();
                                }

                                );


                    } else {
                        // Failure: Identify the specific registration error
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthWeakPasswordException e) {
                            // CASE: Password is less than 6 characters
                            etPassword.setError("Password too weak (min 6 characters).");
                            etPassword.setBackgroundColor(Color.RED);
                            etPassword.requestFocus();
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            // CASE: Email is not formatted correctly (e.g., missing @)
                            etEmail.setError("Invalid email format.");
                            etEmail.setBackgroundColor(Color.RED);
                            etEmail.requestFocus();
                        } catch (FirebaseAuthUserCollisionException e) {
                            // CASE: This email is already registered to someone else
                            etEmail.setError("This email is already registered.");
                            etEmail.setBackgroundColor(Color.RED);
                            etEmail.requestFocus();
                        } catch (Exception e) {
                            // CASE: Other errors (Network, etc.)
                            Toast.makeText(SignUp.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Custom TextWatcher that resets the EditText UI state when the user types.
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
            // When user types, clear error message and red background
            editText.setBackgroundColor(Color.TRANSPARENT);
            editText.setError(null);
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }
}