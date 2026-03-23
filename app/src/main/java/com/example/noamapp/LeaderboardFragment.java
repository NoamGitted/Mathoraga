package com.example.noamapp;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
    private Button btnNextRound, btnEnd;

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
        btnEnd = view.findViewById(R.id.btn_end);
        // Set up the RecyclerView's "shape" (Vertical List)
        adapter = new LeaderboardAdapter(playerList);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        startLeaderboardListener();
        setupStateListener();

        btnNextRound.setOnClickListener(v -> {
            // 1. Tell Firestore to move the game to the next state
            //set 'inRound' to 'false' and 'isReady' for all players to 'false'
            resetPlayersForNextRound();
        });

        btnEnd.setOnClickListener(v -> {
            if (playerList == null || playerList.isEmpty()) return;

            // 1. Identify the Winner (Index 0 is the highest score)
            Player winner = playerList.get(0);

            // 2. The Final Transaction: Update the Winner's Permanent Profile
            dbz.collection("users").document(winner.getuID())
                    .update("numberOfWins", com.google.firebase.firestore.FieldValue.increment(1))
                    .addOnSuccessListener(aVoid -> {

                        // 3. Cleanup: Delete the Lobby Document
                        // This triggers the 'SnapshotListener' on everyone else's phone to kick them out
                        dbz.collection("gameInstance").document(lobbyID).delete()
                                .addOnSuccessListener(deleted -> {
                                    // 4. Exit: Close the Activity for the Host
                                    if (getActivity() != null) {
                                        // Create Intent to go back to MainMenu
                                        Intent intent = new Intent(getActivity(), MainMenu.class);
                                        // Clear the stack so this new MainMenu is the only thing open
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);

                                        getActivity().finish(); // Close the current Game_Page
                                    }
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Fail","Failed to save the win!");
                    });
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
        dbz.collection("gameInstance").document(lobbyID).get().addOnSuccessListener(snapshot -> {
            if (snapshot != null && snapshot.exists()) {
                // 1. Get the round count
                Long roundsLeft = snapshot.getLong("roundsLeft");

                // 2. Update the UI text
                txtlobbyID.setText("Lobby: " + lobbyID + " | Rounds Left: " + roundsLeft);

                // 3. Perform the "Ready" loop
                boolean allReady = true;
                for (Player p : players) {
                    if (!p.isReady()) {
                        allReady = false;
                        break;
                    }
                }

                // 4. Handle Button Visibility
                if (allReady && isHost) {
                    if (roundsLeft != null && roundsLeft <= 0) {
                        btnNextRound.setVisibility(View.GONE);
                        btnEnd.setVisibility(View.VISIBLE);
                    } else {
                        btnNextRound.setVisibility(View.VISIBLE);
                        btnEnd.setVisibility(View.GONE);
                    }
                }
            }
        });
    }
    private void resetPlayersForNextRound() {
        WriteBatch batch = dbz.batch();

        // 1. Reset every player's "ready" status
        for (Player p : playerList) {
            DocumentReference pRef = dbz.collection("gameInstance").document(lobbyID)
                    .collection("players").document(p.getuID());
            batch.update(pRef, "isReady", false);
        }

        // 2. Pick a random theme
        String[] themes = {"Quadratic Equations", "Percentages", "Geometry", "Fractions", "Algebra"};
        String chosenTheme = themes[new Random().nextInt(themes.length)];

        // 3. UPDATE THE LOBBY
        DocumentReference gameRef = dbz.collection("gameInstance").document(lobbyID);
        batch.update(gameRef, "currentTheme", chosenTheme);
        batch.update(gameRef, "inRound", true);
        batch.update(gameRef, "roundsLeft", com.google.firebase.firestore.FieldValue.increment(-1));

        batch.commit().addOnSuccessListener(aVoid -> {
            Log.d("GAME", "Next round started and roundsLeft decreased!");
        });
    }

    // Inside LeaderboardFragment's onViewCreated or an initialization method
    private void setupStateListener() {
        dbz.collection("gameInstance").document(lobbyID)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e("LOBBY", "Listen failed.", e);
                        return;
                    }

                    // --- THE KICK LOGIC ---
                    // If the document is null or doesn't exist, the Host has deleted it
                    if (snapshot == null || !snapshot.exists()) {
                        if (getActivity() != null) {
                            Toast.makeText(getContext(), "Game has ended!", Toast.LENGTH_SHORT).show();

                            // Create Intent to go back to MainMenu
                            Intent intent = new Intent(getActivity(), MainMenu.class);
                            // Clear the stack so this new MainMenu is the only thing open
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);

                            getActivity().finish(); // Close the current Game_Page
                        }
                        return; // Exit the listener early
                    }

                    // --- ROUND LOGIC ---
                    Boolean inRound = snapshot.getBoolean("inRound");

                    if (inRound != null && inRound) {
                        if (getActivity() instanceof Game_Page) {
                            ((Game_Page) getActivity()).startNewRoundUI();
                        }

                        getParentFragmentManager().beginTransaction()
                                .remove(this)
                                .commit();
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