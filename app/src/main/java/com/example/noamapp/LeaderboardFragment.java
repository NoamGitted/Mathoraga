package com.example.noamapp;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.noamapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class LeaderboardFragment extends Fragment {

    private FirebaseFirestore dbz = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private String lobbyID;
    private boolean isHost;
    private List<Player> playerList = new ArrayList<>();

    private TextView txtlobbyID;
    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private Button btnNextRound;

    public LeaderboardFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Grab the Lobby ID passed from Game_Page
        if (getArguments() != null) {
            lobbyID = getArguments().getString("LOBBY_ID");
        }
        String cUId = mAuth.getUid();
        if (cUId != null && lobbyID != null) {
            dbz.collection("gameInstance").document(lobbyID)
                    .collection("players").document(cUId).get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            // Set the global variable
                            this.isHost = snapshot.getBoolean("isHost");
                            checkEveryoneReady(playerList);
                        }
                    });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 1. Inflate the layout
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        // Link the UI attributes to the XML IDs
        recyclerView = view.findViewById(R.id.recycler_leaderboard);
        btnNextRound = view.findViewById(R.id.btn_next_round);
        txtlobbyID = view.findViewById(R.id.txtlobbyID);
        txtlobbyID.setText(lobbyID);
        // Set up the RecyclerView's "shape" (Vertical List)
        adapter = new LeaderboardAdapter(playerList);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        startLeaderboardListener();
        setupStateListener();

        btnNextRound.setOnClickListener(v -> {
            // 1. Tell Firestore to move the game to the next state
            // We will set 'gameState' to 'QUESTION' and 'isReady' for all players to 'false'
            resetPlayersForNextRound();
        });

        return view;
    }

    private void startLeaderboardListener() {
        // 1. Point to the players collection in this specific lobby
        dbz.collection("gameInstance").document(lobbyID)
                .collection("players")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("LOBBY_ERROR", "Listen failed.", error);
                        return;
                    }

                    // Pass the 'value' (QuerySnapshot) to your new method
                    if (value != null) {
                        updateLeaderboard(value);
                    }
                });
    }

    private void checkEveryoneReady(List<Player> players) {
        // 1. Start by assuming everyone IS ready
        boolean allReady = true;

        // 2. Loop through every player in the list
        for (Player p : players) {
            // If even ONE person has isReady == false, the whole group isn't ready
            if (!p.isReady()) {
                allReady = false;
                break; // Stop looking, we found someone not ready
            }
        }
Log.d("Noam","allReady= " + allReady);
        // 3. Update the button visibility
        // Only show the "Next Round" button if ALL are ready AND this user is the Host
        if (allReady && isHost) {
            btnNextRound.setVisibility(View.VISIBLE);
            Log.d("Noam", "The button should be visible");
            dbz.collection("gameInstance").document(lobbyID)
                    .update("inRound", false);
        } else {
            // Hide it if someone is still answering or if you aren't the host
            btnNextRound.setVisibility(View.GONE);
        }
    }
    private void resetPlayersForNextRound() {
        WriteBatch batch = dbz.batch();

        // Reset every player's "ready" status so they can answer the next question
        for (Player p : playerList) {
            DocumentReference pRef = dbz.collection("gameInstance").document(lobbyID)
                    .collection("players").document(p.getuID());
            batch.update(pRef, "isReady", false);
        }

        String[] themes = {"Quadratic Equations", "Percentages", "Geometry", "Fractions", "Algebra"};

// 2. Pick a random index
        int randomIndex = new Random().nextInt(themes.length);
        String chosenTheme = themes[randomIndex];


        // Update the main game state to trigger the screen switch for everyone
        DocumentReference gameRef = dbz.collection("gameInstance").document(lobbyID);
        batch.update(gameRef, "currentTheme", chosenTheme);
        batch.update(gameRef, "inRound", true);

        batch.commit().addOnSuccessListener(aVoid -> {
            Log.d("GAME", "Next round started!");
        });
    }

    // Inside LeaderboardFragment's onViewCreated or an initialization method
    private void setupStateListener() {
        dbz.collection("gameInstance").document(lobbyID)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        Boolean inRound = snapshot.getBoolean("inRound");

                        // If the Host flipped the switch to true...
                        if (inRound != null && inRound) {

                            // 1. Tell the Activity to show the pre-loaded question
                            if (getActivity() instanceof Game_Page) {
                                ((Game_Page) getActivity()).startNewRoundUI();
                            }

                            // 2. Remove THIS fragment from the screen
                            getParentFragmentManager().beginTransaction()
                                    .remove(this)
                                    .commit();
                        }
                    }
                });
    }
    private void updateLeaderboard(com.google.firebase.firestore.QuerySnapshot value) {
        // 1. Convert the snapshot into your List of Player objects
        this.playerList.clear();
        this.playerList.addAll(value.toObjects(Player.class));

        // 2. Sort them by points (Highest first)
        Collections.sort(playerList, (p1, p2) ->
                Integer.compare(p2.getCurrentPoints(), p1.getCurrentPoints()));

        // 3. Tell the adapter to refresh the UI
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        // 4. Run your sync check
        checkEveryoneReady(playerList);
    }

}