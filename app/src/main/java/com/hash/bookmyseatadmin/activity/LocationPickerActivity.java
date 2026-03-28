package com.hash.bookmyseatadmin.activity;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.hash.bookmyseatadmin.R;
import com.hash.bookmyseatadmin.adapter.PlacesAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SearchView searchView;
    private RecyclerView rvPlaces;
    private PlacesAdapter placesAdapter;
    private List<String> placesList = new ArrayList<>();
    private Marker currentMarker;
    private String selectedAddress = "";
    private double selectedLat = 6.9271;
    private double selectedLng = 79.8612;
    private boolean isMapReady = false;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // Sri Lanka bounds
    private static final LatLngBounds SRI_LANKA_BOUNDS = new LatLngBounds(
            new LatLng(5.0, 79.0),   // Southwest (Southern point)
            new LatLng(10.0, 82.0)   // Northeast (Northern point)
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        initViews();
        setupSearch();
        setupRecyclerView();

        mainHandler.postDelayed(() -> {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            } else {
                Toast.makeText(this, "Map fragment not found", Toast.LENGTH_SHORT).show();
            }
        }, 500);
    }

    private void initViews() {
        searchView = findViewById(R.id.searchView);
        rvPlaces = findViewById(R.id.rvPlaces);

        findViewById(R.id.btnConfirm).setOnClickListener(v -> confirmLocation());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchLocation(query);
                rvPlaces.setVisibility(android.view.View.GONE);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() > 2) {
                    searchSriLankaPlaces(newText);
                } else {
                    rvPlaces.setVisibility(android.view.View.GONE);
                }
                return true;
            }
        });
    }

    private void setupRecyclerView() {
        rvPlaces.setLayoutManager(new LinearLayoutManager(this));
        placesAdapter = new PlacesAdapter(placesList, place -> {
            searchView.setQuery(place, false);
            searchLocation(place);
            rvPlaces.setVisibility(android.view.View.GONE);
        });
        rvPlaces.setAdapter(placesAdapter);
    }

    private void searchSriLankaPlaces(String query) {
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                // 🔥 CRITICAL: Add "Sri Lanka" to restrict results
                List<Address> addresses = geocoder.getFromLocationName(query + ", Sri Lanka", 10);
                List<String> results = new ArrayList<>();
                for (Address address : addresses) {
                    String fullAddress = address.getAddressLine(0);
                    // Filter to ensure it's in Sri Lanka
                    if (fullAddress.contains("Sri Lanka") || fullAddress.contains("Sri Lanka")) {
                        results.add(fullAddress);
                    }
                }
                runOnUiThread(() -> {
                    placesList.clear();
                    placesList.addAll(results);
                    placesAdapter.notifyDataSetChanged();
                    rvPlaces.setVisibility(placesList.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);

                    if (placesList.isEmpty()) {
                        Toast.makeText(this, "No places found in Sri Lanka", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void searchLocation(String locationName) {
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                // 🔥 CRITICAL: Add "Sri Lanka" to restrict results
                List<Address> addresses = geocoder.getFromLocationName(locationName + ", Sri Lanka", 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    double lat = address.getLatitude();
                    double lng = address.getLongitude();
                    String addr = address.getAddressLine(0);

                    // Check if location is within Sri Lanka
                    if (isInSriLanka(lat, lng)) {
                        runOnUiThread(() -> updateMapLocation(lat, lng, addr));
                    } else {
                        runOnUiThread(() -> Toast.makeText(this,
                                "Location not in Sri Lanka. Please select a location in Sri Lanka.",
                                Toast.LENGTH_LONG).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(this,
                            "Location not found in Sri Lanka", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Error searching location", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private boolean isInSriLanka(double lat, double lng) {
        return lat >= 5.0 && lat <= 10.0 && lng >= 79.0 && lng <= 82.0;
    }

    private void updateMapLocation(double lat, double lng, String address) {
        if (!isMapReady || mMap == null) {
            selectedLat = lat;
            selectedLng = lng;
            selectedAddress = address;
            searchView.setQuery(address, false);
            return;
        }

        selectedLat = lat;
        selectedLng = lng;
        selectedAddress = address;

        LatLng location = new LatLng(lat, lng);
        if (currentMarker != null) {
            currentMarker.remove();
        }
        currentMarker = mMap.addMarker(new MarkerOptions().position(location).title(address));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
        searchView.setQuery(address, false);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        isMapReady = true;

        // Configure map UI
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);

        // Set Sri Lanka bounds to restrict camera movement
        mMap.setLatLngBoundsForCameraTarget(SRI_LANKA_BOUNDS);
        mMap.setMinZoomPreference(6.0f);
        mMap.setMaxZoomPreference(15.0f);

        // Set initial location to Colombo
        LatLng colombo = new LatLng(6.9271, 79.8612);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(colombo, 8));

        if (!selectedAddress.isEmpty()) {
            updateMapLocation(selectedLat, selectedLng, selectedAddress);
        }

        mMap.setOnMapClickListener(latLng -> {
            if (isInSriLanka(latLng.latitude, latLng.longitude)) {
                selectedLat = latLng.latitude;
                selectedLng = latLng.longitude;
                getAddressFromLatLng(latLng);

                if (currentMarker != null) {
                    currentMarker.remove();
                }
                currentMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
            } else {
                Toast.makeText(this, "Please select a location within Sri Lanka", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getAddressFromLatLng(LatLng latLng) {
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    String address = addresses.get(0).getAddressLine(0);
                    runOnUiThread(() -> {
                        selectedAddress = address;
                        searchView.setQuery(address, false);
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void confirmLocation() {
        if (selectedAddress.isEmpty()) {
            Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent();
        intent.putExtra("location", selectedAddress);
        intent.putExtra("lat", selectedLat);
        intent.putExtra("lng", selectedLng);
        setResult(RESULT_OK, intent);
        finish();
    }
}