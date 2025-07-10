package com.qolorco.paytask;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TasksListActivity extends AppCompatActivity {

    AlertDialog dialog;



    FirebaseFirestore db;
    FirebaseAuth auth;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks_list);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        ImageView backImg = findViewById(R.id.backButton);
        backImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        detectTimeManipulation();

        db.collection("users").document(auth.getUid()).addSnapshotListener((snapshot, error) -> {

            FetchDataFromFirestore();

        });
        FetchDataFromFirestore();
    }

    private void FetchDataFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getUid();
        List<TaskModel> taskList = new ArrayList<>();
        TaskAdapter adapter = new TaskAdapter(this, taskList);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        db.collection("Tasks").get().addOnSuccessListener(taskDocs -> {
            db.collection("users").document(uid).collection("tasks").get()
                    .addOnSuccessListener(userTasks -> {
                        Map<String, String> userTaskMap = new HashMap<>();
                        for (DocumentSnapshot snap : userTasks.getDocuments()) {
                            userTaskMap.put(snap.getId(), snap.getString("value"));
                        }

                        List<TaskModel> availableTasks = new ArrayList<>();
                        List<TaskModel> completedTasks = new ArrayList<>();
                        long currentTime = System.currentTimeMillis();
                        long cooldownMillis = 24 * 60 * 60 * 1000;

                        for (DocumentSnapshot doc : taskDocs) {
                            String taskId = doc.getId();
                            String name = doc.getString("name");
                            double price = doc.getDouble("price");
                            String link = doc.getString("link");
                            String tutorial = doc.getString("tutorial");

                            String value = "n";

                            if (userTaskMap.containsKey(taskId)) {
                                String val = userTaskMap.get(taskId);
                                if (val.equals("n")) {
                                    value = "n";
                                } else {
                                    try {
                                        long lastCompletedTime = Long.parseLong(val);
                                        if (currentTime - lastCompletedTime >= cooldownMillis) {
                                            // reset to n
                                            db.collection("users").document(uid)
                                                    .collection("tasks").document(taskId)
                                                    .update("value", "n");
                                            value = "n";
                                        } else {
                                            value = val;
                                        }
                                    } catch (NumberFormatException e) {
                                        value = "n";
                                    }
                                }
                            }

                            TaskModel taskModel = new TaskModel(taskId, name, price, link, value, tutorial);
                            if (value.equals("n")) {
                                availableTasks.add(taskModel);
                            } else {
                                completedTasks.add(taskModel);
                            }
                        }

                        taskList.clear();
                        if (!availableTasks.isEmpty()) {
                            taskList.add(new TaskModel("Available Tasks", true));
                            taskList.addAll(availableTasks);
                        }

                        if (!completedTasks.isEmpty()) {
                            taskList.add(new TaskModel("Completed Tasks", true));
                            taskList.addAll(completedTasks);
                        }

                        adapter.notifyDataSetChanged();
                    });
        });
    }




    @Override
    protected void onResume() {
        detectTimeManipulation();
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