package com.hash.bookmyseatadmin.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hash.bookmyseatadmin.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CreateEventActivity extends AppCompatActivity {

    private TextInputLayout layoutTitle, layoutDescription, layoutMovieTitle,
            layoutVenue, layoutPricePerSeat, layoutTotalSeats;
    private TextInputLayout layoutDate, layoutTime, layoutLocation, layoutContactNumber;  // ← ADDED
    private android.widget.Spinner spinnerStatus;
    private MaterialButton btnCreateEvent, btnSelectImage, btnSelectDate, btnSelectTime, btnSelectLocation;
    private ImageView ivEventPoster;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentAdminUid;
    private String selectedImageBase64 = "";
    private String selectedDate = "";
    private String selectedTime = "";
    private String selectedLocation = "";
    private String selectedContactNumber = "";  // ← ADDED
    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int LOCATION_PICK_REQUEST = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

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
        setClickListeners();
        setupStatusSpinner();
    }

    private void initViews() {
        layoutTitle = findViewById(R.id.layoutTitle);
        layoutDescription = findViewById(R.id.layoutDescription);
        layoutMovieTitle = findViewById(R.id.layoutMovieTitle);
        layoutVenue = findViewById(R.id.layoutVenue);
        layoutPricePerSeat = findViewById(R.id.layoutPricePerSeat);
        layoutTotalSeats = findViewById(R.id.layoutTotalSeats);
        layoutDate = findViewById(R.id.layoutDate);
        layoutTime = findViewById(R.id.layoutTime);
        layoutLocation = findViewById(R.id.layoutLocation);
        layoutContactNumber = findViewById(R.id.layoutContactNumber);  // ← ADDED
        spinnerStatus = findViewById(R.id.spinnerStatus);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSelectTime = findViewById(R.id.btnSelectTime);
        btnSelectLocation = findViewById(R.id.btnSelectLocation);
        ivEventPoster = findViewById(R.id.ivEventPoster);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        layoutDate.getEditText().setFocusable(false);
        layoutDate.getEditText().setClickable(false);
        layoutTime.getEditText().setFocusable(false);
        layoutTime.getEditText().setClickable(false);
        layoutLocation.getEditText().setFocusable(false);
        layoutLocation.getEditText().setClickable(false);
        layoutContactNumber.getEditText().setFocusable(true);  // ← Contact number should be editable
    }

    private void setupStatusSpinner() {
        String[] statuses = {"upcoming", "coming_soon"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);
    }

    private void setClickListeners() {
        btnSelectImage.setOnClickListener(v -> selectImage());
        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnSelectTime.setOnClickListener(v -> showTimePicker());
        btnSelectLocation.setOnClickListener(v -> pickLocation());
        btnCreateEvent.setOnClickListener(v -> createEvent());
    }

    private void showDatePicker() {
        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
                    layoutDate.getEditText().setText(selectedDate);
                },
                java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                java.util.Calendar.getInstance().get(java.util.Calendar.MONTH),
                java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void showTimePicker() {
        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    selectedTime = String.format("%02d:%02d", hourOfDay, minute);
                    layoutTime.getEditText().setText(selectedTime);
                },
                java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
                java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE), true);
        timePickerDialog.show();
    }

    private void pickLocation() {
        Intent intent = new Intent(CreateEventActivity.this, LocationPickerActivity.class);
        startActivityForResult(intent, LOCATION_PICK_REQUEST);
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                selectedImageBase64 = convertImageToBase64(imageUri);
                if (!selectedImageBase64.isEmpty()) {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    ivEventPoster.setImageBitmap(bitmap);
                    ivEventPoster.setVisibility(android.view.View.VISIBLE);
                    Toast.makeText(this, "Image selected!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Image too large!", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == LOCATION_PICK_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedLocation = data.getStringExtra("location");
            layoutLocation.getEditText().setText(selectedLocation);
            Toast.makeText(this, "Location selected: " + selectedLocation, Toast.LENGTH_SHORT).show();
        }
    }

    private String convertImageToBase64(Uri imageUri) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

        int maxSize = 800;
        if (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize) {
            float ratio = Math.min((float) maxSize / bitmap.getWidth(), (float) maxSize / bitmap.getHeight());
            int newWidth = Math.round(bitmap.getWidth() * ratio);
            int newHeight = Math.round(bitmap.getHeight() * ratio);
            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] imageBytes = baos.toByteArray();

        if (imageBytes.length > 500 * 1024) {
            return "";
        }

        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    private void createEvent() {
        String title = layoutTitle.getEditText().getText().toString().trim();
        String description = layoutDescription.getEditText().getText().toString().trim();
        String movieTitle = layoutMovieTitle.getEditText().getText().toString().trim();
        String venue = layoutVenue.getEditText().getText().toString().trim();
        String priceStr = layoutPricePerSeat.getEditText().getText().toString().trim();
        String seatsStr = layoutTotalSeats.getEditText().getText().toString().trim();
        String status = spinnerStatus.getSelectedItem().toString();
        String contactNumber = layoutContactNumber.getEditText().getText().toString().trim();  // ← ADDED

        // Validation
        if (TextUtils.isEmpty(title)) {
            layoutTitle.setError("Title required");
            return;
        }
        if (TextUtils.isEmpty(description)) {
            layoutDescription.setError("Description required");
            return;
        }
        if (TextUtils.isEmpty(movieTitle)) {
            layoutMovieTitle.setError("Movie title required");
            return;
        }
        if (TextUtils.isEmpty(selectedDate)) {
            layoutDate.setError("Date required");
            return;
        }
        if (TextUtils.isEmpty(selectedTime)) {
            layoutTime.setError("Time required");
            return;
        }
        if (TextUtils.isEmpty(venue)) {
            layoutVenue.setError("Venue name required");
            return;
        }
        if (TextUtils.isEmpty(selectedLocation)) {
            layoutLocation.setError("Location required");
            return;
        }
        if (TextUtils.isEmpty(contactNumber)) {  // ← ADDED VALIDATION
            layoutContactNumber.setError("Contact number required");
            return;
        }
        if (TextUtils.isEmpty(priceStr)) {
            layoutPricePerSeat.setError("Price required");
            return;
        }
        if (TextUtils.isEmpty(seatsStr)) {
            layoutTotalSeats.setError("Total seats required");
            return;
        }

        double price = Double.parseDouble(priceStr);
        int totalSeats = Integer.parseInt(seatsStr);
        String eventId = "EVT-" + System.currentTimeMillis();

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventId", eventId);
        eventData.put("title", title);
        eventData.put("description", description);
        eventData.put("movieTitle", movieTitle);
        eventData.put("date", selectedDate);
        eventData.put("time", selectedTime);
        eventData.put("venue", venue);
        eventData.put("location", selectedLocation);
        eventData.put("contactNumber", contactNumber);  // ← ADDED
        eventData.put("pricePerSeat", price);
        eventData.put("totalSeats", totalSeats);
        eventData.put("createdBy", currentAdminUid);
        eventData.put("createdAt", new Date());
        eventData.put("status", status);
        eventData.put("posterBase64", selectedImageBase64);

        btnCreateEvent.setEnabled(false);
        btnCreateEvent.setText("Creating Event...");

        db.collection("events").document(eventId).set(eventData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event created successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(CreateEventActivity.this, AdminDashboardActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnCreateEvent.setEnabled(true);
                    btnCreateEvent.setText("Create Event");
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}