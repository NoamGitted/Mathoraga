package com.example.noamapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainMenu extends AppCompatActivity implements View.OnClickListener {
    private FirebaseAuth mAuth;
    ImageView bLogOut;
    Button btnToTheGame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            return insets;
        });
        mAuth = FirebaseAuth.getInstance();

        bLogOut = findViewById(R.id.bLogOut);
        bLogOut.setOnClickListener(this);
btnToTheGame = findViewById(R.id.btnToTheGame);
btnToTheGame.setOnClickListener(this);
        }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.bLogOut){
            mAuth.signOut();
            Intent backToSplash = new Intent(MainMenu.this, com.example.noamapp.MainActivity.class);
            startActivity(backToSplash);
            finish();
        }
        if (id == R.id.btnToTheGame){
            Intent toTheGame = new Intent(MainMenu.this, com.example.noamapp.Game_Page.class);
            startActivity(toTheGame);
            finish();
        }

    }
}
