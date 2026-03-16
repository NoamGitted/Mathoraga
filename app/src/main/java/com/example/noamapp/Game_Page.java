package com.example.noamapp;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.ai.*;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.common.util.concurrent.*;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerationConfig;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.type.RequestOptions;
import com.google.firebase.ai.type.Schema;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Game_Page extends AppCompatActivity implements View.OnClickListener {
    //FireBase
    private GenerativeModelFutures model;
    private FirebaseAuth mAuth;
    private FirebaseFirestore dbz;

    //Other...
    private static final String TAG = "Noam";
    private int correctAnswerIndex;
    private String lobbyID, uID;

    //XML
    private TextView txtQuestion, txtTimer;
    private Button btnOpt1, btnOpt2, btnOpt3, btnOpt4;

    //Timer
    private CountDownTimer questionTimer;
    private long timeLeftInMillis;
    private static final long TOTAL_TIME = 20000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mAuth = FirebaseAuth.getInstance();
        dbz = FirebaseFirestore.getInstance();
        setupGemini();
        txtQuestion = findViewById(R.id.tvquestion);
        txtTimer = findViewById(R.id.txtTimer);
        lobbyID = getIntent().getStringExtra("LOBBY_ID");
        uID = mAuth.getUid();
        btnOpt1 = findViewById(R.id.btnanswer1);
        btnOpt2 = findViewById(R.id.btnanswer2);
        btnOpt3 = findViewById(R.id.btnanswer3);
        btnOpt4 = findViewById(R.id.btnanswer4);
        fetchNewQuestion();

        btnOpt1.setOnClickListener(this);
        btnOpt2.setOnClickListener(this);
        btnOpt3.setOnClickListener(this);
        btnOpt4.setOnClickListener(this);
    }
    protected void onDestroy() {
        super.onDestroy();
        // Stop the timer so it doesn't keep running in the background
        if (questionTimer != null) {
            questionTimer.cancel();
        }
    }

    public void onClick(View v) {
        // 1. Stop the timer immediately to "freeze" the time
        if (questionTimer != null) {
            questionTimer.cancel();
        }

        // 2. Calculate points using the current value of timeLeftInMillis
        // Example: Base 100 points + 10 points for every second left
        int secondsLeft = (int) (timeLeftInMillis / 1000);
        int scoreForThisRound = 100 + (secondsLeft * 10);

        // 3. Check if answer is correct
        int id = v.getId();
        int selectedIndex = -1;

        if (id == R.id.btnanswer1) selectedIndex = 0;
        else if (id == R.id.btnanswer2) selectedIndex = 1;
        else if (id == R.id.btnanswer3) selectedIndex = 2;
        else if (id == R.id.btnanswer4) selectedIndex = 3;

        if (selectedIndex == correctAnswerIndex) {
            Toast.makeText(this, "Correct! +" + scoreForThisRound, Toast.LENGTH_SHORT).show();
            updateInGamePoints(scoreForThisRound);
            btnEnabler(false);

        } else {
            Toast.makeText(this, "Wrong!", Toast.LENGTH_SHORT).show();
            fetchNewQuestion();
            btnEnabler(false);
        }
    }
    private void setupGemini() {
        // 1. The Schema (The structure/mold)
        Schema mathSchema = Schema.obj(
                Map.of(
                        "question", Schema.str(),
                        "answers", Schema.array(Schema.str()),
                        "correctAnswerIndex", Schema.numInt()
                ),
                List.of("question", "answers", "correctAnswerIndex")
        );

        // 2. Technical Rules (MimeType & Schema)
        GenerationConfig config = new GenerationConfig.Builder()
                .setResponseMimeType("application/json")
                .setResponseSchema(mathSchema)
                .setTemperature(1f)
                .build();

        // 3. THE 3-ARGUMENT CALL (Matches your IDE perfectly)
        GenerativeModel ai = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel(
                        "gemini-3-flash-preview",
                        config,
                        null // SafetySettings (expects List, null is allowed)
                );

        model = GenerativeModelFutures.from(ai);

    }

    private void fetchNewQuestion() {
        // 1. The Prompt (Including the Persona since it's not in our constructor)
        Content prompt = new Content.Builder()
                // The Identity + The Task
                .addText("You are a high school math teacher. Generate a new math question.")
                .build();
        // 2. The Request
        ListenableFuture<GenerateContentResponse> response = model.generateContent(prompt);


        // 3. The Callback (The "Waiting" room)
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String jsonOutput = result.getText();
                
                // Turn the AI's text into your MathQuestion object
                // Using Gson is the standard for this
                Gson gson = new Gson();
                MathQuestion questionObj = gson.fromJson(jsonOutput, MathQuestion.class);

                // Update the UI (Must be on the Main Thread)
                runOnUiThread(() -> {
                    displayQuestion(questionObj);
                });

            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("AI_ERROR", "Fetch failed: " + t.getMessage());
            }
        }, ContextCompat.getMainExecutor(this)); // Essential for Android thread safety
    }

    private void displayQuestion(MathQuestion mq) {
        // Assuming you have these views defined in your layout
        txtQuestion.setText(mq.question);
        btnOpt1.setText(mq.answers[0]);
        btnOpt2.setText(mq.answers[1]);
        btnOpt3.setText(mq.answers[2]);
        btnOpt4.setText(mq.answers[3]);
        // Save the correct index to a variable in your activity
        this.correctAnswerIndex = mq.correctAnswerIndex;
        startTimer();
        btnEnabler(true);
    }
    public static class MathQuestion {
        public String question;
        public String[] answers;
        public int correctAnswerIndex;
        public MathQuestion() {}

    }

    private void startTimer() {
        Log.d(TAG, "Timer started!"); // Check Logcat for this!
        if (questionTimer != null) {
            questionTimer.cancel(); // Stop any existing timer
        }

        questionTimer = new CountDownTimer(TOTAL_TIME, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                int seconds = (int) (millisUntilFinished / 1000);
                // Update a TextView so the user sees the countdown
                txtTimer.setText(String.valueOf(seconds));
                if (seconds <= 5) {
                    txtTimer.setTextColor(Color.RED);
                    txtTimer.setTextSize(24); // Make it pop
                } else {
                    txtTimer.setTextColor(Color.BLACK);
                    txtTimer.setTextSize(18);
                }
            }

            @Override
            public void onFinish() {
                txtTimer.setText("0");
                // Logic for when time runs out (e.g., auto-fetch next question)
                Toast.makeText(Game_Page.this, "Time's up!", Toast.LENGTH_SHORT).show();
                fetchNewQuestion();
            }
        }.start();
    }

    private void updateInGamePoints(int pointsToAdd) {

        if (lobbyID != null && uID != null) {
            // Path: gameInstance -> {lobbyID} -> players -> {uid}
            dbz.collection("gameInstance")
                    .document(lobbyID)
                    .collection("players")
                    .document(uID)
                    .update("currentPoints", com.google.firebase.firestore.FieldValue.increment(pointsToAdd))
                    .addOnSuccessListener(aVoid -> {
                        Log.d("NOAM_APP", "Points updated in DB!");
                        // After updating points, wait a moment or show the leaderboard,
                        // then fetch the next question.
                        fetchNewQuestion();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("NOAM_APP", "Failed to update points", e);
                    });
        }
    }

    private void btnEnabler(boolean q){
        btnOpt1.setEnabled(q);
        btnOpt2.setEnabled(q);
        btnOpt3.setEnabled(q);
        btnOpt4.setEnabled(q);
    }
}