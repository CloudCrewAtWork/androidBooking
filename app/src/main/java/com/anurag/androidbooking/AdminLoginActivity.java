package com.anurag.androidbooking;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.R;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.Objects;

public class AdminLoginActivity extends AppCompatActivity {

    FirebaseFirestore db;
    EditText adminEditText;
    String id;

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_admin_login);
//        db = FirebaseFirestore.getInstance();
//        adminEditText = findViewById(R.id.adminID);
    }

    public void loginAdmin(View view) {
        id = adminEditText.getText().toString();

        db.collection("AdminId")
                .whereEqualTo("Aid", id)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (Objects.requireNonNull(task.getResult()).size() == 1) {
                            //start new Intent
                            Intent adminIntent = new Intent(AdminLoginActivity.this, AdminFinalViewActivity.class);
                            startActivity(adminIntent);
                        } else {

                        }
                    } else {
                        Log.d("Query", "Error getting documents: ", task.getException());
                    }
                });
    }
}
