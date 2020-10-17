package com.dubhacks.alms;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.RelativeLayout;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener,
        GoogleMap.OnMarkerClickListener, GeoQueryEventListener {

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Location currentLocation;
    private FirebaseUser currentUser;
    private FirebaseAuth mAuth;
    private SlidingUpPanelLayout dragLayout;
    private static final int REQUEST_CODE = 101;
    private View zoomControls, locationButton;
    private RelativeLayout.LayoutParams rlp, params_zoom;


    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);


        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        // ZoomControl is inside of RelativeLayout
        zoomControls = mapFragment.getView().findViewById(0x1);
        params_zoom = (RelativeLayout.LayoutParams) zoomControls.getLayoutParams();
        // move myLocation button
        locationButton = ((View) mapFragment.getView().findViewById(Integer.parseInt("1")).
                getParent()).findViewById(Integer.parseInt("2"));
        // and next place it, for example, on bottom right (as Google Maps app)
        rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();

        // retrieve current Location from the phone
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fetchLastLocation();

    }


    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng curr = new LatLng(currentLocation.getLatitude(),
                currentLocation.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(curr));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(curr, 14.0f));

        if (fusedLocationProviderClient != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }

        mMap.setOnMarkerClickListener(this);

        mMap.setMyLocationEnabled(true);
        UiSettings settings = mMap.getUiSettings();
        settings.setRotateGesturesEnabled(true);
        settings.setScrollGesturesEnabled(true);
        settings.setZoomControlsEnabled(true);
        settings.setZoomGesturesEnabled(true);

        // move myLocation button
        View locationButton = ((View) mapFragment.getView().findViewById(Integer.parseInt("1")).
                getParent()).findViewById(Integer.parseInt("2"));
        // and next place it, for example, on bottom right (as Google Maps app)
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        // position on right bottom
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        rlp.setMargins(0, 0, 30, 300);

//        getEventsInRadius();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fetchLastLocation();
                }
                break;
        }
    }

    private void fetchLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
        }

        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    buildLocationRequest();
                    buildLocationCallBack();
                    currentLocation = location;
                    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                    mapFragment.getMapAsync(MapActivity.this);
                }
            }
        });

    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);
    }

    /**
     * This function takes the user location and check if user is near the event or not
     */
    public void buildLocationCallBack() {
//        locationCallback = new com.google.android.gms.location.LocationCallback() {
//            @Override
//            public void onLocationResult(final LocationResult locationResult) {
//                GeoFire geoFire = new GeoFire(mDataGeoFire);
//
//                if (mMap != null) {
//                    GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(locationResult.getLastLocation().getLatitude(),
//                            locationResult.getLastLocation().getLongitude()), 0.1f);
//                            locationResult.getLastLocation().getLongitude()), 0.1f);
//
//                    geoQuery.addGeoQueryEventListener(MapsActivity.this);
//                }
//            }
//        };

        locationCallback = new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(final LocationResult locationResult) {
//                GeoFire geoFire = new GeoFire(geoFireData);
//
//                if (mMap != null) {
//                    GeoQuery geoQuery = null;
//                    if (geoQuery == null) {
//                        geoQuery = geoFire.queryAtLocation(new GeoLocation(locationResult.getLastLocation().getLatitude(),
//                                locationResult.getLastLocation().getLongitude()), 0.1f);
//                    } else {
//                        geoQuery.setCenter(new GeoLocation(locationResult.getLastLocation().getLatitude(),
//                                locationResult.getLastLocation().getLongitude()));
//                    }
//
//                    geoQuery.addGeoQueryEventListener(MapActivity.this);
//
//                    getEventsInRadiusUpdate(new GeoLocation(locationResult.getLastLocation().getLatitude(),
//                            locationResult.getLastLocation().getLongitude()));
//
//                }
            }
        };
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {

    }

    @Override
    public void onKeyExited(String key) {

    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {

    }

    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {

    }
}