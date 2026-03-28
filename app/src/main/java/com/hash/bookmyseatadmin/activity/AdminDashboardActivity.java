package com.hash.bookmyseatadmin.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;
import com.hash.bookmyseatadmin.R;
import com.hash.bookmyseatadmin.adapter.BookingsAdapter;
import com.hash.bookmyseatadmin.model.AdminBooking;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalBookings, tvTodayBookings, tvTotalIssued, tvTotalAttended;
    private RecyclerView rvRecentBookings;
    private MaterialButton btnScanQR, btnAttendance, btnMyEvents, btnLogout;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private BookingsAdapter adapter;
    private List<AdminBooking> bookingList = new ArrayList<>();
    private String currentAdminUid;
    private boolean isSuperAdmin = false;
    private static final String TAG = "AdminDashboard";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentAdminUid = mAuth.getCurrentUser().getUid();
            Log.d(TAG, "Current Admin UID: " + currentAdminUid);
        } else {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        checkAdminRole();
    }

    private void initViews() {
        tvTotalBookings = findViewById(R.id.tvTotalBookings);
        tvTodayBookings = findViewById(R.id.tvTodayBookings);
        tvTotalIssued = findViewById(R.id.tvTotalIssued);
        tvTotalAttended = findViewById(R.id.tvTotalAttended);
        rvRecentBookings = findViewById(R.id.rvRecentBookings);
        btnScanQR = findViewById(R.id.btnScanQR);
        btnAttendance = findViewById(R.id.btnAttendance);
        btnMyEvents = findViewById(R.id.btnMyEvents);
        btnLogout = findViewById(R.id.btnLogout);

        rvRecentBookings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingsAdapter(bookingList, booking -> {
            Intent intent = new Intent(AdminDashboardActivity.this, BookingDetailsActivity.class);
            intent.putExtra("bookingId", booking.getBookingId());
            intent.putExtra("movieTitle", booking.getMovieTitle());
            intent.putExtra("seats", booking.getSeats());
            intent.putExtra("totalAmount", booking.getTotalAmount());
            intent.putExtra("ticketIssued", booking.isTicketIssued());
            intent.putExtra("attended", booking.isAttended());
            intent.putExtra("userId", booking.getUserId());
            intent.putExtra("userEmail", booking.getUserEmail());
            startActivity(intent);
        });
        rvRecentBookings.setAdapter(adapter);

        btnScanQR.setOnClickListener(v -> startActivity(new Intent(this, QRScanActivity.class)));
        btnAttendance.setOnClickListener(v -> startActivity(new Intent(this, AttendanceActivity.class)));
        btnMyEvents.setOnClickListener(v -> startActivity(new Intent(this, MyEventsActivity.class)));
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, AdminLoginActivity.class));
            finish();
        });
    }

    private void checkAdminRole() {
        // Check if user is super admin by email or role
        String email = mAuth.getCurrentUser().getEmail();
        Log.d(TAG, "Admin Email: " + email);

        // Super admin emails list
        List<String> superAdminEmails = new ArrayList<>();
        superAdminEmails.add("admin@bookmyseat.com");
        superAdminEmails.add("superadmin@bookmyseat.com");

        if (superAdminEmails.contains(email)) {
            isSuperAdmin = true;
            Log.d(TAG, "User is SUPER ADMIN by email: " + email);
            loadDashboardStats();
            loadRecentBookings();
            return;
        }

        // Also check Firestore admins collection
        db.collection("admins")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String role = task.getResult().getDocuments().get(0).getString("role");
                        isSuperAdmin = "super_admin".equals(role);
                        Log.d(TAG, "Admin Role from Firestore: " + role + ", isSuperAdmin: " + isSuperAdmin);
                    } else {
                        Log.d(TAG, "Admin not found in Firestore, treating as regular admin");
                    }
                    loadDashboardStats();
                    loadRecentBookings();
                });
    }

    private String formatBookingDate(Object dateObj) {
        if (dateObj == null) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        }

        if (dateObj instanceof Timestamp) {
            Timestamp timestamp = (Timestamp) dateObj;
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(timestamp.toDate());
        } else if (dateObj instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format((Date) dateObj);
        } else if (dateObj instanceof String) {
            return (String) dateObj;
        } else {
            return String.valueOf(dateObj);
        }
    }

    private void loadDashboardStats() {
        Log.d(TAG, "Loading stats, isSuperAdmin: " + isSuperAdmin);

        if (isSuperAdmin) {
            // SUPER ADMIN - see ALL bookings
            Log.d(TAG, "SUPER ADMIN - Loading ALL bookings");

            db.collection("bookings")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            int total = task.getResult().size();
                            tvTotalBookings.setText(String.valueOf(total));
                            Log.d(TAG, "Total Bookings: " + total);
                        }
                    });

            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            db.collection("bookings")
                    .whereGreaterThanOrEqualTo("bookingDate", today + " 00:00:00")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            tvTodayBookings.setText(String.valueOf(task.getResult().size()));
                        }
                    });

            db.collection("bookings")
                    .whereEqualTo("ticketIssued", true)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            tvTotalIssued.setText(String.valueOf(task.getResult().size()));
                        }
                    });

            db.collection("bookings")
                    .whereEqualTo("attended", true)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            tvTotalAttended.setText(String.valueOf(task.getResult().size()));
                        }
                    });

        } else {
            // REGULAR ADMIN - see only bookings from their events
            Log.d(TAG, "REGULAR ADMIN - Loading bookings from their events");

            db.collection("events")
                    .whereEqualTo("createdBy", currentAdminUid)
                    .get()
                    .addOnCompleteListener(eventsTask -> {
                        if (eventsTask.isSuccessful()) {
                            List<String> eventIds = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : eventsTask.getResult()) {
                                eventIds.add(doc.getString("eventId"));
                            }
                            Log.d(TAG, "Admin event IDs: " + eventIds);

                            if (eventIds.isEmpty()) {
                                tvTotalBookings.setText("0");
                                tvTodayBookings.setText("0");
                                tvTotalIssued.setText("0");
                                tvTotalAttended.setText("0");
                                return;
                            }

                            db.collection("bookings")
                                    .whereIn("eventId", eventIds)
                                    .get()
                                    .addOnCompleteListener(bookingsTask -> {
                                        if (bookingsTask.isSuccessful()) {
                                            int total = bookingsTask.getResult().size();
                                            tvTotalBookings.setText(String.valueOf(total));

                                            int issued = 0;
                                            int attended = 0;
                                            int todayCount = 0;
                                            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                                            for (QueryDocumentSnapshot doc : bookingsTask.getResult()) {
                                                if (doc.getBoolean("ticketIssued") != null && doc.getBoolean("ticketIssued")) {
                                                    issued++;
                                                }
                                                if (doc.getBoolean("attended") != null && doc.getBoolean("attended")) {
                                                    attended++;
                                                }

                                                Object bookingDateObj = doc.get("bookingDate");
                                                String bookingDateStr = formatBookingDate(bookingDateObj);
                                                if (bookingDateStr.startsWith(today)) {
                                                    todayCount++;
                                                }
                                            }
                                            tvTodayBookings.setText(String.valueOf(todayCount));
                                            tvTotalIssued.setText(String.valueOf(issued));
                                            tvTotalAttended.setText(String.valueOf(attended));
                                        }
                                    });
                        }
                    });
        }
    }

    private void loadRecentBookings() {
        Log.d(TAG, "Loading recent bookings, isSuperAdmin: " + isSuperAdmin);

        if (isSuperAdmin) {
            // SUPER ADMIN - see ALL recent bookings
            Log.d(TAG, "SUPER ADMIN - Loading ALL recent bookings");

            db.collection("bookings")
                    .orderBy("bookingDate", Query.Direction.DESCENDING)
                    .limit(20)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            bookingList.clear();
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                String bookingDateStr = formatBookingDate(doc.get("bookingDate"));

                                AdminBooking booking = new AdminBooking(
                                        doc.getString("bookingId"),
                                        doc.getString("movieTitle"),
                                        doc.getString("seats"),
                                        doc.getDouble("totalAmount") != null ? doc.getDouble("totalAmount") : 0.0,
                                        doc.getBoolean("ticketIssued") != null ? doc.getBoolean("ticketIssued") : false,
                                        doc.getBoolean("attended") != null ? doc.getBoolean("attended") : false,
                                        bookingDateStr,
                                        doc.getString("userId"),
                                        doc.getString("userEmail")
                                );
                                bookingList.add(booking);
                            }
                            adapter.notifyDataSetChanged();
                            Log.d(TAG, "Loaded " + bookingList.size() + " recent bookings for SUPER ADMIN");
                        }
                    });
        } else {
            // REGULAR ADMIN - see only bookings from their events
            Log.d(TAG, "REGULAR ADMIN - Loading bookings from their events");

            db.collection("events")
                    .whereEqualTo("createdBy", currentAdminUid)
                    .get()
                    .addOnCompleteListener(eventsTask -> {
                        if (eventsTask.isSuccessful()) {
                            List<String> eventIds = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : eventsTask.getResult()) {
                                eventIds.add(doc.getString("eventId"));
                            }

                            if (eventIds.isEmpty()) {
                                bookingList.clear();
                                adapter.notifyDataSetChanged();
                                return;
                            }

                            db.collection("bookings")
                                    .whereIn("eventId", eventIds)
                                    .orderBy("bookingDate", Query.Direction.DESCENDING)
                                    .limit(20)
                                    .get()
                                    .addOnCompleteListener(bookingsTask -> {
                                        if (bookingsTask.isSuccessful()) {
                                            bookingList.clear();
                                            for (QueryDocumentSnapshot doc : bookingsTask.getResult()) {
                                                String bookingDateStr = formatBookingDate(doc.get("bookingDate"));

                                                AdminBooking booking = new AdminBooking(
                                                        doc.getString("bookingId"),
                                                        doc.getString("movieTitle"),
                                                        doc.getString("seats"),
                                                        doc.getDouble("totalAmount") != null ? doc.getDouble("totalAmount") : 0.0,
                                                        doc.getBoolean("ticketIssued") != null ? doc.getBoolean("ticketIssued") : false,
                                                        doc.getBoolean("attended") != null ? doc.getBoolean("attended") : false,
                                                        bookingDateStr,
                                                        doc.getString("userId"),
                                                        doc.getString("userEmail")
                                                );
                                                bookingList.add(booking);
                                            }
                                            adapter.notifyDataSetChanged();
                                            Log.d(TAG, "Loaded " + bookingList.size() + " bookings for regular admin");
                                        }
                                    });
                        }
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardStats();
        loadRecentBookings();
    }
}