package com.qolorco.paytask;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;

public class ProfileActivity extends AppCompatActivity {

    TextView userName, userEmail,walletTxt;
    Button addMoneyBtn;

    LinearLayout joinTelegram,joinYouTube;

    FirebaseUser user;
    FirebaseAuth auth;
    FirebaseFirestore db;
    GetDetails getDetails;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        walletTxt = findViewById(R.id.walletTxt);
        addMoneyBtn = findViewById(R.id.addMoneyBtn);
        joinTelegram = findViewById(R.id.joinTelegram);
        joinYouTube = findViewById(R.id.joinYouTube);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        getDetails = new GetDetails();

        ImageView backImg = findViewById(R.id.backButton);
        backImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        db.collection("users").document(user.getUid()).addSnapshotListener((snapshot, error) -> {
            FetchDataFromFireStore();

        });

        joinTelegram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(getDetails.getMainTelegram()));
                startActivity(intent);
            }
        });
        joinYouTube.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(getDetails.getMainYoutube()));
                startActivity(intent);
            }
        });
        addMoneyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ProfileActivity.this, WithdrawActivity.class));
            }
        });


        FetchDataFromFireStore();
    }
    private void FetchDataFromFireStore() {
        if (user != null) {
            String uid = user.getUid();

            db.collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Double wallet = documentSnapshot.getDouble("wallet");
                            String name = documentSnapshot.getString("username");
                            String email = documentSnapshot.getString("email");

                            userEmail.setText(email);
                            userName.setText(name);
                            DecimalFormat decimalFormat = new DecimalFormat("0.00");
                            walletTxt.setText("₹" + decimalFormat.format(wallet));
                        } else {
                            Toast.makeText(this, "User data not found!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show();
                    });
        }

    }
}