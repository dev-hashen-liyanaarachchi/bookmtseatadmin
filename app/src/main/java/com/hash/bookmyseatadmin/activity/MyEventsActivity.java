package com.hash.bookmyseatadmin.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
    private ProgressBar progressBar;
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
        progressBar = findViewById(R.id.progressBar);

        btnBack.setOnClickListener(v -> finish());

        btnCreateEvent.setOnClickListener(v -> {
            startActivity(new Intent(MyEventsActivity.this, CreateEventActivity.class));
        });

        rvEvents.setLayoutManager(new LinearLayoutManager(this));

        // ✅ Updated adapter with all three click handlers
        adapter = new EventsAdapter(eventList, new EventsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Event event) {
                // View event details (optional)
                Toast.makeText(MyEventsActivity.this, "Event: " + event.getTitle(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onEditClick(Event event) {
                // Navigate to Edit Event screen
                Intent intent = new Intent(MyEventsActivity.this, EditEventActivity.class);
                intent.putExtra("eventId", event.getEventId());
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(Event event) {
                // Show delete confirmation
                showDeleteConfirmation(event);
            }
        });
        rvEvents.setAdapter(adapter);
    }

    private void showDeleteConfirmation(Event event) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete \"" + event.getTitle() + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent(event))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteEvent(Event event) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        db.collection("events").document(event.getEventId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event deleted successfully", Toast.LENGTH_SHORT).show();
                    checkAdminRoleAndLoadEvents(); // Refresh list
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkAdminRoleAndLoadEvents() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        String email = mAuth.getCurrentUser().getEmail();

        List<String> superAdminEmails = new ArrayList<>();
        superAdminEmails.add("admin@bookmyseat.com");
        superAdminEmails.add("superadmin@bookmyseat.com");

        if (superAdminEmails.contains(email)) {
            isSuperAdmin = true;
            loadAllEvents();
            return;
        }

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
        db.collection("events")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);

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
        db.collection("events")
                .whereEqualTo("createdBy", currentAdminUid)
                .get()
                .addOnCompleteListener(task -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);

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

    @Override
    protected void onResume() {
        super.onResume();
        checkAdminRoleAndLoadEvents();
    }
}