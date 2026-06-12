package com.hash.bookmyseatadmin.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hash.bookmyseatadmin.R;
import com.hash.bookmyseatadmin.model.Event;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class EditEventActivity extends AppCompatActivity {

    // ========== UI COMPONENTS ==========
    private TextInputLayout layoutTitle, layoutDescription, layoutMovieTitle,
            layoutVenue, layoutPricePerSeat, layoutTotalSeats;
    private TextInputLayout layoutDate, layoutTime, layoutLocation, layoutContactNumber;
    private Spinner spinnerStatus;
    private MaterialButton btnUpdateEvent, btnSelectImage, btnSelectDate, btnSelectTime, btnSelectLocation;
    private ImageView ivEventPoster, btnBack;

    // ========== FIREBASE ==========
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentAdminUid;
    private String eventId;

    // ========== DATA VARIABLES ==========
    private String selectedImageBase64 = "";
    private String selectedDate = "";
    private String selectedTime = "";
    private String selectedLocation = "";
    private boolean isImageChanged = false;
    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int LOCATION_PICK_REQUEST = 200;

    private Event originalEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Check admin login
        if (mAuth.getCurrentUser() != null) {
            currentAdminUid = mAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get event ID from intent
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) {
            Toast.makeText(this, "Event ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setClickListeners();
        setupStatusSpinner();
        loadEventData();  // Load existing event data
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
        layoutContactNumber = findViewById(R.id.layoutContactNumber);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        btnUpdateEvent = findViewById(R.id.btnUpdateEvent);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSelectTime = findViewById(R.id.btnSelectTime);
        btnSelectLocation = findViewById(R.id.btnSelectLocation);
        ivEventPoster = findViewById(R.id.ivEventPoster);
        btnBack = findViewById(R.id.btnBack);

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Make date/time/location fields use pickers
        layoutDate.getEditText().setFocusable(false);
        layoutDate.getEditText().setClickable(false);
        layoutTime.getEditText().setFocusable(false);
        layoutTime.getEditText().setClickable(false);
        layoutLocation.getEditText().setFocusable(false);
        layoutLocation.getEditText().setClickable(false);
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
        btnUpdateEvent.setOnClickListener(v -> updateEvent());
    }

    private void loadEventData() {
        // Show loading state
        btnUpdateEvent.setEnabled(false);
        btnUpdateEvent.setText("Loading...");

        // Fetch event from Firestore
        db.collection("events").document(eventId)
                .get()
                .addOnCompleteListener(task -> {
                    btnUpdateEvent.setEnabled(true);
                    btnUpdateEvent.setText("Update Event");

                    if (task.isSuccessful() && task.getResult() != null) {
                        originalEvent = task.getResult().toObject(Event.class);

                        if (originalEvent != null) {
                            // Populate all form fields with existing data
                            layoutTitle.getEditText().setText(originalEvent.getTitle());
                            layoutDescription.getEditText().setText(originalEvent.getDescription());
                            layoutMovieTitle.getEditText().setText(originalEvent.getMovieTitle());
                            layoutVenue.getEditText().setText(originalEvent.getVenue());
                            layoutPricePerSeat.getEditText().setText(String.valueOf(originalEvent.getPricePerSeat()));
                            layoutTotalSeats.getEditText().setText(String.valueOf(originalEvent.getTotalSeats()));
                            layoutContactNumber.getEditText().setText(originalEvent.getContactNumber());

                            // Set date, time, location
                            selectedDate = originalEvent.getDate();
                            selectedTime = originalEvent.getTime();
                            selectedLocation = originalEvent.getLocation();
                            selectedImageBase64 = originalEvent.getPosterBase64();

                            layoutDate.getEditText().setText(selectedDate);
                            layoutTime.getEditText().setText(selectedTime);
                            layoutLocation.getEditText().setText(selectedLocation);

                            // Set status spinner
                            String status = originalEvent.getStatus();
                            if ("upcoming".equals(status)) {
                                spinnerStatus.setSelection(0);
                            } else if ("coming_soon".equals(status)) {
                                spinnerStatus.setSelection(1);
                            }

                            // Load existing poster image
                            if (selectedImageBase64 != null && !selectedImageBase64.isEmpty()) {
                                try {
                                    byte[] decodedString = Base64.decode(selectedImageBase64, Base64.DEFAULT);
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                    ivEventPoster.setImageBitmap(bitmap);
                                    ivEventPoster.setVisibility(View.VISIBLE);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this, "Failed to load event data", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void showDatePicker() {
        // Parse current date or use today
        int year, month, day;
        try {
            String[] dateParts = selectedDate.split("-");
            year = Integer.parseInt(dateParts[0]);
            month = Integer.parseInt(dateParts[1]) - 1;
            day = Integer.parseInt(dateParts[2]);
        } catch (Exception e) {
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            year = calendar.get(java.util.Calendar.YEAR);
            month = calendar.get(java.util.Calendar.MONTH);
            day = calendar.get(java.util.Calendar.DAY_OF_MONTH);
        }

        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(this,
                (view, year1, month1, dayOfMonth) -> {
                    selectedDate = year1 + "-" + (month1 + 1) + "-" + dayOfMonth;
                    layoutDate.getEditText().setText(selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        // Parse current time or use current time
        int hour, minute;
        try {
            String[] timeParts = selectedTime.split(":");
            hour = Integer.parseInt(timeParts[0]);
            minute = Integer.parseInt(timeParts[1]);
        } catch (Exception e) {
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
            minute = calendar.get(java.util.Calendar.MINUTE);
        }

        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    selectedTime = String.format("%02d:%02d", hourOfDay, minute1);
                    layoutTime.getEditText().setText(selectedTime);
                }, hour, minute, true);
        timePickerDialog.show();
    }

    private void pickLocation() {
        Intent intent = new Intent(EditEventActivity.this, LocationPickerActivity.class);
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
                    ivEventPoster.setVisibility(View.VISIBLE);
                    isImageChanged = true;
                    Toast.makeText(this, "Image selected!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Image too large! Max 500KB", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == LOCATION_PICK_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedLocation = data.getStringExtra("location");
            layoutLocation.getEditText().setText(selectedLocation);
            Toast.makeText(this, "Location selected", Toast.LENGTH_SHORT).show();
        }
    }

    private String convertImageToBase64(Uri imageUri) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

        // Resize image if too large
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

    private void updateEvent() {
        // Get values from form
        String title = layoutTitle.getEditText().getText().toString().trim();
        String description = layoutDescription.getEditText().getText().toString().trim();
        String movieTitle = layoutMovieTitle.getEditText().getText().toString().trim();
        String venue = layoutVenue.getEditText().getText().toString().trim();
        String priceStr = layoutPricePerSeat.getEditText().getText().toString().trim();
        String seatsStr = layoutTotalSeats.getEditText().getText().toString().trim();
        String status = spinnerStatus.getSelectedItem().toString();
        String contactNumber = layoutContactNumber.getEditText().getText().toString().trim();

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
        if (TextUtils.isEmpty(contactNumber)) {
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

        // Prepare updates map
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", description);
        updates.put("movieTitle", movieTitle);
        updates.put("venue", venue);
        updates.put("date", selectedDate);
        updates.put("time", selectedTime);
        updates.put("location", selectedLocation);
        updates.put("contactNumber", contactNumber);
        updates.put("pricePerSeat", price);
        updates.put("totalSeats", totalSeats);
        updates.put("status", status);
        updates.put("updatedAt", new Date());

        // Only update image if user selected a new one
        if (isImageChanged && !selectedImageBase64.isEmpty()) {
            updates.put("posterBase64", selectedImageBase64);
        }

        // Disable button during update
        btnUpdateEvent.setEnabled(false);
        btnUpdateEvent.setText("Updating...");

        // Update in Firestore
        db.collection("events").document(eventId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditEventActivity.this, "Event updated successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(EditEventActivity.this, MyEventsActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnUpdateEvent.setEnabled(true);
                    btnUpdateEvent.setText("Update Event");
                    Toast.makeText(EditEventActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}