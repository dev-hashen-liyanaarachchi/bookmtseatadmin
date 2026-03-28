package com.hash.bookmyseatadmin.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.hash.bookmyseatadmin.R;

public class AdminLoginActivity extends AppCompatActivity {

    private TextInputLayout layoutEmail, layoutPassword;
    private MaterialButton btnLogin;
    private ImageView btnBack;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views FIRST
        initViews();

        // Check if already logged in (after views initialized)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkIfAdmin(currentUser);
            return;
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // Check if views are null
        if (btnBack == null || layoutEmail == null || layoutPassword == null || btnLogin == null) {
            Toast.makeText(this, "Error: Views not found", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnBack.setOnClickListener(v -> finish());
        btnLogin.setOnClickListener(v -> loginAdmin());
    }

    private void loginAdmin() {
        String email = layoutEmail.getEditText().getText().toString().trim();
        String password = layoutPassword.getEditText().getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            layoutEmail.setError("Email required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            layoutPassword.setError("Password required");
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (btnLogin == null) return; // Safety check

                        btnLogin.setEnabled(true);
                        btnLogin.setText("Login");

                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            checkIfAdmin(user);
                        } else {
                            String error = task.getException().getMessage();
                            if (error.contains("no user record")) {
                                Toast.makeText(AdminLoginActivity.this,
                                        "Admin not found. Please contact super admin.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(AdminLoginActivity.this,
                                        "Login failed: " + error,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

    private void checkIfAdmin(FirebaseUser user) {
        db.collection("admins")
                .whereEqualTo("email", user.getEmail())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (btnLogin == null) return; // Safety check

                        btnLogin.setEnabled(true);
                        btnLogin.setText("Login");

                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            // User is admin
                            startActivity(new Intent(AdminLoginActivity.this, AdminDashboardActivity.class));
                            finish();
                        } else {
                            // Not admin - sign out
                            mAuth.signOut();
                            Toast.makeText(AdminLoginActivity.this,
                                    "Unauthorized: Not an admin account",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}