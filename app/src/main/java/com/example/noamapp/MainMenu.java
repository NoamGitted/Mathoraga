package com.example.noamapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

   private User cUser;
    ImageView bLogOut;
    TextView tVNumberOfWins;
    Button btnHost, btnJoin;
    EditText etLobbyId, etNumberOfRounds;

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
        etLobbyId = findViewById(R.id.etLobbyId);
        etNumberOfRounds = findViewById(R.id.etNumberOfRounds);
        bLogOut = findViewById(R.id.bLogOut);
        bLogOut.setOnClickListener(this);
    btnHost = findViewById(R.id.btnHost);
    btnHost.setOnClickListener(this);
    tVNumberOfWins = findViewById(R.id.tVNumberOfWins);
    btnJoin = findViewById(R.id.btnJoin);
    btnJoin.setOnClickListener(this);
    fetchUserData(mAuth.getUid());
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
        createLobby(cUser, mAuth.getUid());

        }
        if(id == R.id.btnJoin){
            joinLobby(cUser, mAuth.getUid());
        }

    }

    private void createLobby(User user, String uid) {
        String lobbyID = lobbyIDGenerator();
        String input = etNumberOfRounds.getText().toString().trim();
        int rounds;

        if (input.isEmpty()) {
            rounds = 5; // Set a default if they forgot
        } else {
            try {
                rounds = Integer.parseInt(input);
                if (rounds < 1) rounds = 1;
            } catch (NumberFormatException e) {
                rounds = 5; // Backup default if they typed something weird like "abc"
            }
        }

        // 1. Create the Main Lobby Document data
        Map<String, Object> lobbyData = new HashMap<>();
        lobbyData.put("hostID", uid);
        lobbyData.put("currentTheme", "General Math"); // Initial theme
        lobbyData.put("inRound", false); // Start in "Waiting/Leaderboard" mode
        lobbyData.put("lobbyID", lobbyID);
        lobbyData.put("roundsLeft", rounds);
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
    private void joinLobby(User user, String uid) {
        String inputLobbyID = etLobbyId.getText().toString().trim();

        if (inputLobbyID.isEmpty()) {
            Toast.makeText(this, "Please enter a Lobby ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Check if the Lobby Document exists
        dbz.collection("gameInstance").document(inputLobbyID).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 2. Check if the user is already in the players sub-collection
                        checkUserInLobby(inputLobbyID, user, uid);
                    } else {
                        Toast.makeText(this, "Lobby not found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Log.e("JOIN", "Error finding lobby", e));
    }

    private void checkUserInLobby(String lobbyID, User user, String uid) {
        dbz.collection("gameInstance").document(lobbyID)
                .collection("players").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        // User is already here! Just move to the Game Page (Reconnection logic)
                        moveToGame(lobbyID);
                    } else {
                        // 3. New Player: Add them to the lobby
                        addNewPlayerToLobby(lobbyID, user, uid);
                    }
                });
    }

    private void addNewPlayerToLobby(String lobbyID, User user, String uid) {
        Map<String, Object> playerData = new HashMap<>();
        playerData.put("username", user.username);
        playerData.put("currentPoints", 0);
        playerData.put("isHost", false); // Joiners are never hosts
        playerData.put("isReady", false);
        playerData.put("uID", uid);

        dbz.collection("gameInstance").document(lobbyID)
                .collection("players").document(uid)
                .set(playerData)
                .addOnSuccessListener(aVoid -> {
                    moveToGame(lobbyID);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to join", Toast.LENGTH_SHORT).show());
    }

    private void moveToGame(String lobbyID) {
        Intent intent = new Intent(MainMenu.this, Game_Page.class);
        intent.putExtra("LOBBY_ID", lobbyID);
        startActivity(intent);
    }

    private void fetchUserData(String uid) {
        dbz.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // THE COMMAND YOU ASKED FOR:
                        this.cUser = documentSnapshot.toObject(User.class);
                        tVNumberOfWins.setText("Number of wins: " + cUser.getNumberOfWins());
                        btnHost.setEnabled(true);
                        btnJoin.setEnabled(true);
                        Log.d("AUTH", "Welcome, " + cUser.username);
                    }
                });
    }
}
