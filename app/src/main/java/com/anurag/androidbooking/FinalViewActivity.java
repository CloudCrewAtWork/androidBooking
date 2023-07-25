package com.anurag.androidbooking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import org.w3c.dom.Document;

public class FinalViewActivity extends AppCompatActivity {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseAuth auth = FirebaseAuth.getInstance();
    Button book;
    TextView alert;

    long maxLimit;
    long limit;
    long slotNumber;
    Scheduler schedule;

    @Override
    protected void onStart() {
        super.onStart();

        db.collection("Scheduler")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            for (final QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("Get Scheduler", document.getId() + " => " + document.getData());
                                maxLimit = (long) document.get("maxPeopleLimit");
                                Log.d("Get Scheduler","maxLimit = " + maxLimit);

                                db.collection("Count")
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                                        Log.d("Collection Count", document.getId() + " => " + document.getData());
                                                        limit = (long) document.get("log");
                                                    }

                                                    db.collection("TimeSlots")
                                                            .whereEqualTo("email", auth.getCurrentUser().getEmail())
                                                            .get()
                                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                    if (task.isSuccessful()) {
                                                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                                                            Log.d("Get TimeSlots", document.getId() + " => " + document.getData());
                                                                            if (task.getResult().size() == 1) {
                                                                                slotNumber = (long) document.get("SlotDetail");
                                                                            } else {
                                                                                slotNumber = 0;
                                                                            }

                                                                        }
                                                                        Log.d("Get TimeSlots","Count: " + task.getResult().size());
                                                                        Log.d("Slot:", " " + slotNumber);
                                                                        Log.d("Get Scheduler","maxLimit = " + maxLimit);
                                                                        //If result size is equal to zero, there is no booking for the current user

                                                                        if (limit == maxLimit) {
                                                                            Log.d("Limit reached","booked out");
                                                                            Toast.makeText(FinalViewActivity.this, "Already booked out", Toast.LENGTH_SHORT).show();
                                                                            alert.setText("Cafeteria booked out");

                                                                        } else if (slotNumber != 0) {
                                                                            Log.d("Already","booked");
                                                                            Toast.makeText(FinalViewActivity.this, "You already booked", Toast.LENGTH_SHORT).show();
                                                                            alert.setText("Already booked");


                                                                        } else {
                                                                            //available
                                                                            Toast.makeText(FinalViewActivity.this, "Booking available", Toast.LENGTH_SHORT).show();
                                                                            book.setEnabled(true);
                                                                            book.setVisibility(View.VISIBLE);
                                                                        }

                                                                    } else {
                                                                        Log.d("Get TimeSlots", "Error getting documents: ", task.getException());
                                                                    }
                                                                }
                                                            });
                                                } else {
                                                    Log.d("Collection Count", "Error getting documents: ", task.getException());
                                                }
                                            }
                                        });
                            }
                        } else {
                            Log.d("Get Scheduler", "Error getting documents: ", task.getException());
                        }
                    }
                });
        Log.d("Get Scheduler end","maxLimit = " + maxLimit);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_view);
        book = findViewById(R.id.bookButton);
        alert = findViewById(R.id.alertBox);
    }

    public void logoutPressed(View view) {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(FinalViewActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void bookSlot(View view) {
        String email = auth.getCurrentUser().getEmail();
        String userId = auth.getCurrentUser().getUid();
        Log.println(Log.INFO, "Email", email);
        Log.println(Log.INFO, "User ID", userId);

        // Query the "Slots" collection for the current user's ID
        db.collection("Slots")
                .document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                // The user already has a slot, show AlertDialog with delete option
                                showDeleteSlotAlertDialog(userId);
                            } else {
                                // The user doesn't have a slot, create a new one
                                createNewSlot(userId, email);
                            }
                        } else {
                            Log.d("checkSlot", "Error getting document: ", task.getException());
                        }
                    }
                });
    }

    // Function to show AlertDialog with delete option
    private void showDeleteSlotAlertDialog(String userId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Slot Exists");
        builder.setMessage("You already have a slot. Do you want to delete it?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Call function to delete the slot
                deleteSlot(userId);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Function to create a new slot for the logged-in user
    private void createNewSlot(String userId, String email) {
        db.collection("Slots")
                .document(userId)
                .set(new Slot("hello", email, userId), SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("createUser", "Document created successfully");
                        Toast.makeText(FinalViewActivity.this, "Slot created successfully.", Toast.LENGTH_SHORT).show();
                        showAlertMessage("Slot created successfully.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("createUser", "Error creating document: ", e);
                        Toast.makeText(FinalViewActivity.this, "Failed to create slot.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Function to delete the slot for the logged-in user
    private void deleteSlot(String userId) {
        db.collection("Slots")
                .document(userId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("deleteSlot", "Slot deleted successfully");
                        showAlertMessage("Slot deleted successfully");
                        Toast.makeText(FinalViewActivity.this, "Slot deleted successfully.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Toast.makeText(FinalViewActivity.this, "Failed to delete slot.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAlertMessage(String message) {
        TextView alertTextView = findViewById(R.id.alertBox); // Replace "R.id.Alert" with the actual ID of your TextView
        alertTextView.setText(message);
    }


    public class Slot {
        private String name;
        private String email;
        private String uniqueId;

        public Slot(String name, String email, String uniqueId) {
            this.name = name;
            this.email = email;
            this.uniqueId = uniqueId;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getUniqueId() {
            return uniqueId;
        }
    }

}


