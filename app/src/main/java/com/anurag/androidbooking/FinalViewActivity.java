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
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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

        // Get the current time in hours (24-hour format)
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);

        // Calculate the available slots from 9 am to 9 pm
        final int availableSlots = 12; // 12 slots from 9 am to 9 pm
        int startingHour = 9;
        int availableHourSlots = Math.max(availableSlots - (currentHour - startingHour), 0);

        if (availableHourSlots == 0) {
            Toast.makeText(this, "All slots have been booked for today.", Toast.LENGTH_SHORT).show();
        } else {
            // Show AlertDialog with available slots
            showAvailableSlotsAlertDialog(userId, email, availableHourSlots, startingHour);
        }
    }

    private void showAvailableSlotsAlertDialog(String userId, String email, int availableSlots, int startingHour) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Available Slots");

        // Inflate the custom layout
        View view = getLayoutInflater().inflate(R.layout.layout_available_slots, null);
        builder.setView(view);

        // Find the RadioGroup in the custom layout
        RadioGroup slotRadioGroup = view.findViewById(R.id.slotRadioGroup);

        // Generate the list of available slots and add radio buttons to the RadioGroup
        for (int i = 0; i < availableSlots; i++) {
            String slotText = String.format("%02d:00 - %02d:00", startingHour + i, startingHour + i + 1);
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(slotText);
            radioButton.setTag(startingHour + i); // Set the tag to store the hour value
            slotRadioGroup.addView(radioButton);
        }

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Get the selected radio button from the RadioGroup
                int selectedRadioButtonId = slotRadioGroup.getCheckedRadioButtonId();
                if (selectedRadioButtonId != -1) {
                    // Retrieve the selected hour from the radio button's tag
                    int selectedHour = (int) slotRadioGroup.findViewById(selectedRadioButtonId).getTag();
                    String slotTime = String.format("%02d:00 - %02d:00", selectedHour, selectedHour + 1);

                    // Check if the user already has a slot booked
                    db.collection("Slots")
                            .document(userId)
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document.exists()) {
                                            // The user already has a slot, show the current slot timing and offer to delete
                                            String existingSlotTime = document.getString("name");
                                            AlertDialog.Builder builder = new AlertDialog.Builder(FinalViewActivity.this);
                                            builder.setTitle("Slot Exists");
                                            builder.setMessage("You already have a slot at " + existingSlotTime +
                                                    ". Do you want to delete it ?");
                                            builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    // Call function to delete the existing slot and create a new one
                                                    deleteSlot(userId);
                                                }
                                            });
                                            builder.setNegativeButton("Cancel", null);
                                            builder.show();
                                        } else {
                                            // The user doesn't have a slot, create a new one
                                            createSlot(userId, email, slotTime);
                                        }
                                    } else {
                                        Log.d("checkSlot", "Error getting document: ", task.getException());
                                    }
                                }
                            });
                }
            }

        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    private void showDeleteSlotAlertDialog(String userId, String slotTime) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Slot Exists");
        builder.setMessage("You already have a slot at " + slotTime + ". Do you want to delete it?");
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
    // Function to delete the slot for the logged-in user
    private void deleteSlot(String userId) {
        db.collection("Slots")
                .document(userId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("deleteSlot", "Slot deleted successfully");
                        showAlertMessage("Slot deleted successfully.");
                        Toast.makeText(FinalViewActivity.this, "Slot deleted successfully.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("deleteSlot", "Error deleting slot: ", e);
                        Toast.makeText(FinalViewActivity.this, "Failed to delete slot.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void createSlot(String userId, String email, String slotTime) {
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
                                showDeleteSlotAlertDialog(userId, slotTime);
                            } else {
                                // The user doesn't have a slot, create a new one
                                db.collection("Slots")
                                        .document(userId)
                                        .set(new Slot(slotTime, email, userId), SetOptions.merge())
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d("createSlot", "Slot created successfully");
                                                showAlertMessage("Slot created successfully.");
                                                Toast.makeText(FinalViewActivity.this, "Slot created successfully.", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.d("createSlot", "Error creating slot: ", e);
                                                Toast.makeText(FinalViewActivity.this, "Failed to create slot.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            Log.d("checkSlot", "Error getting document: ", task.getException());
                        }
                    }
                });
    }


    // Function to display alert message
//    private void showAlertMessage(String message) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setMessage(message);
//        builder.setPositiveButton("OK", null);
//        builder.show();
//    }


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


