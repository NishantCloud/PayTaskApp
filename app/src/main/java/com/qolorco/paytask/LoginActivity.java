package com.qolorco.paytask;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ktx.Firebase;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {


    FirebaseAuth mAuth;
    FirebaseFirestore db;

    EditText etUsername, etEmail, etPassword, etConfirmPassword;
    CheckBox cbShowPassword;
    Button buttonMain, buttonChange;

    String deviceId;
    TextView topText1, topText2;
    LinearLayout warningLayout;
    LoadingDialog loadingDialog;
    boolean isRegistration = false;
    boolean isPasswordVisible = false;

    String devEmail = "nishantsharma98632@gmail.com";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db= FirebaseFirestore.getInstance();

        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        etEmail = findViewById(R.id.emailInput);
        etPassword = findViewById(R.id.passwordInput);
        etUsername = findViewById(R.id.userNameInput);
        etConfirmPassword = findViewById(R.id.confirmPasswordInput);
        buttonMain = findViewById(R.id.buttonMain);
        buttonChange = findViewById(R.id.buttonChange);

        topText1 = findViewById(R.id.topText1);
        topText2 = findViewById(R.id.topText2);
        warningLayout = findViewById(R.id.warningLayout);

        cbShowPassword = findViewById(R.id.cbShowPassword);

        cbShowPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Show passwords
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                // Hide passwords
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }


            // Maintain cursor position
            etPassword.setSelection(etPassword.getText().length());
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
        });
        buttonChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isRegistration = !isRegistration;
                isRegistration(isRegistration);
            }
        });

        loadingDialog = new LoadingDialog(this);
        buttonMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Show dialog
                etConfirmPassword.setError(null);
                etUsername.setError(null);
                etEmail.setError(null);
                etPassword.setError(null);

                if (isRegistration){
                    registerUser();
                }else{
                    loginUser();
                }
            }
        });

    }

    private void isRegistration(boolean isRegistration){

        etEmail.setText("");
        etPassword.setText("");
        etConfirmPassword.setText("");
        etUsername.setText("");


        etConfirmPassword.setError(null);
        etUsername.setError(null);
        etEmail.setError(null);
        etPassword.setError(null);

        if (isRegistration){
            etConfirmPassword.setVisibility(View.VISIBLE);
            etUsername.setVisibility(View.VISIBLE);
            etEmail.setVisibility(View.VISIBLE);
            etPassword.setVisibility(View.VISIBLE);

            etUsername.requestFocus();

            warningLayout.setVisibility(View.VISIBLE);
            topText1.setText("Create New Account");
            topText2.setVisibility(View.GONE);
            buttonMain.setText("Create");
            buttonChange.setText("Login");
        }else{
            etConfirmPassword.setVisibility(View.GONE);
            etUsername.setVisibility(View.GONE);
            etEmail.setVisibility(View.VISIBLE);
            etPassword.setVisibility(View.VISIBLE);

            etEmail.requestFocus();

            warningLayout.setVisibility(View.GONE);
            topText1.setText("Welcome Back!");
            topText2.setVisibility(View.VISIBLE);
            buttonMain.setText("Login");
            buttonChange.setText("SinUp");
        }
    }
    private void fetchPublicIP(OnIPFetchListener listener) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("https://api.ipify.org");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                java.io.BufferedReader in = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream())
                );
                String ip = in.readLine();
                in.close();

                runOnUiThread(() -> listener.onSuccess(ip));
            } catch (Exception e) {
                runOnUiThread(() -> listener.onFailure(e));
            }
        }).start();
    }

    interface OnIPFetchListener {
        void onSuccess(String ip);
        void onFailure(Exception e);
    }

    private boolean isVpnActive() {
        try {
            for (java.net.NetworkInterface ni : java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())) {
                if (ni.isUp() && (ni.getName().contains("tun") || ni.getName().contains("ppp"))) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isEmulator() {
        String brand = android.os.Build.BRAND;
        String device = android.os.Build.DEVICE;
        String fingerprint = android.os.Build.FINGERPRINT;
        String model = android.os.Build.MODEL;
        String product = android.os.Build.PRODUCT;
        String manufacturer = android.os.Build.MANUFACTURER;

        return fingerprint.startsWith("generic") ||
                model.contains("Emulator") ||
                model.contains("Android SDK built for x86") ||
                manufacturer.contains("Genymotion") ||
                product.contains("sdk") ||
                product.contains("google_sdk") ||
                product.contains("emulator") ||
                product.contains("simulator");
    }

    private void loginUser() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Input validations...
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter email");
            etEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter valid email");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter password");
            etPassword.requestFocus();
            return;
        }

        // Check for emulator or VPN usage
        if (isEmulator()&& !email.equals(devEmail)) {
            ShowToast("Login blocked on emulator");
            return;
        }
        if (isVpnActive()) {
            ShowToast("Disable VPN to login");
            return;
        }


        loadingDialog.show("Logging in...");

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                String uid = mAuth.getCurrentUser().getUid();

                db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String savedDeviceId = doc.getString("device_id");
                        if (deviceId.equals(savedDeviceId)) {
                            ShowToast("Login successful");
                            Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
                            // Redirect to dashboard
                            Intent intent = new Intent(this, DashboardActivity.class);
                            startActivity(intent);
                            finish(); // Prevent going back to login screen
                        } else {

                            loadingDialog.dismiss();
                            ShowToast("This account is linked to another device");
                            mAuth.signOut();
                        }
                    } else {
                        loadingDialog.dismiss();
                        ShowToast("User record not found");
                    }
                });
            } else {
                loadingDialog.dismiss();

                Exception e = task.getException();
                if (e instanceof FirebaseAuthInvalidUserException) {
                    etEmail.setError("No account found with this email");
                    etEmail.requestFocus();
                } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    etPassword.setError("Incorrect password");
                    etPassword.requestFocus();
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    ShowToast("Too many failed attempts. Try again later.");
                } else {
                    ShowToast("Login failed: " + e.getMessage());
                }
            }
        });
    }

    private void ShowToast(String msg) {
        new CustomToast().show(this,msg);

    }

    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // ==== Input Validations ====

        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Enter username");
            etUsername.requestFocus();
            return;
        }
        if (username.length() < 3) {
            etUsername.setError("Username must be at least 3 characters");
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter email");
            etEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter password");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }


        loadingDialog.show("Creating Account...");
        // ==== Check Device Lock ====
        db.collection("devices").document(deviceId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot doc = task.getResult();
                if (doc.exists()) {
                    loadingDialog.dismiss();
                    ShowToast("Only one account allowed per device");
                } else {
                    // ==== Firebase Auth ====
                    mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task1 -> {
                        if (task1.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();

                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("username", username);
                            userMap.put("email", email);
                            userMap.put("password", password); // Not safe in real app
                            userMap.put("device_id", deviceId);// Not safe in real app
                            userMap.put("today_earning", 0);// Not safe in real app
                            userMap.put("total_earning", 0);// Not safe in real app
                            userMap.put("today_task_completed", 0);// Not safe in real app
                            userMap.put("total_task_completed", 0);// Not safe in real app
                            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.getDefault());
                            userMap.put("live_time", sdf.format(new Date()));// Not safe in real app
                            userMap.put("wallet", 0);

                            db.collection("users").document(uid).set(userMap);

                            Map<String, Object> deviceMap = new HashMap<>();
                            deviceMap.put("uid", uid);
                            deviceMap.put("email", email);

                            db.collection("devices").document(deviceId).set(deviceMap);

                            loadingDialog.dismiss();
                            ShowToast("Registration successful. Login Account !!");
                            loadingDialog.show("Registration successful. Login Account !!");
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {

                              loadingDialog.dismiss();

                              isRegistration = !isRegistration;
                              isRegistration(isRegistration);
                                }  },2500);


                        } else {
                            loadingDialog.dismiss();

                            Exception e = task.getException();
                            if (e instanceof FirebaseAuthInvalidUserException) {
                                etEmail.setError("This email is already registered");
                                etEmail.requestFocus();
                            } else if (e instanceof FirebaseAuthWeakPasswordException) {
                                etPassword.setError("Weak password: " + ((FirebaseAuthWeakPasswordException) e).getReason());
                                etPassword.requestFocus();
                            } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                etEmail.setError("Invalid email format");
                                etEmail.requestFocus();
                            } else {
                                ShowToast("Registration failed: " + e.getMessage());
                            }
                        }
                    });
                }
            } else {
                loadingDialog.dismiss();
                ShowToast("Error checking device");
            }
        });
    }
}