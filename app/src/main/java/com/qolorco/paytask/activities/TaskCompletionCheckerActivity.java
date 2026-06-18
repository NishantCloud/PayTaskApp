package com.qolorco.paytask.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.icu.text.DecimalFormat;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;
import com.qolorco.paytask.R;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TaskCompletionCheckerActivity extends AppCompatActivity {

    AlertDialog dialog;
    ConstraintLayout redirectionLayout, taskCompletionLayout;
    String url;
    String taskId;
    String name;
    String price;
    String tutorial;

    TextView taskTitle, taskReward, taskCheckingTxt;
    Button backToTasksBtn;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_completion_checker);
        ImageView backImg = findViewById(R.id.backButton);
        backImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(TaskCompletionCheckerActivity.this, TasksListActivity.class));
                finish();
            }
        });
        redirectionLayout = findViewById(R.id.redirectionLayout);
        taskCompletionLayout = findViewById(R.id.taskCompletionLayout);

        taskTitle = findViewById(R.id.taskTitle);
        backToTasksBtn = findViewById(R.id.backToTasksBtn);
        taskReward = findViewById(R.id.taskReward);
        taskCheckingTxt = findViewById(R.id.taskCheckingTxt);



        redirectionLayout.setVisibility(View.VISIBLE);
        taskCompletionLayout.setVisibility(View.GONE);

        backToTasksBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(TaskCompletionCheckerActivity.this,TasksListActivity.class));
                finish();
            }
        });


    }


    @Override
    protected void onResume() {
        handleIntent(getIntent());
        super.onResume();
    }

    @Override
    protected void onStart() {
        detectTimeManipulation();
        super.onStart();
    }

    @Override
    protected void onRestart() {
        detectTimeManipulation();
        super.onRestart();
    }

    @SuppressLint("ResourceAsColor")
    private void handleIntent(Intent intent) {
        Uri data = intent.getData();
        if (data == null) return;

        String host = data.getHost();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        SharedPreferences prefs = getSharedPreferences("feature_unlock", MODE_PRIVATE);


        //Toast.makeText(this, "Checking Task Completeion", Toast.LENGTH_SHORT).show();
        taskCheckingTxt.setText("Checking Task Completion");
        if ("unlock_reward1".equals(host)) {
            TaskCompleted("task1");

        }
    }

    private void TaskCompleted(String TaskCheckingID) {
        String uid = FirebaseAuth.getInstance().getUid();
        completeTaskAndRewardUser(this, uid, TaskCheckingID, () -> {
            redirectionLayout.setVisibility(View.GONE);
            taskCompletionLayout.setVisibility(View.VISIBLE);
        });
    }

    public void completeTaskAndRewardUser(Context context, String uid, String taskId, Runnable onSuccess) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference userTaskRef = db.collection("users").document(uid).collection("tasks").document(taskId);
        DocumentReference taskRef = db.collection("Tasks").document(taskId);
        DocumentReference userRef = db.collection("users").document(uid);

        userTaskRef.get().addOnSuccessListener(taskSnapshot -> {
            if (taskSnapshot.exists()) {
                String value = taskSnapshot.getString("value");
                if (value != null && value.equals("n")) {

                    // Step 1: Get task price from global Tasks collection
                    taskRef.get().addOnSuccessListener(taskDoc -> {
                        if (taskDoc.exists()) {
                            double price = taskDoc.getDouble("price");
                            String taskName = taskDoc.getString("name");

                            // Step 2: Update user main data atomically
                            db.runTransaction((Transaction.Function<Void>) transaction -> {
                                DocumentSnapshot userSnap = transaction.get(userRef);

                                double wallet = userSnap.getDouble("wallet") != null ? userSnap.getDouble("wallet") : 0.0;
                                double todayEarn = userSnap.getDouble("today_earning") != null ? userSnap.getDouble("today_earning") : 0.0;
                                double totalEarn = userSnap.getDouble("total_earning") != null ? userSnap.getDouble("total_earning") : 0.0;
                                long todayTasks = userSnap.getLong("today_task_completed") != null ? userSnap.getLong("today_task_completed") : 0;
                                long totalTasks = userSnap.getLong("total_task_completed") != null ? userSnap.getLong("total_task_completed") : 0;

                                transaction.update(userRef, "wallet", wallet + price);
                                transaction.update(userRef, "today_earning", todayEarn + price);
                                transaction.update(userRef, "total_earning", totalEarn + price);
                                transaction.update(userRef, "today_task_completed", todayTasks + 1);
                                transaction.update(userRef, "total_task_completed", totalTasks + 1);

                                // Step 3: Set task as completed with timestamp
                                transaction.set(userTaskRef, Collections.singletonMap("value", String.valueOf(System.currentTimeMillis())));

                                return null;
                            }).addOnSuccessListener(unused -> {

                                DecimalFormat decimalFormat = new DecimalFormat("0.00");
                                taskReward.setText("+ ₹" + decimalFormat.format(price) + " Added");
                                taskTitle.setText("Task : " + taskName);
                                addTaskToHistory(taskId,taskName,price);
                               // Toast.makeText(context, "Task completed! ₹" + price + " added", Toast.LENGTH_SHORT).show();
                                if (onSuccess != null) onSuccess.run();
                            }).addOnFailureListener(e -> {
                                //Toast.makeText(context, "Transaction failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });

                        } else {
                            Toast.makeText(context, "Task not found in main list", Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    Toast.makeText(context, "Task already completed or invalid", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "User task not found", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void addTaskToHistory(String taskId, String taskName, double price) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getUid();

        Map<String, Object> historyData = new HashMap<>();
        historyData.put("task_name", taskName);
        historyData.put("taskId", taskId);
        historyData.put("price", price);
        historyData.put("time", FieldValue.serverTimestamp());

        db.collection("users")
                .document(uid)
                .collection("task_history")
                .add(historyData)
                .addOnSuccessListener(documentReference ->
                        Log.d("TaskHistory", "History saved successfully"))
                .addOnFailureListener(e ->
                        Log.e("TaskHistory", "Error saving history", e));
    }

    public void detectTimeManipulation() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Save current server time
        DocumentReference tempRef = db.collection("time_check").document("check");
        Map<String, Object> timeMap = new HashMap<>();
        timeMap.put("server_time", FieldValue.serverTimestamp());

        tempRef.set(timeMap).addOnSuccessListener(aVoid -> {
            tempRef.get().addOnSuccessListener(doc -> {
                Timestamp serverTimestamp = doc.getTimestamp("server_time");
                if (serverTimestamp != null) {
                    long serverTime = serverTimestamp.toDate().getTime();
                    long deviceTime = System.currentTimeMillis();

                    long diffInMillis = Math.abs(deviceTime - serverTime);
                    long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);

                    if (diffInMinutes > 3) { // Set 3 min tolerance
                        // ⚠ Time tampering detected
                        ShowWarningDialog();
                        // Optional: block access
                        // finishAffinity();
                    } else {
                        Log.d("TIME_CHECK", "✅ Time is correct.");
                    }
                }
            });
        });
    }


    private void ShowWarningDialog() {


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.item_time_change_dialog, null);


        builder.setView(view);
        builder.setCancelable(false);

        dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }
}