package com.example.noamapp;

public class User {
    public String username;
    public int numberOfWins;

    // The empty constructor is MANDATORY for Firestore's .toObject() method
    public User() {}

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getNumberOfWins() {
        return numberOfWins;
    }

    public void setNumberOfWins(int numberOfWins) {
        this.numberOfWins = numberOfWins;
    }

    public User(String username, int numberOfWins) {
        this.username = username;
        this.numberOfWins = numberOfWins;
    }
}