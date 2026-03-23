package com.example.noamapp;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private List<Player> playerList;

    public LeaderboardAdapter(List<Player> playerList) {
        this.playerList = playerList;
    }

    // This creates the "Row" UI for the first time
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard_row, parent, false);
        return new ViewHolder(view);
    }

    // This puts the DATA into the UI
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Player player = playerList.get(position);

        holder.txtRank.setText("#" + (position + 1));
        holder.txtUsername.setText(player.username);
        holder.txtPoints.setText(String.valueOf(player.currentPoints));
        Log.d("Noam", "The data: "+ position+  player.username + player.currentPoints);
    }

    @Override
    public int getItemCount() {
        return playerList.size();
    }

    // The ViewHolder "holds" the views so we don't have to keep searching for them
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtRank, txtUsername, txtPoints;

        public ViewHolder(View itemView) {
            super(itemView);
            txtRank = itemView.findViewById(R.id.txt_rank);
            txtUsername = itemView.findViewById(R.id.txt_username);
            txtPoints = itemView.findViewById(R.id.txt_points);
        }
    }
}