package com.hash.bookmyseatadmin.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.hash.bookmyseatadmin.R;

public class QRScanActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scan_simple);

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnScan).setOnClickListener(v -> checkCameraPermission());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startQRScanner();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQRScanner();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startQRScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan QR Code");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            } else {
                String qrData = result.getContents();
                processQRData(qrData);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void processQRData(String qrData) {
        String bookingId = extractBookingId(qrData);

        if (bookingId != null) {
            db.collection("bookings")
                    .whereEqualTo("bookingId", bookingId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            var doc = task.getResult().getDocuments().get(0);

                            Intent intent = new Intent(QRScanActivity.this, BookingDetailsActivity.class);
                            intent.putExtra("bookingId", doc.getString("bookingId"));
                            intent.putExtra("movieTitle", doc.getString("movieTitle"));
                            intent.putExtra("seats", doc.getString("seats"));
                            intent.putExtra("totalAmount", doc.getDouble("totalAmount") != null ?
                                    doc.getDouble("totalAmount") : 0.0);
                            intent.putExtra("ticketIssued", doc.getBoolean("ticketIssued") != null ?
                                    doc.getBoolean("ticketIssued") : false);
                            intent.putExtra("attended", doc.getBoolean("attended") != null ?
                                    doc.getBoolean("attended") : false);
                            intent.putExtra("userId", doc.getString("userId"));
                            intent.putExtra("userEmail", doc.getString("userEmail"));
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "Invalid QR Code: Booking not found",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            Toast.makeText(this, "Invalid QR Code format", Toast.LENGTH_SHORT).show();
        }
    }

    private String extractBookingId(String qrData) {
        try {
            String[] lines = qrData.split("\n");
            for (String line : lines) {
                if (line.startsWith("Booking ID:")) {
                    return line.replace("Booking ID:", "").trim();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}