package com.example.noamapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.ai.*;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.common.util.concurrent.*;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerationConfig;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.gson.Gson;

import java.util.Collections;

public class Game_Page extends AppCompatActivity {
    private GenerativeModelFutures modelFutures;
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
        //here I put the setupGemini
    }
    private void setupGemini() {
        // 1. The Persona
        Content systemInstruction = new Content.Builder()
                .addText("You are a high school math teacher. Response must be JSON.")
                .build();

        // 2. The Technical Rules
        GenerationConfig config = new GenerationConfig.Builder()
                .setResponseMimeType("application/json")
                .setTemperature(0.7f)
                .build();

        // 3. THE FIX: The Java GenerativeModel Builder
        // This removes all slot-guessing.
        GenerativeModel model = new GenerativeModel.Builder()
                .setModelName("gemini-3-flash-preview")
                .setBackend(GenerativeBackend.googleAI())
                .setGenerationConfig(config)
                .setSystemInstruction(systemInstruction) // Java now knows this is Content
                .build();

        // 4. The Java Compatibility Wrapper
        modelFutures = GenerativeModelFutures.from(model);
    }
    //here I put the fetchNewQuestion
    public static class MathQuestion {
        public String question;
        public String[] answers;
        public int correctAnswerIndex;
        public MathQuestion() {}

    }
}