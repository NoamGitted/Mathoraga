package com.example.noamapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class SignUp extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "Noam";

    private FirebaseAuth mAuth;
    Button btnSubmit;

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

        mAuth = FirebaseAuth.getInstance();

        btnSubmit = findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(this);


    }


    @Override
    public void onClick(View v) {
        EditText t1 = findViewById(R.id.username);
        String gmail = t1.getText().toString();
        EditText t2 = findViewById(R.id.password);
        String password = t2.getText().toString();
        mAuth.createUserWithEmailAndPassword(gmail, password).addOnCompleteListener(this, task -> {
        if (task.isSuccessful()){


        }
        else {

        }

            });


    }
}
