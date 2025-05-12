package com.example.chat_application;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText emailEdit, passwordEdit;
    private Button loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEdit = findViewById(R.id.emailEdit);
        passwordEdit = findViewById(R.id.passwordEdit);
        loginBtn = findViewById(R.id.loginBtn);

        mAuth = FirebaseAuth.getInstance();

        loginBtn.setOnClickListener(v -> {
            String email = emailEdit.getText().toString();
            String password = passwordEdit.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            proceedToChat();
                        } else {
                            // Try signing up if sign-in failed
                            mAuth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(signupTask -> {
                                        if (signupTask.isSuccessful()) {
                                            proceedToChat();
                                        } else {
                                            Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                                            Log.e("AuthError", signupTask.getException().getMessage());
                                        }
                                    });
                        }
                    });
        });
    }

    private void proceedToChat() {
        FirebaseUser user = mAuth.getCurrentUser();
        String name = user.getEmail().split("@")[0]; // Derive name from email
        Intent intent = new Intent(LoginActivity.this, ChattingSpace.class);
        intent.putExtra("name", name);
        startActivity(intent);
        finish(); // Prevent going back to login
    }
}
