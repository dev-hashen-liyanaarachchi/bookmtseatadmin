package com.hash.bookmyseatadmin.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.hash.bookmyseatadmin.R;
import com.hash.bookmyseatadmin.adapter.EventsAdapter;
import com.hash.bookmyseatadmin.model.Event;

import java.util.ArrayList;
import java.util.List;

public class MyEventsActivity extends AppCompatActivity {

    private RecyclerView rvEvents;
    private EventsAdapter adapter;
    private List<Event> eventList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ImageView btnBack;
    private MaterialButton btnCreateEvent;
    private String currentAdminUid;
    private boolean isSuperAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_events);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentAdminUid = mAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        checkAdminRoleAndLoadEvents();
    }

    private void initViews() {
        rvEvents = findViewById(R.id.rvEvents);
        btnBack = findViewById(R.id.btnBack);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);

        btnBack.setOnClickListener(v -> finish());

        btnCreateEvent.setOnClickListener(v -> {
            startActivity(new Intent(MyEventsActivity.this, CreateEventActivity.class));
        });

        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventsAdapter(eventList, event -> {
            Toast.makeText(this, "Event: " + event.getTitle(), Toast.LENGTH_SHORT).show();
        });
        rvEvents.setAdapter(adapter);
    }

    private void checkAdminRoleAndLoadEvents() {
        // Check if super admin by email
        String email = mAuth.getCurrentUser().getEmail();

        List<String> superAdminEmails = new ArrayList<>();
        superAdminEmails.add("admin@bookmyseat.com");
        superAdminEmails.add("superadmin@bookmyseat.com");

        if (superAdminEmails.contains(email)) {
            isSuperAdmin = true;
            loadAllEvents();  // Super Admin sees ALL events
            return;
        }

        // Check Firestore for role
        db.collection("admins")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String role = task.getResult().getDocuments().get(0).getString("role");
                        isSuperAdmin = "super_admin".equals(role);
                    }

                    if (isSuperAdmin) {
                        loadAllEvents();
                    } else {
                        loadMyEvents();
                    }
                });
    }

    private void loadAllEvents() {
        // Super Admin - load ALL events
        db.collection("events")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        eventList.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Event event = doc.toObject(Event.class);
                            eventList.add(event);
                        }
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "Total Events: " + eventList.size(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadMyEvents() {
        // Regular Admin - load only their events
        db.collection("events")
                .whereEqualTo("createdBy", currentAdminUid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        eventList.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Event event = doc.toObject(Event.class);
                            eventList.add(event);
                        }
                        adapter.notifyDataSetChanged();

                        if (eventList.isEmpty()) {
                            Toast.makeText(this, "No events found. Create your first event!",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}