package com.anurag.androidbooking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    FirebaseFirestore db;

    String name;
    String password;
    String id;
    String email;

    EditText nameField;
    EditText idField;
    EditText mailField;
    EditText passwordField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        db = FirebaseFirestore.getInstance();
        nameField = findViewById(R.id.nameEditText);
        idField = findViewById(R.id.idEditText);
        mailField = findViewById(R.id.emailEditText);
        passwordField = findViewById(R.id.passwordEditText);
        mAuth = FirebaseAuth.getInstance();
        getUsers();
    }

    private void getUsers() {
        db.collection("Users")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Log.d("users","" + task.getResult().size());
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("TAG", document.getId() + " => " + document.getData());
                            }
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    public void registerClick(View view) {
        //check if user entered all fields
        name = nameField.getText().toString();
        password = passwordField.getText().toString();
        id = idField.getText().toString();
        email = mailField.getText().toString();

        if (name.isEmpty() || password.isEmpty() || id.isEmpty() || email.isEmpty()) {
            Toast.makeText(RegisterActivity.this, "Fill all fields", Toast.LENGTH_SHORT).show();
        } else {
            //Create user from details that are entered
            db.collection("Uid")
                    .whereEqualTo("id", id)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        int count = 0;

                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                Log.d("checked UID", "uid: " + id + "size: " + task.getResult().size());
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Log.d("TAG", document.getId() + " => " + document.getData());
                                    count++;
                                }
                                if (count == 0) {
                                    //No id found
                                    Toast.makeText(RegisterActivity.this, "invalid UID", Toast.LENGTH_SHORT).show();
                                } else {
                                    //id found -> valid
                                    Toast.makeText(RegisterActivity.this, "valid UID", Toast.LENGTH_SHORT).show();
                                    Log.d("checked UID", "uid: " + id);
                                    db.collection("Users")
                                            .whereEqualTo("uniqueId", id)
                                            .get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                int newCount=0;

                                                @Override
                                                public void onComplete(@NonNull Task<QuerySnapshot> task2) {
                                                    if (task2.isSuccessful()) {
                                                        Log.d("2checked UID", "uid: " + id + "size: " + task2.getResult().size());

                                                        for (QueryDocumentSnapshot document : task2.getResult()) {
                                                            Log.d("TAG", document.getId() + " => " + document.getData());
                                                            newCount++;
                                                        }
                                                        if (newCount == 0) {
                                                            //found no user with the id we entered -> register and create user
                                                            Toast.makeText(RegisterActivity.this, "ID available", Toast.LENGTH_SHORT).show();

                                                            register();
                                                        } else {
                                                            //id already taken
                                                            Toast.makeText(RegisterActivity.this, "ID already taken", Toast.LENGTH_SHORT).show();
                                                        }
                                                    } else {
                                                        Log.d("TAG", "Error getting documents: ", task2.getException());
                                                    }
                                                }
                                            });
                                }
                            } else {
                                Log.d("TAG", "Error getting documents: ", task.getException());
                            }
                        }
                    });
        }
    }

    private void createUser() {
//        Map<String, Object> newUser = new HashMap<>();
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setUniqueId(id);
//        newUser.put(user.getName(), user);
        db.collection("Users").document(name).set(user).addOnCompleteListener(new OnCompleteListener(){


            @Override
            public void onComplete(@NonNull Task task) {
                Log.println(Log.INFO, "Insertion Completed",user.getName());

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.println(Log.INFO, "Failed to Upload the data", e.toString());
            }
        });
//        .addOnSuccessListener(new OnCompleteListener<QuerySnapshot>(){
//
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Log.d("createUser", "error creating user");
//                    }
//                });
    }

    private void register() {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("TAG", "createUserWithEmail:success");
                            createUser();
                            //start new intent
                            Intent booking = new Intent(RegisterActivity.this, FinalViewActivity.class);
                            startActivity(booking);

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("TAG", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(RegisterActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

}
