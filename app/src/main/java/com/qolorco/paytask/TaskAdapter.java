package com.qolorco.paytask;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<TaskModel> taskList;
    private String uid;

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_TASK = 1;

    public TaskAdapter(Context context, List<TaskModel> taskList) {
        this.context = context;
        this.taskList = taskList;
        this.uid = FirebaseAuth.getInstance().getUid();
    }

    @Override
    public int getItemViewType(int position) {
        return taskList.get(position).isHeader ? TYPE_HEADER : TYPE_TASK;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_task_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_task_card, parent, false);
            return new TaskViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TaskModel task = taskList.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).headerTitle.setText(task.name);
        } else {
            TaskViewHolder h = (TaskViewHolder) holder;

            h.taskName.setText(task.name);
            h.taskPrice.setText("₹" + String.format("%.2f", task.price));

            if (task.status.equals("n")) {
                h.taskButton.setText("Complete Now");
                h.taskButton.setEnabled(true);
                h.taskButton.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));
            } else {
                try {
                    long lastTime = Long.parseLong(task.status);
                    long nextAllowed = lastTime + (24 * 60 * 60 * 1000);
                    long now = System.currentTimeMillis();

                    if (now < nextAllowed) {
                        long remaining = nextAllowed - now;
                        long hrs = TimeUnit.MILLISECONDS.toHours(remaining);
                        long mins = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60;
                        h.taskButton.setText("Available Again\nAfter (" + hrs + "h " + mins + "m)");
                        h.taskButton.setEnabled(false);
                        h.taskButton.setBackgroundColor(ContextCompat.getColor(context, R.color.textLightGray));
                    } else {
                        h.taskButton.setText("Complete Now");
                        h.taskButton.setEnabled(true);
                        h.taskButton.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));
                    }
                } catch (Exception e) {
                    h.taskButton.setText("Complete Now");
                    h.taskButton.setEnabled(true);
                    h.taskButton.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));}
            }

            h.taskButton.setOnClickListener(v -> {
                if (task.status.equals("n")) {
                    Intent intent = new Intent(context, CompleteTasksActivity.class);
                    intent.putExtra("url", task.link);
                    intent.putExtra("taskId", task.taskId);
                    intent.putExtra("price", String.valueOf(task.price));
                    intent.putExtra("name", task.name);
                    intent.putExtra("tutorial", task.tutorial);
                    context.startActivity(intent);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskName, taskPrice;
        Button taskButton;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskName = itemView.findViewById(R.id.taskTitle);
            taskPrice = itemView.findViewById(R.id.taskPrice);
            taskButton = itemView.findViewById(R.id.completeButton);
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerTitle;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerTitle = itemView.findViewById(R.id.headerTitle);
        }
    }
}
