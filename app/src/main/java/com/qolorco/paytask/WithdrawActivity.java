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
import com.google.firebase.firestore.Query;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WithdrawActivity extends AppCompatActivity {

    AlertDialog dialog;
    Button withdrawButton;
    TextView balanceAmount;

    EditText upiId, withdrawAmount;

    FirebaseFirestore db;
    FirebaseUser user;
    FirebaseAuth auth;
    RecyclerView transactionList;
    double minimumWithdraw = 100;
    double availableBalance=0;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();
        withdrawButton = findViewById(R.id.withdrawButton);
        upiId = findViewById(R.id.upiId);
        withdrawAmount = findViewById(R.id.withdrawAmount);
        balanceAmount = findViewById(R.id.balanceAmount);
        transactionList = findViewById(R.id.transactionList);
        ImageView backImg = findViewById(R.id.backImg);


        db.collection("users").document(user.getUid()).addSnapshotListener((snapshot, error) -> {
           GetFireBase();
        });
        detectTimeManipulation();
        GetFireBase();
        loadTransactionHistory();

        backImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        withdrawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptWithdraw();
            }
        });
    }

    public void attemptWithdraw() {
        String upi = upiId.getText().toString().trim();

        String amountStr = withdrawAmount.getText().toString().trim();


        if (upi.isEmpty()) {
            upiId.setError("Enter your Qolor Email");
            return;
        }

        if (amountStr.isEmpty()) {
            withdrawAmount.setError("Enter amount");
            return;
        }

        double amount = Double.parseDouble(amountStr);

        if (amount < minimumWithdraw) {
            withdrawAmount.setError("Minimum withdrawal is ₹100");
            return;
        }

        if (amount > availableBalance) {
            withdrawAmount.setError("Not enough balance");
            return;
        }

        // Save request
        Map<String, Object> data = new HashMap<>();
        data.put("user_id", user.getUid());
        data.put("qolor_email", upi);
        data.put("amount", amount);
        data.put("status", "pending");
        data.put("requested_at", FieldValue.serverTimestamp());

        db.collection("withdrawals").add(data)
                .addOnSuccessListener(doc -> {
                    ShowDoneDialog("₹"+amount+" Withdraw Request Sent!");
                    withdrawAmount.setText("");
                    upiId.setText("");
                    loadTransactionHistory();

                    DocumentReference userRef = db.collection("users").document(user.getUid());
                    userRef.update("wallet", FieldValue.increment(-amount))
                            .addOnSuccessListener(unused -> Log.d("Wallet", "Wallet updated"))
                            .addOnFailureListener(e -> Log.e("Wallet", "Wallet update failed: " + e.getMessage()));
                    // refresh history
                })
                .addOnFailureListener(e ->
                        Log.e("FireStore","Error: " + e.getMessage())
                );
    }

    public void loadTransactionHistory() {
        List<WithdrawalModel> list = new ArrayList<>();
        WithdrawalAdapter adapter = new WithdrawalAdapter(list);
        transactionList.setLayoutManager(new LinearLayoutManager(this));
        transactionList.setAdapter(adapter);

        db.collection("withdrawals")
                .whereEqualTo("user_id", FirebaseAuth.getInstance().getUid())
                .orderBy("requested_at", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    list.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        WithdrawalModel model = doc.toObject(WithdrawalModel.class);
                        list.add(model);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error: " + e.getMessage()));
    }

    private void GetFireBase() {
        if (user != null) {
            String uid = user.getUid();

            db.collection("AppData").document("minimum_withdraw").get().addOnSuccessListener(documentSnapshot -> {

                if (documentSnapshot.exists()){

                    String minimum_withdraw =String.valueOf( documentSnapshot.get("value"));
                    minimumWithdraw = Integer.parseInt(minimum_withdraw);
                    withdrawAmount.setHint("Enter amount (min ₹"+minimumWithdraw+")");
                }

            });
            db.collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Double wallet = documentSnapshot.getDouble("wallet");
                            String name = documentSnapshot.getString("username");
                            String email = documentSnapshot.getString("email");

                            availableBalance = wallet;
                            DecimalFormat decimalFormat = new DecimalFormat("0.00");
                            balanceAmount.setText("₹" + decimalFormat.format(wallet));
                        } else {
                            Toast.makeText(this, "User data not found!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show();
                    });
        }
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


    private void ShowDoneDialog(String msg) {


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.item_done_dialog, null);


        builder.setView(view);
        builder.setCancelable(true);

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
}