package com.example.bankingapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
// Bỏ import android.net.Uri; nếu chỉ dùng Routes API và không mở Google Maps app nữa
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bankingapplication.Adapter.BankOfficeAdapter;
// Các import cho Model của Routes API (đảm bảo bạn đã tạo các file này)
import com.example.bankingapplication.Model.Routes.ComputeRoutesRequest;
import com.example.bankingapplication.Model.Routes.ComputeRoutesResponse;
import com.example.bankingapplication.Model.Routes.Location_Routes; // Đã đổi tên để tránh trùng
import com.example.bankingapplication.Model.Routes.Route; // Route của Routes API
import com.example.bankingapplication.Model.Routes.RoutesLatLng;
import com.example.bankingapplication.Model.Routes.Waypoint;
import com.example.bankingapplication.Model.Routes.Polyline; // Polyline của Routes API (nếu tên khác với gms.maps.model.Polyline)
// import com.example.bankingapplication.Model.Routes.Leg; // Nếu bạn dùng Leg của Routes API
// import com.example.bankingapplication.Model.Routes.Distance_Routes;
// import com.example.bankingapplication.Model.Routes.Duration_Routes;

import com.example.bankingapplication.Network.RoutesApiService; // Service mới
import com.example.bankingapplication.Object.BankOffice;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.maps.android.PolyUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class NavigationActivity extends AppCompatActivity implements OnMapReadyCallback, BankOfficeAdapter.OnOfficeClickListener {

    private static final String TAG = "NavigationActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final float DEFAULT_ZOOM = 14f;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private List<BankOffice> bankOfficesList = new ArrayList<>();
    private Location currentUserLocation;
    private ProgressBar progressBar;
    private com.google.android.gms.maps.model.Polyline currentRoutePolylineView; // Để vẽ lên bản đồ

    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private LinearLayout bottomSheetLayout;
    private RecyclerView recyclerViewBankOffices;
    private BankOfficeAdapter bankOfficeAdapter;
    private FloatingActionButton fabShowList;
    private Toolbar toolbar;

    private Retrofit retrofitRoutes;
    private RoutesApiService routesApiService;
    private String routesApiKey;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        toolbar = findViewById(R.id.toolbar_navigation);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        progressBar = findViewById(R.id.progressBar);
        fabShowList = findViewById(R.id.fab_show_list);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();

        routesApiKey = BuildConfig.DIRECTIONS_API_KEY; // Vẫn dùng key đã cấu hình
        if (routesApiKey == null || routesApiKey.isEmpty()) {
            Log.e(TAG, "ROUTES_API_KEY (DIRECTIONS_API_KEY) is not set or empty in BuildConfig.");
            Toast.makeText(this, "Lỗi cấu hình API chỉ đường. Chức năng có thể không hoạt động.", Toast.LENGTH_LONG).show();
        }
        setupRoutesApiService();

        setupBottomSheet();
        setupRecyclerView();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Lỗi - Không tìm thấy Map Fragment!", Toast.LENGTH_SHORT).show();
        }

        fabShowList.setOnClickListener(v -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    }

    private void setupRoutesApiService() {
        if (retrofitRoutes == null) { // Chỉ khởi tạo nếu chưa có
            retrofitRoutes = new Retrofit.Builder()
                    .baseUrl("https://routes.googleapis.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        routesApiService = retrofitRoutes.create(RoutesApiService.class);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupBottomSheet() {
        bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        try {
            bottomSheetBehavior.setPeekHeight(getResources().getDimensionPixelSize(R.dimen.bottom_sheet_peek_height));
        } catch (Exception e) {
            int fallbackPeekHeight = (int) (100 * getResources().getDisplayMetrics().density);
            bottomSheetBehavior.setPeekHeight(fallbackPeekHeight);
            Log.w(TAG, "R.dimen.bottom_sheet_peek_height not found, using fallback: " + fallbackPeekHeight + "px");
        }
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    fabShowList.setImageResource(R.drawable.ic_keyboard_arrow_down);
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    fabShowList.setImageResource(R.drawable.ic_list);
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });
    }

    private void setupRecyclerView() {
        recyclerViewBankOffices = findViewById(R.id.recycler_view_bank_offices);
        recyclerViewBankOffices.setLayoutManager(new LinearLayoutManager(this));
        bankOfficeAdapter = new BankOfficeAdapter(this, new ArrayList<>(), currentUserLocation, this);
        recyclerViewBankOffices.setAdapter(bankOfficeAdapter);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override public View getInfoWindow(@NonNull Marker marker) { return null; }
            @Override
            public View getInfoContents(@NonNull Marker marker) {
                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_window, null);
                BankOffice office = (BankOffice) marker.getTag();
                TextView title = infoWindow.findViewById(R.id.info_title);
                TextView addressView = infoWindow.findViewById(R.id.info_address);
                TextView phoneView = infoWindow.findViewById(R.id.info_phone);
                TextView hoursView = infoWindow.findViewById(R.id.info_hours);

                if (office != null) {
                    title.setText(office.getName());
                    addressView.setText("Địa chỉ: " + office.getAddress());
                    phoneView.setText("SĐT: " + office.getPhone());
                    hoursView.setText("Giờ: " + office.getOpenHoursFormatted() + " - " + office.getCloseHoursFormatted());
                } else {
                    title.setText(marker.getTitle());
                    addressView.setText(marker.getSnippet());
                    phoneView.setVisibility(View.GONE);
                    hoursView.setVisibility(View.GONE);
                }
                return infoWindow;
            }
        });

        mMap.setOnInfoWindowClickListener(marker -> {
            BankOffice office = (BankOffice) marker.getTag();
            if (office != null && office.getLatLng() != null && currentUserLocation != null) {
                fetchAndDrawRouteUsingRoutesAPI(
                        new LatLng(currentUserLocation.getLatitude(), currentUserLocation.getLongitude()),
                        office.getLatLng()
                );
            } else {
                Toast.makeText(this, "Không thể lấy vị trí hiện tại hoặc vị trí văn phòng.", Toast.LENGTH_SHORT).show();
            }
        });

        mMap.setOnMapClickListener(latLng -> {
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        checkLocationPermissionAndFetchData();
    }

    @Override
    public void onOfficeClick(BankOffice office) {
        if (office != null && office.getLatLng() != null && currentUserLocation != null) {
            fetchAndDrawRouteUsingRoutesAPI(
                    new LatLng(currentUserLocation.getLatitude(), currentUserLocation.getLongitude()),
                    office.getLatLng()
            );
            if (mMap != null) { // Kiểm tra mMap trước khi dùng
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(office.getLatLng(), DEFAULT_ZOOM + 1));
            }
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        } else {
            Toast.makeText(this, "Không có thông tin vị trí để vẽ đường đi.", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchAndDrawRouteUsingRoutesAPI(LatLng originGms, LatLng destinationGms) {
        if (routesApiKey == null || routesApiKey.isEmpty() || routesApiKey.equals("YOUR_PLACEHOLDER_IF_KEY_MISSING")) {
            Toast.makeText(this, "Chức năng chỉ đường chưa được cấu hình (API Key).", Toast.LENGTH_LONG).show();
            Log.e(TAG, "API Key for Routes API is missing or invalid.");
            return;
        }
        if (mMap == null || routesApiService == null) {
            Toast.makeText(this, "Dịch vụ bản đồ hoặc chỉ đường chưa sẵn sàng.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentRoutePolylineView != null) {
            currentRoutePolylineView.remove();
        }

        progressBar.setVisibility(View.VISIBLE);

        Waypoint originWaypoint = new Waypoint(new Location_Routes(new RoutesLatLng(originGms.latitude, originGms.longitude)));
        Waypoint destinationWaypoint = new Waypoint(new Location_Routes(new RoutesLatLng(destinationGms.latitude, destinationGms.longitude)));
        ComputeRoutesRequest requestBody = new ComputeRoutesRequest(originWaypoint, destinationWaypoint, "DRIVE");
        requestBody.setLanguageCode("vi"); // Yêu cầu thông tin bằng tiếng Việt nếu có

        String fieldMask = "routes.polyline.encodedPolyline,routes.duration,routes.distanceMeters";

        Log.d(TAG, "Requesting Routes API: Origin=" + originGms + ", Dest=" + destinationGms);

        routesApiService.computeRoutes(routesApiKey, fieldMask, requestBody)
                .enqueue(new Callback<ComputeRoutesResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ComputeRoutesResponse> call, @NonNull Response<ComputeRoutesResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            ComputeRoutesResponse routesResponse = response.body();
                            if (routesResponse.getRoutes() != null && !routesResponse.getRoutes().isEmpty()) {
                                Route route = routesResponse.getRoutes().get(0);

                                if (route.getPolyline() != null && route.getPolyline().getEncodedPolyline() != null) {
                                    List<LatLng> decodedPath = PolyUtil.decode(route.getPolyline().getEncodedPolyline());
                                    if (!decodedPath.isEmpty()) {
                                        currentRoutePolylineView = mMap.addPolyline(new PolylineOptions()
                                                .addAll(decodedPath)
                                                .width(12f)
                                                .color(ContextCompat.getColor(NavigationActivity.this, R.color.colorPrimary))
                                                .geodesic(true)
                                                .startCap(new RoundCap())
                                                .endCap(new RoundCap()));

                                        String durationStr = route.getDuration() != null ? formatDurationFromSecondsString(route.getDuration()) : "N/A";
                                        String distanceStr = route.getDistanceMeters() > 0 ? formatDistanceMeters(route.getDistanceMeters()) : "N/A";
                                        Toast.makeText(NavigationActivity.this, "Khoảng cách: " + distanceStr + ", Thời gian: " + durationStr, Toast.LENGTH_LONG).show();
                                        Log.d(TAG, "Route drawn. Distance: " + distanceStr + ", Duration: " + durationStr);

                                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                        for (LatLng point : decodedPath) {
                                            builder.include(point);
                                        }
                                        builder.include(originGms);
                                        builder.include(destinationGms);
                                        try {
                                            LatLngBounds bounds = builder.build();
                                            int padding = (int) (getResources().getDisplayMetrics().widthPixels * 0.15); // 15% màn hình
                                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                                            mMap.animateCamera(cu);
                                        } catch (IllegalStateException e) {
                                            Log.e(TAG, "Error animating camera to bounds: " + e.getMessage());
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destinationGms, DEFAULT_ZOOM));
                                        }
                                    } else {
                                        Log.w(TAG, "Routes API: Decoded path is empty.");
                                        Toast.makeText(NavigationActivity.this, "Không thể giải mã đường đi.", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Log.w(TAG, "Routes API: Polyline or encodedPolyline is null.");
                                    Toast.makeText(NavigationActivity.this, "Không nhận được dữ liệu polyline.", Toast.LENGTH_SHORT).show();
                                }
                            } else if (routesResponse.getError() != null) {
                                Log.e(TAG, "Routes API Error: " + routesResponse.getError().getStatus() + " - " + routesResponse.getError().getMessage());
                                Toast.makeText(NavigationActivity.this, "Lỗi chỉ đường: " + routesResponse.getError().getMessage(), Toast.LENGTH_LONG).show();
                            } else {
                                Log.w(TAG, "Routes API: No routes found or error not specified in response body.");
                                Toast.makeText(NavigationActivity.this, "Không tìm thấy lộ trình.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            String errorBodyStr = "";
                            try {
                                if (response.errorBody() != null) errorBodyStr = response.errorBody().string();
                            } catch (IOException e) { Log.e(TAG, "Error reading error body", e); }
                            Log.e(TAG, "Routes API request failed. Code: " + response.code() + ", Msg: " + response.message() + ", ErrorBody: " + errorBodyStr);
                            Toast.makeText(NavigationActivity.this, "Không thể tải chỉ đường (Mã: " + response.code() + ")", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ComputeRoutesResponse> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Routes API request failure: ", t);
                        Toast.makeText(NavigationActivity.this, "Lỗi kết nối khi tải chỉ đường.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String formatDurationFromSecondsString(String durationSecondsString) {
        if (durationSecondsString == null || !durationSecondsString.endsWith("s")) {
            return "N/A";
        }
        try {
            int totalSeconds = Integer.parseInt(durationSecondsString.replace("s", ""));
            if (totalSeconds < 0) return "N/A";
            int hours = totalSeconds / 3600;
            int minutes = (totalSeconds % 3600) / 60;
            StringBuilder sb = new StringBuilder();
            if (hours > 0) sb.append(hours).append(" giờ ");
            if (minutes > 0 || hours == 0) sb.append(minutes).append(" phút"); // Hiển thị phút nếu có, hoặc nếu giờ = 0
            return sb.length() > 0 ? sb.toString().trim() : "0 phút";
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing duration string: " + durationSecondsString, e);
            return "N/A";
        }
    }

    private String formatDistanceMeters(int distanceMeters) {
        if (distanceMeters < 0) return "N/A";
        if (distanceMeters == 0) return "0 km";
        double distanceKm = distanceMeters / 1000.0;
        return String.format(Locale.getDefault(), "%.1f km", distanceKm);
    }


    // --- Các phương thức còn lại (checkLocationPermissionAndFetchData, etc.) ---
    // Giữ nguyên các phương thức này như bạn đã cung cấp.
    // Đảm bảo tên collection trong fetchBankOffices là "bankofficers"
    // và tên trường "name" được lấy đúng từ document.

    private void checkLocationPermissionAndFetchData() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            if (mMap != null) mMap.setMyLocationEnabled(true);
            getCurrentLocationAndFetchBanks();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (mMap != null) mMap.setMyLocationEnabled(true);
                getCurrentLocationAndFetchBanks();
            } else {
                Toast.makeText(this, "Quyền vị trí bị từ chối.", Toast.LENGTH_LONG).show();
                fetchBankOffices(null);
            }
        }
    }

    private void getCurrentLocationAndFetchBanks() {
        progressBar.setVisibility(View.VISIBLE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            progressBar.setVisibility(View.GONE);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    progressBar.setVisibility(View.GONE);
                    if (location != null) {
                        Log.d(TAG, "Current location obtained: " + location.getLatitude() + ", " + location.getLongitude());
                        currentUserLocation = location;
                        if (bankOfficeAdapter != null) {
                            bankOfficeAdapter.updateUserLocationAndSort(currentUserLocation);
                        }
                        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        if (mMap != null) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, DEFAULT_ZOOM));
                        }
                        fetchBankOffices(userLatLng);
                    } else {
                        Toast.makeText(this, "Không thể lấy vị trí hiện tại. Hãy đảm bảo vị trí đã được bật.", Toast.LENGTH_LONG).show();
                        fetchBankOffices(null);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi khi lấy vị trí: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    fetchBankOffices(null);
                });
    }

    private void fetchBankOffices(final LatLng userLatLng) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("bankofficers")
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        List<BankOffice> fetchedOffices = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String officeName = document.getString("name"); // Lấy tên từ trường 'name' của document
                            String address = document.getString("address");
                            String phone = document.getString("phone");
                            Timestamp openHoursTimestamp = document.getTimestamp("openHours");
                            Timestamp closeHoursTimestamp = document.getTimestamp("closeHours");

                            Map<String, Object> locationMap = (Map<String, Object>) document.get("location");
                            if (locationMap != null) {
                                String latStr = (String) locationMap.get("latitude");
                                String lonStr = (String) locationMap.get("longitude");

                                if (latStr != null && lonStr != null) {
                                    double lat = parseDmsToDecimal(latStr);
                                    double lon = parseDmsToDecimal(lonStr);

                                    if (lat != 0.0 || lon != 0.0) { // Allow (0,0) if it's a valid coordinate for some reason
                                        LatLng officeLatLng = new LatLng(lat, lon);
                                        BankOffice office = new BankOffice(document.getId(), officeName, address, officeLatLng, phone, openHoursTimestamp, closeHoursTimestamp);
                                        fetchedOffices.add(office);

                                        if (mMap != null) {
                                            Marker marker = mMap.addMarker(new MarkerOptions()
                                                    .position(officeLatLng)
                                                    .title(officeName)
                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                                            if (marker != null) marker.setTag(office);
                                        }
                                    } else {
                                        Log.w(TAG, "Parsed lat/lon is (0,0) for " + officeName + ". Original DMS: " + latStr + ", " + lonStr);
                                    }
                                } else {
                                    Log.w(TAG, "Latitude or Longitude string is null in locationMap for document: " + document.getId());
                                }
                            } else {
                                Log.w(TAG, "locationMap is null for document: " + document.getId());
                            }
                        }
                        this.bankOfficesList.clear();
                        this.bankOfficesList.addAll(fetchedOffices);

                        if (bankOfficeAdapter != null) {
                            bankOfficeAdapter.updateBankOfficeList(new ArrayList<>(this.bankOfficesList));
                            if (this.currentUserLocation != null) {
                                bankOfficeAdapter.updateUserLocationAndSort(this.currentUserLocation);
                            }
                        }

                        if (userLatLng != null && !this.bankOfficesList.isEmpty()) {
                            findAndHighlightNearestBank(userLatLng);
                        } else if (this.bankOfficesList.isEmpty()) {
                            Toast.makeText(NavigationActivity.this, "Không tìm thấy văn phòng nào.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "Lỗi khi lấy documents từ bankofficers.", task.getException());
                        Toast.makeText(NavigationActivity.this, "Lỗi khi tải danh sách văn phòng.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private double parseDmsToDecimal(String dmsStringWithDirection) {
        if (dmsStringWithDirection == null || dmsStringWithDirection.isEmpty()) { Log.e(TAG, "DMS string is null or empty."); return 0.0; }
        char direction = ' ';
        String dmsString = dmsStringWithDirection.toUpperCase().trim();
        if (dmsString.endsWith("N") || dmsString.endsWith("S") || dmsString.endsWith("E") || dmsString.endsWith("W")) {
            direction = dmsString.charAt(dmsString.length() - 1);
            dmsString = dmsString.substring(0, dmsString.length() - 1);
        }
        dmsString = dmsString.replace("\"", "").replace("“", "").replace("”", "");
        Pattern pattern = Pattern.compile("(\\d+)[°\\s]+(\\d+)[?'\\s]+([\\d.]+).*");
        Matcher matcher = pattern.matcher(dmsString);
        if (matcher.find() && matcher.groupCount() >= 3) {
            try {
                double degrees = Double.parseDouble(matcher.group(1));
                double minutes = Double.parseDouble(matcher.group(2));
                double seconds = Double.parseDouble(matcher.group(3));
                double decimal = degrees + (minutes / 60.0) + (seconds / 3600.0);
                if (direction == 'S' || direction == 'W') { decimal *= -1; }
                return decimal;
            } catch (NumberFormatException e) { Log.e(TAG, "Error parsing DMS components from: " + dmsStringWithDirection, e); return 0.0; }
        } else {
            Log.e(TAG, "DMS string does not match expected pattern: " + dmsStringWithDirection + " (cleaned: " + dmsString + ")");
            try { return Double.parseDouble(dmsStringWithDirection.replaceAll("[^0-9.-]", ""));}
            catch (NumberFormatException e) { Log.e(TAG, "Fallback parsing failed for: " + dmsStringWithDirection, e); return 0.0; }
        }
    }

    private void findAndHighlightNearestBank(LatLng userLatLng) {
        if (bankOfficesList.isEmpty()) return;
        BankOffice nearestOffice = null;
        float minDistance = Float.MAX_VALUE;
        for (BankOffice office : bankOfficesList) {
            if (office.getLatLng() == null) continue;
            float[] results = new float[1];
            Location.distanceBetween(userLatLng.latitude, userLatLng.longitude, office.getLatLng().latitude, office.getLatLng().longitude, results);
            if (results[0] < minDistance) {
                minDistance = results[0];
                nearestOffice = office;
            }
        }
        if (nearestOffice != null && mMap != null) {
            Toast.makeText(this, "Văn phòng gần nhất: " + (nearestOffice.getName() != null ? nearestOffice.getName() : "N/A"), Toast.LENGTH_LONG).show();
            if (nearestOffice.getLatLng() != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nearestOffice.getLatLng(), DEFAULT_ZOOM + 1));
            }
        }
    }
}