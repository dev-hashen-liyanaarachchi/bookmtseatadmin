package com.hash.bookmyseatadmin.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.hash.bookmyseatadmin.R;

import java.util.HashMap;
import java.util.Map;

public class BookingDetailsActivity extends AppCompatActivity {

    private TextView tvBookingId, tvMovie, tvSeats, tvAmount, tvStatus, tvUserInfo;
    private Button btnIssueTicket, btnMarkAttendance;
    private ImageView btnBack;
    private FirebaseFirestore db;
    private String bookingId;
    private boolean ticketIssued, attended;
    private String userId, userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_details);

        db = FirebaseFirestore.getInstance();

        // Get data from intent
        bookingId = getIntent().getStringExtra("bookingId");
        String movieTitle = getIntent().getStringExtra("movieTitle");
        String seats = getIntent().getStringExtra("seats");
        double totalAmount = getIntent().getDoubleExtra("totalAmount", 0);
        ticketIssued = getIntent().getBooleanExtra("ticketIssued", false);
        attended = getIntent().getBooleanExtra("attended", false);
        userId = getIntent().getStringExtra("userId");
        userEmail = getIntent().getStringExtra("userEmail");

        initViews();
        displayDetails(bookingId, movieTitle, seats, totalAmount, userId, userEmail);
        updateButtonStates();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvBookingId = findViewById(R.id.tvBookingId);
        tvMovie = findViewById(R.id.tvMovie);
        tvSeats = findViewById(R.id.tvSeats);
        tvAmount = findViewById(R.id.tvAmount);
        tvStatus = findViewById(R.id.tvStatus);
        tvUserInfo = findViewById(R.id.tvUserInfo);
        btnIssueTicket = findViewById(R.id.btnIssueTicket);
        btnMarkAttendance = findViewById(R.id.btnMarkAttendance);

        btnBack.setOnClickListener(v -> finish());
        btnIssueTicket.setOnClickListener(v -> issueTicket());
        btnMarkAttendance.setOnClickListener(v -> markAttendance());
    }

    private void displayDetails(String bookingId, String movieTitle, String seats,
                                double totalAmount, String userId, String userEmail) {
        tvBookingId.setText("Booking ID: " + bookingId);
        tvMovie.setText("Movie: " + movieTitle);
        tvSeats.setText("Seats: " + seats);
        tvAmount.setText("Total: LKR " + totalAmount);
        tvUserInfo.setText("User: " + userEmail + "\nUser ID: " + userId);
    }

    private void updateButtonStates() {
        if (ticketIssued) {
            btnIssueTicket.setEnabled(false);
            btnIssueTicket.setText("✓ Ticket Issued");
        }

        if (attended) {
            btnMarkAttendance.setEnabled(false);
            btnMarkAttendance.setText("✓ Attendance Marked");
        }

        updateStatusText();
    }

    private void updateStatusText() {
        if (attended) {
            tvStatus.setText("Status: ATTENDED");
            tvStatus.setTextColor(0xFF4CAF50);
        } else if (ticketIssued) {
            tvStatus.setText("Status: TICKET ISSUED");
            tvStatus.setTextColor(0xFFFF9800);
        } else {
            tvStatus.setText("Status: PENDING");
            tvStatus.setTextColor(0xFFF44336);
        }
    }

    private void issueTicket() {
        if (ticketIssued) {
            Toast.makeText(this, "Ticket already issued", Toast.LENGTH_SHORT).show();
            return;
        }

        btnIssueTicket.setEnabled(false);
        btnIssueTicket.setText("Processing...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("ticketIssued", true);
        updates.put("ticketIssuedAt", System.currentTimeMillis());

        db.collection("bookings")
                .whereEqualTo("bookingId", bookingId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String docId = task.getResult().getDocuments().get(0).getId();

                        db.collection("bookings").document(docId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    ticketIssued = true;
                                    btnIssueTicket.setText("✓ Ticket Issued");
                                    updateStatusText();
                                    Toast.makeText(BookingDetailsActivity.this,
                                            "Ticket issued successfully",
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    btnIssueTicket.setEnabled(true);
                                    btnIssueTicket.setText("Issue Ticket");
                                    Toast.makeText(BookingDetailsActivity.this,
                                            "Failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }

    private void markAttendance() {
        if (attended) {
            Toast.makeText(this, "Attendance already marked", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ticketIssued) {
            Toast.makeText(this, "Please issue ticket first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnMarkAttendance.setEnabled(false);
        btnMarkAttendance.setText("Processing...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("attended", true);
        updates.put("attendedAt", System.currentTimeMillis());

        db.collection("bookings")
                .whereEqualTo("bookingId", bookingId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String docId = task.getResult().getDocuments().get(0).getId();

                        db.collection("bookings").document(docId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    attended = true;
                                    btnMarkAttendance.setText("✓ Attendance Marked");
                                    updateStatusText();
                                    Toast.makeText(BookingDetailsActivity.this,
                                            "Attendance marked successfully",
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    btnMarkAttendance.setEnabled(true);
                                    btnMarkAttendance.setText("Mark Attendance");
                                    Toast.makeText(BookingDetailsActivity.this,
                                            "Failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }
}