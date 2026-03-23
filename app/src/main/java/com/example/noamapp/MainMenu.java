package com.example.noamapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MainMenu extends AppCompatActivity implements View.OnClickListener {
    private FirebaseAuth mAuth;
    private FirebaseFirestore dbz;
    private static final String TAG = "Noam";
    ImageView bLogOut;
    Button btnHost, btnJoin;

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
        dbz = FirebaseFirestore.getInstance();
        bLogOut = findViewById(R.id.bLogOut);
        bLogOut.setOnClickListener(this);
    btnHost = findViewById(R.id.btnHost);
    btnHost.setOnClickListener(this);

    btnJoin = findViewById(R.id.btnJoin);
    btnJoin.setOnClickListener(this);
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
        if (id == R.id.btnHost){
        String lobby = lobbyIDGenerator();
        String uid = mAuth.getUid();

            dbz.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    //Firestore maps the DB fields directly to User class
                    User currentUser = doc.toObject(User.class);
                    Log.d(TAG,currentUser.getUsername());

                    if (currentUser != null) {
                        createLobby(currentUser, uid);
                    }
                }
            });


        }
        if(id == R.id.btnJoin){
            int bye = 8/0;
        }

    }

    private void createLobby(User user, String uid) {
        String lobbyID = lobbyIDGenerator();

        // 1. Create the Main Lobby Document data
        Map<String, Object> lobbyData = new HashMap<>();
        lobbyData.put("hostID", uid);
        lobbyData.put("currentTheme", "General Math"); // Initial theme
        lobbyData.put("inRound", false); // Start in "Waiting/Leaderboard" mode
        lobbyData.put("lobbyID", lobbyID);

        // 2. Create the Host Player data
        Map<String, Object> playerInLobby = new HashMap<>();
        playerInLobby.put("username", user.username);
        playerInLobby.put("currentPoints", 0);
        playerInLobby.put("isHost", true);
        playerInLobby.put("isReady", false);
        playerInLobby.put("uID", uid);

        // 3. Write the Lobby Document first
        dbz.collection("gameInstance")
                .document(lobbyID)
                .set(lobbyData)
                .addOnSuccessListener(aVoid -> {
                    // 4. Once Lobby exists, add the Host to the players sub-collection
                    dbz.collection("gameInstance")
                            .document(lobbyID)
                            .collection("players")
                            .document(uid)
                            .set(playerInLobby)
                            .addOnSuccessListener(aVoid2 -> {
                                // 5. Success! Move to Game_Page
                                Intent intent = new Intent(MainMenu.this, Game_Page.class);
                                intent.putExtra("LOBBY_ID", lobbyID);
                                startActivity(intent);
                            });
                })
                .addOnFailureListener(e -> Log.e("LOBBY", "Failed to create lobby", e));
    }

    public String lobbyIDGenerator(){
        String lobby = "";
        Random rng = new Random();
        for (int i=0; i<6; i++) {
            lobby += String.valueOf(rng.nextInt(10));
        }
        return lobby;
    }

}
