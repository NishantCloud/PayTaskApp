package com.qolorco.paytask;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CompleteTasksActivity extends AppCompatActivity {

    AlertDialog dialog;
    TextView taskReward,taskTitle;

    Button startTaskBtn, watchTutorialBtn;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_tasks);

        taskReward=findViewById(R.id.taskReward);
        taskTitle = findViewById(R.id.taskTitle);
        startTaskBtn = findViewById(R.id.startTaskBtn);
        watchTutorialBtn = findViewById(R.id.watchTutorialBtn);
        ImageView backImg = findViewById(R.id.backButton);
        backImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        String url = getIntent().getStringExtra("url");
        String taskId = getIntent().getStringExtra("taskId");
        String name = getIntent().getStringExtra("name");
        String price = getIntent().getStringExtra("price");
        String tutorial = getIntent().getStringExtra("tutorial");

        DecimalFormat format  = new DecimalFormat("0.00");
        if (price!=null) taskReward.setText("Reward: ₹"+format.format(Float.valueOf(price)));
        taskTitle.setText(name+" ( Link Shortener )");

        watchTutorialBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 assert tutorial != null;
                if (!tutorial.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(tutorial));
                    startActivity(intent);

                }   else {
                    new CustomToast().show(CompleteTasksActivity.this, "Tutorial is Not Available Yet!");
                }
            }
        });

        startTaskBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                finish();
                startActivity(intent);/*
                Intent intent = new Intent(CompleteTasksActivity.this, TaskCompletionCheckerActivity.class);
                intent.putExtra("url", url);
                intent.putExtra("taskId",  taskId);
                intent.putExtra("price",  price);
                intent.putExtra("name",  name);
                intent.putExtra("tutorial",  tutorial);
                startActivity(intent);*/
            }
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