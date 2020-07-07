package com.example.maskapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements NaverMap.OnMapClickListener, OnMapReadyCallback, Overlay.OnClickListener, NaverMap.OnCameraChangeListener, NaverMap.OnCameraIdleListener {
    private static final int ACCESS_LOCATION_PERMISSION_REQUEST_CODE = 100;

    private FusedLocationSource locationSource;
    private NaverMap naverMap;
    private InfoWindow infoWindow;
    private List<Marker> markerList = new ArrayList<Marker>();
    private boolean isCameraAnimated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 네이버맵 Async로 호출
        MapFragment mapFragment = (MapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return true;

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.show_list:
                Intent intent = new Intent(this, StoreActivity.class);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        // 현재위치
        locationSource = new FusedLocationSource(this, ACCESS_LOCATION_PERMISSION_REQUEST_CODE);
        naverMap.setLocationSource(locationSource);
        // 현재위치 렌더링 UI
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(true);
        // 맵 위치 변경 시 리스너
        naverMap.addOnCameraChangeListener(this);
        naverMap.addOnCameraIdleListener(this);
        // 맵 중앙위치
        LatLng mapCenter = naverMap.getCameraPosition().target;
        fetchStoreSale(mapCenter.latitude, mapCenter.longitude, 5000);

        infoWindow = new InfoWindow();
        // 인포윈도우 어댑터
        infoWindow.setAdapter(new InfoWindow.DefaultViewAdapter(this) {

            @NonNull
            @Override
            protected View getContentView(@NonNull InfoWindow infoWindow) {
                Marker marker = infoWindow.getMarker();
                Store store = (Store) marker.getTag();
                View view = View.inflate(MainActivity.this, R.layout.view_info_window, null);
                ((TextView) view.findViewById(R.id.name)).setText(store.name);
                if ("plenty".equalsIgnoreCase(store.remain_stat)) {
                    ((TextView) view.findViewById(R.id.stock)).setText("100개 이상");
                } else if ("some".equalsIgnoreCase(store.remain_stat)) {
                    ((TextView) view.findViewById(R.id.stock)).setText("30개 이상 100개 미만");
                } else if ("few".equalsIgnoreCase(store.remain_stat)) {
                    ((TextView) view.findViewById(R.id.stock)).setText("2개 이상 30개 미만");
                } else if ("empty".equalsIgnoreCase(store.remain_stat)) {
                    ((TextView) view.findViewById(R.id.stock)).setText("1개 이하");
                } else if ("break".equalsIgnoreCase(store.remain_stat)) {
                    ((TextView) view.findViewById(R.id.stock)).setText("판매중지");
                } else {
                    ((TextView) view.findViewById(R.id.stock)).setText(null);
                }
                ((TextView) view.findViewById(R.id.time)).setText("입고 " + store.stock_at);
                return view;
            }
        });

    }
    // 개인정보 위치 허가여부
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case ACCESS_LOCATION_PERMISSION_REQUEST_CODE:
                locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults);
                return;
        }
    }
    // 마스크 판매장소 API의 데이터 가져오기
    private void fetchStoreSale(double lat, double lng, int m) {
        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://8oi9s0nnth.apigw.ntruss.com").addConverterFactory(GsonConverterFactory.create()).build();
        MaskApi maskApi = retrofit.create(MaskApi.class);
        maskApi.getStoresByGeo(lat, lng, m).enqueue(new Callback<StoreSaleResult>() {

            @Override
            public void onResponse(Call<StoreSaleResult> call, Response<StoreSaleResult> response) {
                if (response.code() == 200) {
                    StoreSaleResult result = response.body();
                    updateMapMarkers(result);
                }
            }

            @Override
            public void onFailure(Call<StoreSaleResult> call, Throwable t) {

            }
        });
    }
    // API에서 가져온 가게 좌표마다 marker 띄움
    private void updateMapMarkers(StoreSaleResult result) {
        resetMarkerList();
        if (result.stores != null && result.stores.size() > 0) {
            for (Store store : result.stores) {
                Marker marker = new Marker();
                marker.setTag(store);
                marker.setPosition(new LatLng(store.lat, store.lng));
                if ("plenty".equalsIgnoreCase(store.remain_stat)) {
                    marker.setIcon(OverlayImage.fromResource(R.drawable.marker_green));
                } else if ("some".equalsIgnoreCase(store.remain_stat)) {
                    marker.setIcon(OverlayImage.fromResource(R.drawable.marker_yellow));
                } else if ("few".equalsIgnoreCase(store.remain_stat)) {
                    marker.setIcon(OverlayImage.fromResource(R.drawable.marker_red));
                } else {
                    marker.setIcon(OverlayImage.fromResource(R.drawable.marker_gray));
                }
                marker.setAnchor(new PointF(0.5f, 1.0f));
                marker.setMap(naverMap);
                marker.setOnClickListener(this);
                markerList.add(marker);
            }
        }
    }
    // 마커리스트 지우고 리셋
    private void resetMarkerList() {
        if (markerList != null && markerList.size() > 0) {
            for (Marker marker : markerList) {
                marker.setMap(null);
            }
            markerList.clear();
        }
    }
    // 클릭 시 마커 띄우기
    @Override
    public boolean onClick(@NonNull Overlay overlay) {
        if (overlay instanceof Marker) {
            Marker marker = (Marker) overlay;
            if (marker.getInfoWindow() != null) {
                infoWindow.close();
            } else {
                infoWindow.open(marker);
            }
            return true;
        }
        return false;
    }
    // 지도가 이동 시에 이동 중임을 확인
    @Override
    public void onCameraChange(int reason, boolean animated) {
        isCameraAnimated = animated;
    }
    // 지도가 멈춘 위치의 좌표 API로 URL 포스트
    @Override
    public void onCameraIdle() {
        if (isCameraAnimated) {
            LatLng mapCenter = naverMap.getCameraPosition().target;
            fetchStoreSale(mapCenter.latitude, mapCenter.longitude, 5000);
        }
    }
    // 마커 클릭 시 사라짐
    @Override
    public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
        if (infoWindow.getMarker() != null) {
            infoWindow.close();
        }
    }
}