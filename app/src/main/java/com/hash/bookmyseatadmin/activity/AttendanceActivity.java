package com.hash.bookmyseatadmin.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.hash.bookmyseatadmin.R;
import com.hash.bookmyseatadmin.adapter.BookingsAdapter;
import com.hash.bookmyseatadmin.model.AdminBooking;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceActivity extends AppCompatActivity {

    private TextView tvDate, tvTotalAttended, tvTotalBookings;
    private RecyclerView rvAttendances;
    private ImageView btnBack;
    private FirebaseFirestore db;
    private List<AdminBooking> attendanceList = new ArrayList<>();
    private BookingsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        db = FirebaseFirestore.getInstance();
        initViews();
        loadAttendanceData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvDate = findViewById(R.id.tvDate);
        tvTotalAttended = findViewById(R.id.tvTotalAttended);
        tvTotalBookings = findViewById(R.id.tvTotalBookings);
        rvAttendances = findViewById(R.id.rvAttendances);

        btnBack.setOnClickListener(v -> finish());

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvDate.setText("Date: " + today);

        rvAttendances.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingsAdapter(attendanceList, booking -> {
            // Optional: navigate to details
            Intent intent = new Intent(AttendanceActivity.this, BookingDetailsActivity.class);
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
        rvAttendances.setAdapter(adapter);
    }

    private void loadAttendanceData() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        db.collection("bookings")
                .whereGreaterThanOrEqualTo("bookingDate", today + " 00:00:00")
                .whereLessThanOrEqualTo("bookingDate", today + " 23:59:59")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        attendanceList.clear();
                        int attended = 0;

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            boolean isAttended = doc.getBoolean("attended") != null ?
                                    doc.getBoolean("attended") : false;

                            if (isAttended) attended++;

                            AdminBooking booking = new AdminBooking(
                                    doc.getString("bookingId"),
                                    doc.getString("movieTitle"),
                                    doc.getString("seats"),
                                    doc.getDouble("totalAmount") != null ? doc.getDouble("totalAmount") : 0.0,
                                    doc.getBoolean("ticketIssued") != null ? doc.getBoolean("ticketIssued") : false,
                                    isAttended,
                                    doc.getString("bookingDate"),
                                    doc.getString("userId"),
                                    doc.getString("userEmail")
                            );
                            attendanceList.add(booking);
                        }

                        tvTotalBookings.setText("Total Bookings: " + attendanceList.size());
                        tvTotalAttended.setText("Attended: " + attended);
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}