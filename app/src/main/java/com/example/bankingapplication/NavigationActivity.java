package com.example.bankingapplication; // Hoặc package của bạn

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color; // Import Color
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem; // Import MenuItem
import android.view.View;
import android.widget.LinearLayout; // Import LinearLayout
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Import Toolbar
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager; // Import LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView; // Import RecyclerView

import com.example.bankingapplication.Adapter.BankOfficeAdapter; // Import Adapter của bạn
import com.example.bankingapplication.Object.BankOffice; // Import Object của bạn
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline; // Import Polyline
import com.google.android.gms.maps.model.PolylineOptions; // Import PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior; // Import BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Import FloatingActionButton
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Implement BankOfficeAdapter.OnOfficeClickListener
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
    private Marker nearestBankMarker;
    private Polyline currentRoutePolyline; // Để vẽ đường đi

    // Views cho BottomSheet và RecyclerView
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private LinearLayout bottomSheetLayout;
    private RecyclerView recyclerViewBankOffices;
    private BankOfficeAdapter bankOfficeAdapter;
    private FloatingActionButton fabShowList;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        toolbar = findViewById(R.id.toolbar_navigation);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Hiển thị nút back
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }


        progressBar = findViewById(R.id.progressBar);
        fabShowList = findViewById(R.id.fab_show_list);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Xử lý sự kiện nhấn nút back trên toolbar
        if (item.getItemId() == android.R.id.home) {
            finish(); // Đóng activity hiện tại và quay lại activity trước đó
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupBottomSheet() {
        bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        bottomSheetBehavior.setPeekHeight(getResources().getDimensionPixelSize(R.dimen.bottom_sheet_peek_height)); // Đặt peek height trong dimens.xml
        // Ví dụ trong res/values/dimens.xml: <dimen name="bottom_sheet_peek_height">100dp</dimen>
        bottomSheetBehavior.setHideable(true); // Cho phép ẩn hoàn toàn
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED); // Trạng thái ban đầu

        // Xử lý khi trạng thái của bottom sheet thay đổi (tùy chọn)
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // Ví dụ: thay đổi icon của FAB
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    fabShowList.setImageResource(R.drawable.ic_keyboard_arrow_down); // Tạo icon này
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    fabShowList.setImageResource(R.drawable.ic_list);
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Xử lý khi bottom sheet đang được kéo
            }
        });
    }
    // Tạo drawable ic_keyboard_arrow_down.xml
    // <vector xmlns:android="http://schemas.android.com/apk/res/android"
    //     android:width="24dp"
    //     android:height="24dp"
    //     android:viewportWidth="24.0"
    //     android:viewportHeight="24.0"
    //     android:tint="?attr/colorControlNormal">
    //   <path
    //       android:fillColor="@android:color/white"
    //       android:pathData="M7.41,7.84L12,12.42l4.59,-4.58L18,9.25l-6,6 -6,-6z"/>
    // </vector>

    private void setupRecyclerView() {
        recyclerViewBankOffices = findViewById(R.id.recycler_view_bank_offices);
        recyclerViewBankOffices.setLayoutManager(new LinearLayoutManager(this));
        // Khởi tạo adapter với danh sách rỗng và currentUserLocation có thể là null ban đầu
        bankOfficeAdapter = new BankOfficeAdapter(this, new ArrayList<>(), currentUserLocation, this);
        recyclerViewBankOffices.setAdapter(bankOfficeAdapter);
    }

    // Khai báo biến Polyline ở đầu lớp nếu chưa có
    private Polyline currentFallbackPolyline; // Hoặc bạn có thể dùng chung biến với currentRoutePolyline nếu chỉ có 1 loại polyline tại một thời điểm

    // Hàm vẽ đường thẳng dự phòng
    private void drawFallbackPolyline(LatLng origin, LatLng destination) {
        if (mMap == null) { // Kiểm tra xem bản đồ đã sẵn sàng chưa
            Log.w(TAG, "Map is not ready, cannot draw fallback polyline.");
            return;
        }

        if (origin == null || destination == null) {
            Log.w(TAG, "Origin or destination is null, cannot draw fallback polyline.");
            return;
        }

        // Xóa polyline cũ nếu có
        if (currentFallbackPolyline != null) {
            currentFallbackPolyline.remove();
        }
        // Hoặc nếu bạn dùng chung biến currentRoutePolyline:
        // if (currentRoutePolyline != null) {
        //    currentRoutePolyline.remove();
        // }


        PolylineOptions polylineOptions = new PolylineOptions()
                .add(origin)
                .add(destination)
                .width(8) // Độ rộng có thể khác một chút so với đường "thật"
                .color(Color.GRAY) // Màu xám hoặc một màu khác để phân biệt
                .geodesic(true);

        currentFallbackPolyline = mMap.addPolyline(polylineOptions);
        // Hoặc nếu dùng chung biến:
        // currentRoutePolyline = mMap.addPolyline(polylineOptions);

        Log.d(TAG, "Fallback polyline drawn from " + origin.toString() + " to " + destination.toString());

        // Tùy chọn: Di chuyển camera để thấy cả hai điểm
        // LatLngBounds.Builder builder = new LatLngBounds.Builder();
        // builder.include(origin);
        // builder.include(destination);
        // LatLngBounds bounds = builder.build();
        // int padding = 100; // pixels
        // CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        // mMap.animateCamera(cu);
    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(true); // Tắt nút mặc định vì ta có FAB và BottomSheet

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(@NonNull Marker marker) { return null; }
            @Override
            public View getInfoContents(@NonNull Marker marker) {
                // ... code InfoWindow như cũ ...
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
                    if (marker.getSnippet() != null && !marker.getSnippet().isEmpty()) {
                        addressView.setText(marker.getSnippet());
                    } else {
                        addressView.setVisibility(View.GONE);
                    }
                    phoneView.setVisibility(View.GONE);
                    hoursView.setVisibility(View.GONE);
                }
                return infoWindow;
            }
        });

        mMap.setOnInfoWindowClickListener(marker -> {
            BankOffice office = (BankOffice) marker.getTag();
            if (office != null && office.getLatLng() != null) {
                // Mở Google Maps để chỉ đường
                Uri gmmIntentUri = Uri.parse(String.format(Locale.US, "google.navigation:q=%f,%f",
                        office.getLatLng().latitude, office.getLatLng().longitude));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Toast.makeText(this, "Không tìm thấy ứng dụng Google Maps.", Toast.LENGTH_SHORT).show();
                    // Fallback nếu muốn
                    if (currentUserLocation != null) {
                        drawFallbackPolyline(new LatLng(currentUserLocation.getLatitude(), currentUserLocation.getLongitude()), office.getLatLng());
                    }
                }
            } else {
                Toast.makeText(this, "Không có thông tin vị trí cho văn phòng này.", Toast.LENGTH_SHORT).show();
            }
        });

        mMap.setOnMapClickListener(latLng -> {
            // Khi click vào bản đồ, có thể ẩn bottom sheet nếu đang mở rộng
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        checkLocationPermissionAndFetchData();
    }

    private void checkLocationPermissionAndFetchData() {
        // ... code như cũ ...
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true); // Bật lại nếu đã tắt
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
        // ... code như cũ ...
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mMap.setMyLocationEnabled(true);
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
                        // CẬP NHẬT VỊ TRÍ CHO ADAPTER NGAY KHI CÓ
                        if (bankOfficeAdapter != null) { // Kiểm tra adapter đã được khởi tạo chưa
                            bankOfficeAdapter.updateUserLocationAndSort(currentUserLocation);
                        }
                        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        if (mMap != null) { // Kiểm tra mMap đã sẵn sàng chưa
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, DEFAULT_ZOOM));
                        }
                        fetchBankOffices(userLatLng); // Tải danh sách ngân hàng
                    } else {
                        Toast.makeText(this, "Không thể lấy vị trí hiện tại.", Toast.LENGTH_LONG).show();
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
                        List<BankOffice> fetchedOffices = new ArrayList<>(); // Tạo list tạm
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> locationMap = (Map<String, Object>) document.get("location");
                            if (locationMap != null) {
                                String officeName = document.getString("name");
                                String address = document.getString("address");
                                String phone = document.getString("phone");
                                Timestamp openHoursTimestamp = document.getTimestamp("openHours");
                                Timestamp closeHoursTimestamp = document.getTimestamp("closeHours");
                                String latStr = (String) locationMap.get("latitude");
                                String lonStr = (String) locationMap.get("longitude");

                                if (latStr != null && lonStr != null) {
                                    double lat = parseDmsToDecimal(latStr);
                                    double lon = parseDmsToDecimal(lonStr);

                                    if (lat != 0.0 && lon != 0.0) {
                                        LatLng officeLatLng = new LatLng(lat, lon);
                                        BankOffice office = new BankOffice(document.getId(), officeName, address, officeLatLng, phone, openHoursTimestamp, closeHoursTimestamp);
                                        fetchedOffices.add(office); // Thêm vào list tạm

                                        Marker marker = mMap.addMarker(new MarkerOptions()
                                                .position(officeLatLng)
                                                .title(officeName)
                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                                        marker.setTag(office);
                                    }
                                }
                            }
                        }

                        // Cập nhật danh sách vào biến toàn cục của Activity
                        this.bankOfficesList.clear();
                        this.bankOfficesList.addAll(fetchedOffices);

                        // Cập nhật adapter với danh sách MỚI và vị trí người dùng HIỆN TẠI
                        if (bankOfficeAdapter != null) {
                            bankOfficeAdapter.updateBankOfficeList(new ArrayList<>(this.bankOfficesList));
                            // Cập nhật vị trí người dùng để adapter có thể sắp xếp theo khoảng cách
                            if (this.currentUserLocation != null) {
                                bankOfficeAdapter.updateUserLocationAndSort(this.currentUserLocation);
                            }
                        }

                        if (userLatLng != null && !this.bankOfficesList.isEmpty()) {
                            findAndHighlightNearestBank(userLatLng); // Highlight marker gần nhất
                        } else if (this.bankOfficesList.isEmpty()) {
                            Toast.makeText(NavigationActivity.this, "Không tìm thấy văn phòng nào.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "Lỗi khi lấy documents.", task.getException());
                        Toast.makeText(NavigationActivity.this, "Lỗi khi tải danh sách văn phòng.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ... (parseDmsToDecimal, findAndHighlightNearestBank giữ nguyên) ...
    private double parseDmsToDecimal(String dmsStringWithDirection) {
        if (dmsStringWithDirection == null || dmsStringWithDirection.isEmpty()) {
            Log.e(TAG, "Chuỗi DMS rỗng hoặc null.");
            return 0.0;
        }

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
                if (direction == 'S' || direction == 'W') {
                    decimal *= -1;
                }
                return decimal;
            } catch (NumberFormatException e) {
                Log.e(TAG, "Lỗi phân tích các thành phần DMS từ: " + dmsStringWithDirection, e);
                return 0.0;
            }
        } else {
            Log.e(TAG, "Chuỗi DMS không khớp mẫu mong đợi: " + dmsStringWithDirection + " (đã làm sạch: " + dmsString + ")");
            try {
                return Double.parseDouble(dmsStringWithDirection.replaceAll("[^0-9.-]", ""));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Phân tích dự phòng thất bại cho: " + dmsStringWithDirection, e);
                return 0.0;
            }
        }
    }

    private void findAndHighlightNearestBank(LatLng userLatLng) {
        if (bankOfficesList.isEmpty()) return;
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

        if (nearestOffice != null) {
            // Tìm marker tương ứng và làm nổi bật (logic này có thể cần xem lại nếu có nhiều marker)
            // Lặp qua các marker trên bản đồ và tìm marker có tag là nearestOffice
            // mMap.get... không có getMarkers()
            // Tạm thời chỉ di chuyển camera và hiển thị InfoWindow cho marker gần nhất nếu biết cách lấy nó
            if (mMap != null) { // Đảm bảo mMap đã sẵn sàng
                // Bạn cần một cách để lấy đúng marker của nearestOffice
                // Ví dụ: lưu trữ marker khi tạo và tìm lại, hoặc duyệt tất cả marker
                // Để đơn giản, giả sử bạn có thể tìm được marker đó
                // Marker markerToHighlight = findMarkerForOffice(nearestOffice);
                // if (markerToHighlight != null) {
                //    markerToHighlight.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                //    markerToHighlight.showInfoWindow();
                //    mMap.animateCamera(CameraUpdateFactory.newLatLng(markerToHighlight.getPosition()));
                // }
                Toast.makeText(this, "Văn phòng gần nhất: " + nearestOffice.getName(), Toast.LENGTH_LONG).show();
                if (nearestOffice.getLatLng() != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nearestOffice.getLatLng(), DEFAULT_ZOOM + 1));
                    // Có thể mở InfoWindow của marker gần nhất ở đây nếu bạn có tham chiếu đến nó
                }
            }
        }
    }


    // Implement phương thức từ OnOfficeClickListener của Adapter
    @Override
    public void onOfficeClick(BankOffice office) {
        if (office != null && office.getLatLng() != null) {
            // Mở Google Maps để chỉ đường
            Uri gmmIntentUri = Uri.parse(String.format(Locale.US, "google.navigation:q=%f,%f",
                    office.getLatLng().latitude, office.getLatLng().longitude));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Toast.makeText(this, "Không tìm thấy ứng dụng Google Maps.", Toast.LENGTH_SHORT).show();
                // Có thể hiển thị một đường thẳng đơn giản trên bản đồ như một fallback
                if (currentUserLocation != null) {
                    drawFallbackPolyline(new LatLng(currentUserLocation.getLatitude(), currentUserLocation.getLongitude()), office.getLatLng());
                }
            }
            // Di chuyển camera đến vị trí ngân hàng và đóng bottom sheet
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(office.getLatLng(), DEFAULT_ZOOM + 2));
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        } else {
            Toast.makeText(this, "Không có thông tin vị trí cho văn phòng này.", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawRouteToOffice(LatLng origin, LatLng destination) {
        if (currentRoutePolyline != null) {
            currentRoutePolyline.remove(); // Xóa đường cũ nếu có
        }

        // Đây là phần GIẢ LẬP vẽ đường đi bằng Polyline đơn giản
        // Để vẽ đường đi thực tế, bạn cần gọi Google Directions API
        PolylineOptions polylineOptions = new PolylineOptions()
                .add(origin)
                .add(destination)
                .width(10) // Độ rộng của đường
                .color(Color.BLUE) // Màu của đường
                .geodesic(true); // Vẽ đường cong theo bề mặt trái đất

        currentRoutePolyline = mMap.addPolyline(polylineOptions);

        // Bạn cũng có thể di chuyển camera để thấy cả điểm đầu và điểm cuối
        // LatLngBounds.Builder builder = new LatLngBounds.Builder();
        // builder.include(origin);
        // builder.include(destination);
        // LatLngBounds bounds = builder.build();
        // int padding = 100; // padding in pixels
        // CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        // mMap.animateCamera(cu);

        Toast.makeText(this, "Đang hiển thị đường đi (giả lập)", Toast.LENGTH_SHORT).show();
        // TODO: Thay thế bằng việc gọi Google Directions API
        // 1. Tạo URL request đến Directions API
        // 2. Gửi request (ví dụ dùng Retrofit, Volley)
        // 3. Parse JSON response để lấy các điểm của polyline (thường là một chuỗi đã mã hóa)
        // 4. Giải mã chuỗi polyline (PolyUtil.decode() từ thư viện maps-utils)
        // 5. Thêm các điểm đã giải mã vào PolylineOptions và vẽ lên bản đồ
    }
}