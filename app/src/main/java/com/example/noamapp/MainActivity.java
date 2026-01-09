package com.example.noamapp;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "Noam";
    private FirebaseAuth mAuth;
    Button login, signup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;

        });

        mAuth = FirebaseAuth.getInstance();

        login = findViewById(R.id.supabuttonlogin);
        signup = findViewById(R.id.supabuttonsignup);

        login.setOnClickListener(this);
        signup.setOnClickListener(this);
    }
    @Override
    //is someone there?
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {

            Intent main = new Intent(MainActivity.this, com.example.noamapp.MainMenu.class);
            startActivity(main);
            finish();
        }
    }

    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.supabuttonlogin) {
            Intent logIn = new Intent(MainActivity.this, com.example.noamapp.LogIn.class);
            startActivity(logIn);
        }
        if (id == R.id.supabuttonsignup) {
            Intent signup = new Intent(MainActivity.this, com.example.noamapp.SignUp.class);
            startActivity(signup);
        }
    }
}

