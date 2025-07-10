package com.qolorco.paytask;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Date;
import java.util.List;

public class WithdrawalAdapter extends RecyclerView.Adapter<WithdrawalAdapter.ViewHolder> {

    List<WithdrawalModel> list;

    public WithdrawalAdapter(List<WithdrawalModel> list) {
        this.list = list;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView upiText, amountText, statusText, timeText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            upiText = itemView.findViewById(R.id.tvUpi);
            amountText = itemView.findViewById(R.id.tvAmount);
            statusText = itemView.findViewById(R.id.tvStatus);
            timeText = itemView.findViewById(R.id.tvTime);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_withdrawal, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int i) {
        WithdrawalModel w = list.get(i);
        h.upiText.setText("EMAIL: " + w.getQolor_email());
        h.amountText.setText("₹" + w.getAmount());
        h.statusText.setText(w.getStatus().toUpperCase());
        switch (w.getStatus().toUpperCase()) {
            case "PENDING":
                break;
            case "SUCCESS":
                h.statusText.setTextColor(Color.GREEN);
                break;
            case "FAILED":
                h.statusText.setTextColor(Color.RED);
                break;
        }

        if (w.getRequested_at() != null) {
            Date date = w.getRequested_at().toDate();
            h.timeText.setText(new SimpleDateFormat("dd MMM, hh:mm a").format(date));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}

