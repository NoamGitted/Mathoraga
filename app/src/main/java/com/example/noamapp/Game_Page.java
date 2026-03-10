package com.example.noamapp;

import android.os.Bundle;
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
import com.google.gson.Gson;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Game_Page extends AppCompatActivity implements View.OnClickListener {
    private GenerativeModelFutures model;
    private static final String TAG = "Noam";
    private int correctAnswerIndex;
    private TextView txtQuestion;
    private Button btnOpt1;
    private Button btnOpt2;
    private Button btnOpt3;
    private Button btnOpt4;
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
        setupGemini();
        txtQuestion = findViewById(R.id.tvquestion);
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
    public void onClick(View v) {
    int id = v.getId();
    if(id == R.id.btnanswer1){
        if (correctAnswerIndex == 0){
            Toast.makeText(Game_Page.this, "True", Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(Game_Page.this, "Nah", Toast.LENGTH_LONG).show();
        }
    }
        if(id == R.id.btnanswer2){
            if (correctAnswerIndex == 1){
                Toast.makeText(Game_Page.this, "True", Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(Game_Page.this, "Nah", Toast.LENGTH_LONG).show();
            }
        }
        if(id == R.id.btnanswer3){
            if (correctAnswerIndex == 2){
                Toast.makeText(Game_Page.this, "True", Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(Game_Page.this, "Nah", Toast.LENGTH_LONG).show();
            }
        }
        if(id == R.id.btnanswer4){
            if (correctAnswerIndex == 3){
                Toast.makeText(Game_Page.this, "True", Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(Game_Page.this, "Nah", Toast.LENGTH_LONG).show();
            }
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
    }
    public static class MathQuestion {
        public String question;
        public String[] answers;
        public int correctAnswerIndex;
        public MathQuestion() {}

    }
}