package com.example.bankingapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
// Bỏ import android.graphics.Color; // Nếu không dùng trực tiếp
import android.location.Location;
import android.net.Uri; // Cần cho Intent mở Google Maps
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
import com.example.bankingapplication.Object.BankOffice;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap; // GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback; // OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment; // SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
// import com.google.android.gms.maps.model.LatLngBounds; // Giữ lại nếu bạn dùng
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
// Bỏ import Polyline, PolylineOptions, RoundCap của Google nếu không vẽ đường fallback nữa
// import com.google.android.gms.maps.model.Polyline;
// import com.google.android.gms.maps.model.PolylineOptions;
// import com.google.android.gms.maps.model.RoundCap;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
// Bỏ import PolyUtil của Google Maps Utils
// import com.google.maps.android.PolyUtil;
// Bỏ các import của Retrofit và Model classes của Directions/Routes API

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale; // Cần cho String.format
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NavigationActivity extends AppCompatActivity implements OnMapReadyCallback, BankOfficeAdapter.OnOfficeClickListener {

    private static final String TAG = "NavigationActivityGoogle"; // Đổi TAG
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final float DEFAULT_ZOOM = 14f;

    private GoogleMap mMap; // Sử dụng GoogleMap
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private List<BankOffice> bankOfficesList = new ArrayList<>();
    private HashMap<String, Marker> bankOfficeMarkers = new HashMap<>();
    private Location currentUserLocation;
    private ProgressBar progressBar;
    // private Polyline currentRoutePolylineView; // Bỏ nếu không vẽ đường nữa

    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private RecyclerView recyclerViewBankOffices;
    private BankOfficeAdapter bankOfficeAdapter;
    private FloatingActionButton fabShowList;
    private Toolbar toolbar;

    // Bỏ các biến Retrofit và API key của HERE/Mapbox
    // private Retrofit retrofitRoutes;
    // private RoutesApiService routesApiService;
    // private String routesApiKey;

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

        // Bỏ setupRoutesApiService();

        setupBottomSheet();
        setupRecyclerView();

        // Khởi tạo SupportMapFragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map); // ID của FragmentContainerView trong XML
        if (mapFragment != null) {
            mapFragment.getMapAsync(this); // this (Activity) implement OnMapReadyCallback
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupBottomSheet() {
        // ... (Giữ nguyên logic setupBottomSheet)
        LinearLayout bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        try {
            bottomSheetBehavior.setPeekHeight(getResources().getDimensionPixelSize(R.dimen.bottom_sheet_peek_height));
        } catch (Exception e) {
            bottomSheetBehavior.setPeekHeight(100 * (int) getResources().getDisplayMetrics().density);
            Log.w(TAG, "R.dimen.bottom_sheet_peek_height not found, using fallback 100dp");
        }
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // Log trạng thái để debug
                // String stateString = "";
                // switch (newState) {
                //     case BottomSheetBehavior.STATE_COLLAPSED: stateString = "COLLAPSED"; break;
                //     case BottomSheetBehavior.STATE_EXPANDED: stateString = "EXPANDED"; break;
                //     case BottomSheetBehavior.STATE_DRAGGING: stateString = "DRAGGING"; break;
                //     case BottomSheetBehavior.STATE_SETTLING: stateString = "SETTLING"; break;
                //     case BottomSheetBehavior.STATE_HIDDEN: stateString = "HIDDEN"; break;
                //     case BottomSheetBehavior.STATE_HALF_EXPANDED: stateString = "HALF_EXPANDED"; break;
                // }
                // Log.d(TAG, "BottomSheet onStateChanged: " + stateString);


                if (fabShowList == null) return; // Kiểm tra null cho FAB

                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    fabShowList.setImageResource(R.drawable.ic_keyboard_arrow_down); // Đảm bảo bạn có drawable này
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    fabShowList.setImageResource(R.drawable.ic_list); // Đảm bảo bạn có drawable này
                }
                // Bạn có thể xử lý các trạng thái khác nếu cần (DRAGGING, SETTLING, HALF_EXPANDED)
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // slideOffset từ -1.0 (ẩn) đến 0.0 (peekHeight/collapsed) đến 1.0 (mở rộng hoàn toàn)
                // Bạn có thể để trống nếu không cần xử lý gì đặc biệt khi đang trượt.
                // Ví dụ:
                // if (fabShowList != null) {
                //     // Làm mờ FAB khi bottom sheet không ở trạng thái collapsed hoặc expanded hoàn toàn
                //     if (slideOffset > 0 && slideOffset < 1) { // Đang giữa collapsed và expanded
                //         fabShowList.setAlpha(1.0f - slideOffset);
                //     } else if (slideOffset < 0) { // Đang giữa collapsed và hidden
                //         fabShowList.setAlpha(1.0f + slideOffset);
                //     } else { // Ở trạng thái collapsed, expanded, hoặc hidden
                //         fabShowList.setAlpha(1.0f);
                //     }
                // }
            }
        });
    }

    private void setupRecyclerView() {
        // ... (Giữ nguyên logic setupRecyclerView)
        recyclerViewBankOffices = findViewById(R.id.recycler_view_bank_offices);
        recyclerViewBankOffices.setLayoutManager(new LinearLayoutManager(this));
        bankOfficeAdapter = new BankOfficeAdapter(this, new ArrayList<>(), currentUserLocation, this);
        recyclerViewBankOffices.setAdapter(bankOfficeAdapter);
    }

    // Callback khi GoogleMap sẵn sàng
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.i(TAG, "Google Map is ready.");

        // Bạn có thể bật/tắt nút zoom mặc định ở đây
        // mMap.getUiSettings().setZoomControlsEnabled(true); // Hoặc false nếu bạn tự tạo nút zoom

        mMap.getUiSettings().setMyLocationButtonEnabled(true); // Nút vị trí của tôi (mặc định của Google)

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(@NonNull Marker marker) { return null; } // Sử dụng frame mặc định
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
                    title.setText(marker.getTitle()); // Fallback nếu không có BankOffice object
                    addressView.setText(marker.getSnippet());
                    phoneView.setVisibility(View.GONE);
                    hoursView.setVisibility(View.GONE);
                }
                return infoWindow;
            }
        });

        // Khi người dùng nhấn vào InfoWindow (cái bảng thông tin của marker)
        // Trong onMapReady()
        mMap.setOnInfoWindowClickListener(marker -> {
            BankOffice office = (BankOffice) marker.getTag();
            if (office != null && office.getLatLng() != null) {
                // Truyền tên văn phòng cho trường hợp fallback
                openGoogleMapsForDirections(office.getLatLng(), office.getName());
            } else {
                Toast.makeText(NavigationActivity.this, "Không có đủ thông tin để chỉ đường.", Toast.LENGTH_SHORT).show();
            }
        });

        mMap.setOnMapClickListener(latLng -> {
            if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        checkLocationPermissionAndFetchData();
    }

    // Khi nhấn vào một item trong RecyclerView
    @Override
    public void onOfficeClick(BankOffice office) {
        if (mMap == null) {
            Toast.makeText(this, "Bản đồ chưa sẵn sàng.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (office != null && office.getLatLng() != null && office.getUID() != null) {
            Marker markerToShow = bankOfficeMarkers.get(office.getUID());

            if (markerToShow != null) {
                // Di chuyển camera đến marker và hiển thị InfoWindow
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markerToShow.getPosition(), DEFAULT_ZOOM + 2), 500, null);
                markerToShow.showInfoWindow();
            } else {
                // Fallback: nếu không tìm thấy marker, chỉ di chuyển camera
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(office.getLatLng(), DEFAULT_ZOOM + 1));
                Log.w(TAG, "Marker không được tìm thấy trong HashMap cho văn phòng: " + office.getName());
                Toast.makeText(this, "Đã chọn: " + office.getName() + ". Không tìm thấy marker liên kết.", Toast.LENGTH_SHORT).show();
            }

            if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        } else {
            Toast.makeText(this, "Không có thông tin vị trí hoặc ID cho văn phòng này.", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm mở ứng dụng Google Maps để chỉ đường
    private void openGoogleMapsForDirections(LatLng destination, String destinationName) {
        if (currentUserLocation == null) {
            // Nếu không có vị trí hiện tại, chỉ hiển thị điểm đến trên Google Maps
            Log.w(TAG, "Current user location is null. Showing destination only.");
            String uriStringViewLocation = String.format(Locale.US, "geo:0,0?q=%f,%f(%s)",
                    destination.latitude,
                    destination.longitude,
                    Uri.encode(destinationName != null ? destinationName : "Điểm đến")
            );
            Uri gmmIntentUri = Uri.parse(uriStringViewLocation);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Toast.makeText(this, "Không tìm thấy ứng dụng Google Maps.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Có vị trí hiện tại, tạo intent để hiển thị màn hình xem trước lộ trình
        String originLatLng = currentUserLocation.getLatitude() + "," + currentUserLocation.getLongitude();
        String destinationLatLng = destination.latitude + "," + destination.longitude;

        // Tạo Uri cho Google Maps Intent để hiển thị màn hình xem trước lộ trình
        // Sử dụng daddr (destination address) và saddr (source address)
        // Thêm dirflg=d để ưu tiên chế độ lái xe (d: driving, w: walking, b: bicycling, r: transit)
        Uri gmmIntentUri = Uri.parse("https://maps.google.com/maps?saddr=" + originLatLng + "&daddr=" + destinationLatLng + "&dirflg=d");

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps"); // Chỉ định mở bằng Google Maps

        Log.d(TAG, "Opening Google Maps with directions intent: " + gmmIntentUri.toString());

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "Không tìm thấy ứng dụng Google Maps.", Toast.LENGTH_SHORT).show();
            // Fallback: Mở bằng trình duyệt web nếu không có ứng dụng Google Maps
            Intent webIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/dir/?api=1&origin=" + originLatLng + "&destination=" + destinationLatLng + "&travelmode=driving"));
            if (webIntent.resolveActivity(getPackageManager()) != null) {
                Log.d(TAG, "Falling back to web browser for directions.");
                startActivity(webIntent);
            } else {
                Toast.makeText(this, "Không thể mở bản đồ chỉ đường.", Toast.LENGTH_LONG).show();
            }
        }
    }


    // --- Các hàm xử lý vị trí và Firebase Firestore ---
    private void checkLocationPermissionAndFetchData() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            if (mMap != null) mMap.setMyLocationEnabled(true); // Bật layer vị trí của tôi trên Google Map
            getCurrentLocationAndFetchBanks();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
                fetchBankOffices(null); // Vẫn tải danh sách ngân hàng
            }
        }
    }

    private void getCurrentLocationAndFetchBanks() {
        progressBar.setVisibility(View.VISIBLE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            progressBar.setVisibility(View.GONE); return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    progressBar.setVisibility(View.GONE);
                    if (location != null) {
                        currentUserLocation = location;
                        Log.d(TAG, "Current location: " + location.getLatitude() + "," + location.getLongitude());
                        if (mMap != null) {
                            LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, DEFAULT_ZOOM));
                        }
                        if (bankOfficeAdapter != null) {
                            bankOfficeAdapter.updateUserLocationAndSort(currentUserLocation);
                        }
                        fetchBankOffices(new LatLng(location.getLatitude(), location.getLongitude()));
                    } else { /* ... */ }
                })
                .addOnFailureListener(e -> { /* ... */ });
    }

    private void fetchBankOffices(final LatLng userGoogleLatLng) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        db.collection("bankofficers") // Đảm bảo tên collection đúng
                .get()
                .addOnCompleteListener(task -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        bankOfficesList.clear();
                        bankOfficeMarkers.clear(); // Xóa markers cũ trong HashMap
                        if (mMap != null) {
                            mMap.clear(); // Xóa tất cả markers, polylines, etc. khỏi bản đồ
                        }

                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                String officeName = document.getString("name");
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
                                        if (lat != 0.0 || lon != 0.0) { // Cho phép (0,0) nếu là tọa độ hợp lệ
                                            LatLng officeLatLng = new LatLng(lat, lon);
                                            String officeUID = document.getId(); // Lấy UID
                                            BankOffice office = new BankOffice(officeUID, officeName, address, officeLatLng, phone, openHoursTimestamp, closeHoursTimestamp);
                                            bankOfficesList.add(office);

                                            if (mMap != null) {
                                                MarkerOptions markerOptions = new MarkerOptions()
                                                        .position(officeLatLng)
                                                        .title(officeName) // Title sẽ hiển thị mặc định khi chạm marker
                                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                                                Marker marker = mMap.addMarker(markerOptions);
                                                if (marker != null) {
                                                    marker.setTag(office); // Gắn đối tượng BankOffice vào marker
                                                    bankOfficeMarkers.put(officeUID, marker); // Lưu marker vào HashMap
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (bankOfficeAdapter != null) {
                            bankOfficeAdapter.updateBankOfficeList(new ArrayList<>(bankOfficesList));
                            if (currentUserLocation != null) {
                                bankOfficeAdapter.updateUserLocationAndSort(currentUserLocation);
                            }
                        }

                        if (userGoogleLatLng != null && !bankOfficesList.isEmpty()) {
                            findAndHighlightNearestBank(userGoogleLatLng); // Gọi lại nếu bạn muốn chức năng này
                        } else if (bankOfficesList.isEmpty()) {
                            Toast.makeText(NavigationActivity.this, "Không tìm thấy văn phòng nào.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "Lỗi khi lấy documents từ bankofficers.", task.getException());
                        Toast.makeText(NavigationActivity.this, "Lỗi khi tải danh sách văn phòng.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private double parseDmsToDecimal(String dmsStringWithDirection) {
        // ... (code parseDmsToDecimal như cũ)
        if (dmsStringWithDirection == null || dmsStringWithDirection.isEmpty()) { Log.e(TAG, "DMS string is null or empty."); return 0.0; }
        char direction = ' '; String dmsString = dmsStringWithDirection.toUpperCase().trim();
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
            } catch (NumberFormatException e) { Log.e(TAG, "Error parsing DMS: " + dmsStringWithDirection, e); return 0.0; }
        } else {
            Log.e(TAG, "DMS pattern not matched: " + dmsStringWithDirection);
            try { return Double.parseDouble(dmsStringWithDirection.replaceAll("[^0-9.-]", ""));}
            catch (NumberFormatException e) { Log.e(TAG, "Fallback DMS parsing failed: " + dmsStringWithDirection, e); return 0.0; }
        }
    }

    // Hàm findAndHighlightNearestBank có thể giữ lại nếu bạn muốn chức năng này
    // Hàm findAndHighlightNearestBank có thể giữ lại nếu bạn muốn chức năng này
    private void findAndHighlightNearestBank(LatLng userLatLng) {
        if (bankOfficesList.isEmpty() || mMap == null) {
            return;
        }

        BankOffice nearestOffice = null;
        float minDistance = Float.MAX_VALUE;

        for (BankOffice office : bankOfficesList) {
            if (office.getLatLng() == null) continue;
            float[] results = new float[1];
            Location.distanceBetween(userLatLng.latitude, userLatLng.longitude,
                    office.getLatLng().latitude, office.getLatLng().longitude, results);
            float distance = results[0];

            if (distance < minDistance) {
                minDistance = distance;
                nearestOffice = office;
            }
        }

        if (nearestOffice != null && nearestOffice.getUID() != null) {
            Marker nearestMarker = bankOfficeMarkers.get(nearestOffice.getUID());
            if (nearestMarker != null) {
                // Thay đổi icon của marker gần nhất (ví dụ: sang màu xanh lá)
                nearestMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                nearestMarker.showInfoWindow(); // Hiển thị InfoWindow của marker gần nhất
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nearestMarker.getPosition(), DEFAULT_ZOOM + 1));
                Toast.makeText(this, "Văn phòng gần nhất: " + nearestOffice.getName(), Toast.LENGTH_LONG).show();
            } else {
                Log.w(TAG, "Nearest marker not found in HashMap for office: " + nearestOffice.getName());
                // Fallback nếu không tìm thấy marker, vẫn có thể di chuyển camera
                if (nearestOffice.getLatLng() != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nearestOffice.getLatLng(), DEFAULT_ZOOM + 1));
                    Toast.makeText(this, "Văn phòng gần nhất (không có marker): " + nearestOffice.getName(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    // Bỏ các hàm lifecycle của MapView (onStart, onStop, etc.) vì SupportMapFragment tự quản lý
}