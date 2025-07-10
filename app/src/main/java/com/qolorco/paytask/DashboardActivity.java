package com.qolorco.paytask;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.BuildConfig;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class DashboardActivity extends AppCompatActivity {


    TextView tvTotalEarning, tvTodayTasks, tvTodayEarning;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    FirebaseUser user ;
    LinearLayout bnbTasks,bnbProfile;

    Button btWithdraw,btJoinTelegram;

    private AlertDialog dialog;
    LoadingDialog dialog1;

    GetDetails getDetails;

    @Override
    protected void onResume() {
        UpdateLiveTime();
        super.onResume();
    }

    @Override
    protected void onRestart() {
        UpdateLiveTime();
        super.onRestart();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        UpdateLiveTime();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        View rootView = findViewById(android.R.id.content);
        View bottomNav = findViewById(R.id.bottomNav);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        getDetails = new GetDetails();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(DashboardActivity.this, LoginActivity.class));
            finish();
            return;
        }
        dialog1 = new LoadingDialog(this);
        dialog1.show("Updating Dashboard...");
        checkAndResetDailyStats();





        btJoinTelegram = findViewById(R.id.btJoinTelegram);
        btWithdraw = findViewById(R.id.btWithdraw);
        tvTotalEarning = findViewById(R.id.tvTotalEarning);
        tvTodayTasks = findViewById(R.id.tvTodayTasks);
        tvTodayEarning = findViewById(R.id.tvTodayEarning);
        bnbProfile = findViewById(R.id.bnbProfile);
        bnbTasks = findViewById(R.id.bnbTasks);

        bnbTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(DashboardActivity.this, TasksListActivity.class));
            }
        });
        bnbProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(DashboardActivity.this, ProfileActivity.class));
            }
        });

        btWithdraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(DashboardActivity.this, WithdrawActivity.class));
            }
        });

        btJoinTelegram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
               // Toast.makeText(DashboardActivity.this, "Link :"+getDetails.getWhatsappChannelLink(), Toast.LENGTH_SHORT).show();
                    intent.setData(Uri.parse(getDetails.getWhatsappChannelLink()));
                    startActivity(intent);


            }
        });

        checkForAppUpdate();
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Ensure bottom padding to avoid overlapping
            bottomNav.setPadding(
                    bottomNav.getPaddingLeft(),
                    bottomNav.getPaddingTop(),
                    bottomNav.getPaddingRight(),
                    navInsets.bottom
            );

            return insets;
        });

        db.collection("users").document(user.getUid()).addSnapshotListener((snapshot, error) -> {
            FetchDataFromFireStore();
            CheckForTasks();
        });
        FetchDataFromFireStore();
        CheckForTasks();

        //Ip Manipulation Checking
        new Thread(() -> {
            String ip = getPublicIPAddress();
            if (ip != null) {
                runOnUiThread(() -> {
                    saveUserIPToFirestore(ip);
                    checkForDuplicateIP(ip, mAuth.getUid());
                });
            } else {
                runOnUiThread(() -> Log.e("IP","Couldn't Fetch"));
            }
        }).start();

    }

    private void UpdateLiveTime(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) return;

        DocumentReference userRef = db.collection("users").document(uid);
        userRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists() && snapshot.contains("live_time")) {
                String liveTimeString = snapshot.getString("live_time");

                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.getDefault());
                try {
                    Date lastLiveDate = sdf.parse(liveTimeString);

                    Calendar lastCal = Calendar.getInstance();
                    lastCal.setTime(lastLiveDate);

                    Calendar nowCal = Calendar.getInstance();

                    boolean isNewDay =
                            nowCal.get(Calendar.YEAR) != lastCal.get(Calendar.YEAR) ||
                                    nowCal.get(Calendar.DAY_OF_YEAR) != lastCal.get(Calendar.DAY_OF_YEAR);

                    if (isNewDay) {
                        // Reset stats and update live_time
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("today_earning", 0);
                        updates.put("today_task_completed", 0);

                        // Update live_time to current
                        String newTime = sdf.format(new Date());
                        updates.put("live_time", newTime);

                        userRef.update(updates)
                                .addOnSuccessListener(unused ->
                                        Log.d("DailyReset", "Stats reset for new day"))
                                .addOnFailureListener(e ->
                                        Log.e("DailyReset", "Failed to reset stats", e));
                    } else {
                        // Just update live_time with current time
                        String newTime = sdf.format(new Date());
                        userRef.update("live_time", newTime);
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                    Log.e("DailyReset", "Failed to parse live_time string");
                }

            } else {
                // No live_time found – set current time for first run
                String now = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.getDefault()).format(new Date());
                userRef.update("live_time", now);
            }
        });

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
    public String getPublicIPAddress() {
        try {
            URL url = new URL("https://api.ipify.org");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            return reader.readLine();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ✅ Save IP address to Firestore under user document
    public void saveUserIPToFirestore(String ip) {
        Map<String, Object> update = new HashMap<>();
        update.put("ip_address", ip);
        update.put("ip_checked", true);

        db.collection("users").document(mAuth.getUid())
                .update(update)
                .addOnSuccessListener(aVoid -> Log.d("IP", "IP saved: " + ip))
                .addOnFailureListener(e -> Log.e("IP", "Error saving IP: " + e.getMessage()));
    }

    // ✅ Check if this IP is used by other users
    public void checkForDuplicateIP(String ip, String currentUid) {
        db.collection("users")
                .whereEqualTo("ip_address", ip)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int conflictCount = 0;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String uid = doc.getId();
                        if (!uid.equals(currentUid)) {
                            conflictCount++;
                        }
                    }

                    if (conflictCount > 0) {
                        // ⚠ Show warning
                        showIPConflictWarning();
                    } else {
                        Log.d("IP", "✅ No IP conflict detected.");
                    }
                })
                .addOnFailureListener(e -> Log.e("IP", "Error checking IP: " + e.getMessage()));
    }

    private void checkForAppUpdate() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("AppData").document("version")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String latestVersion = doc.getString("value");String currentVersion = "";
                        try {
                            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                            currentVersion = pInfo.versionName;
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }


                       // Toast.makeText(this, "L : "+latestVersion+" c : "+currentVersion, Toast.LENGTH_SHORT).show();
                        if (!currentVersion.equals(latestVersion)) {
                            showUpdateDialog();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                   // Toast.makeText(this, "Failed to check for update", Toast.LENGTH_SHORT).show();
                });



    }
    private void showUpdateDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.update_dialog);
        dialog.setCancelable(false);

        Button updateNow = dialog.findViewById(R.id.btnUpdateNow);
        updateNow.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(new GetDetails().getDownloadLink()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                // Toast.makeText(this, "Could not open Play Store", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.show();
    }
    private void checkAndResetDailyStats() {


        UpdateLiveTime();
    }



    // ✅ Show warning dialog if IP is in use by another user
    private void showIPConflictWarning() {

        String msg = "Another device is using the same IP address.\nPlease change your network.";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.item_warning_dialog, null);


        builder.setView(view);
        builder.setCancelable(false);

        TextView textView = view.findViewById(R.id.loadingMessage);
        textView.setText(msg);

        dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();

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

    private void CheckForTasks(){
       if (user != null) {
            String uid = user.getUid();

            // Step 1: Get all current tasks from main collection
            db.collection("Tasks").get().addOnSuccessListener(mainTasksSnapshot -> {
                Set<String> mainTaskIds = new HashSet<>();
                for (DocumentSnapshot doc : mainTasksSnapshot.getDocuments()) {
                    mainTaskIds.add(doc.getId()); // e.g. "task1", "task2"
                }

                // Step 2: Get user's current task subcollection
                db.collection("users")
                        .document(uid)
                        .collection("tasks")
                        .get()
                        .addOnSuccessListener(userTasksSnapshot -> {

                            // Step 3: Remove tasks not in main list
                            for (DocumentSnapshot doc : userTasksSnapshot.getDocuments()) {
                                String userTaskId = doc.getId();
                                if (!mainTaskIds.contains(userTaskId)) {
                                    db.collection("users")
                                            .document(uid)
                                            .collection("tasks")
                                            .document(userTaskId)
                                            .delete()
                                            .addOnSuccessListener(unused ->
                                                    Log.d("SYNC_TASKS", "Removed extra task: " + userTaskId)
                                            );
                                }
                            }

                            // Step 4: Add missing tasks (value: "n")
                            for (String taskId : mainTaskIds) {
                                DocumentReference userTaskRef = db.collection("users")
                                        .document(uid)
                                        .collection("tasks")
                                        .document(taskId);

                                userTaskRef.get().addOnSuccessListener(taskDoc -> {
                                    if (!taskDoc.exists()) {
                                        Map<String, Object> taskMap = new HashMap<>();
                                        taskMap.put("value", "n");
                                        userTaskRef.set(taskMap);
                                    }
                                });
                            }

                        }).addOnFailureListener(e ->
                                Log.e("SYNC_TASKS", "Failed to get user tasks: " + e.getMessage())
                        );

            }).addOnFailureListener(e ->
                    Log.e("SYNC_TASKS", "Failed to fetch main tasks: " + e.getMessage())
            );
        }

    }

    private void FetchDataFromFireStore() {
        if (user != null) {
            String uid = user.getUid();

            db.collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Double totalEarning = documentSnapshot.getDouble("total_earning");
                            Double todayEarning = documentSnapshot.getDouble("today_earning");
                            String todayTaskCompleted =String.valueOf( documentSnapshot.get("today_task_completed"));


                            dialog1.dismiss();
                            DecimalFormat decimalFormat = new DecimalFormat("0.00");  tvTotalEarning.setText("₹" + decimalFormat.format(totalEarning));
                            tvTodayTasks.setText(todayTaskCompleted + " Tasks");
                            tvTodayEarning.setText("Earned ₹" + decimalFormat.format(todayEarning) + " today");
                        } else {
                          //  Toast.makeText(this, "User data not found!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                       // Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show();
                    });
        }

    }
}