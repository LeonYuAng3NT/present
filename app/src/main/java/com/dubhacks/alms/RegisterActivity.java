package com.dubhacks.alms;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {
    Button btnSign;
    EditText emailSign, passwordSign;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        btnSign = findViewById(R.id.btnSign);
        emailSign = findViewById(R.id.emailSign);
        passwordSign = findViewById(R.id.passwordSign);


        mAuth = FirebaseAuth.getInstance();

        btnSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // register a new user; if successfully registered, open the add event page
                mAuth.createUserWithEmailAndPassword(emailSign.getText().toString(),
                        passwordSign.getText().toString())
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    //FirebaseUser user = mAuth.getCurrentUser();
                                    Toast.makeText(RegisterActivity.this, "Register Succeeded",
                                            Toast.LENGTH_SHORT).show();
                                    openMapActivity();
                                } else {
                                    Toast.makeText(RegisterActivity.this, task.getException().getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
    }

    /**
     * Send the event host to the AddEvent page
     */
    public void openMapActivity() {
        Intent intent = new Intent(RegisterActivity.this, MapActivity.class);
        startActivity(intent);
    }

};