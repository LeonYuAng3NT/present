package com.dubhacks.alms;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.dubhacks.alms.Model.Events;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.core.utilities.Pair;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener,
        GoogleMap.OnMarkerClickListener, GeoQueryEventListener {

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Location currentLocation;
    private DatabaseReference mDataEvents, mDataGeoFire;
    private FirebaseUser currentUser;
    private FirebaseAuth mAuth;
    private SlidingUpPanelLayout dragLayout;
    private static final int REQUEST_CODE = 101;
    private View zoomControls, locationButton;
    private RelativeLayout.LayoutParams rlp, params_zoom;
    private FloatingActionButton btnAdd;


    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        btnAdd = findViewById(R.id.fab_add);

        // ZoomControl is inside of RelativeLayout
        zoomControls = mapFragment.getView().findViewById(0x1);
        params_zoom = (RelativeLayout.LayoutParams) zoomControls.getLayoutParams();
        // move myLocation button
        locationButton = ((View) mapFragment.getView().findViewById(Integer.parseInt("1")).
                getParent()).findViewById(Integer.parseInt("2"));
        // and next place it, for example, on bottom right (as Google Maps app)
        rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();

        // connect to firebase
        mDataEvents = FirebaseDatabase.getInstance().getReference("Events");
        mDataGeoFire = FirebaseDatabase.getInstance().getReference("GeoFire");

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAddActivity();
            }
        });

        // retrieve current Location from the phone
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fetchLastLocation();

    }

    public void openAddActivity() {
        Intent intent = new Intent(MapActivity.this, AddEventActivity.class);
        startActivity(intent);
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
        final LatLng sanJoseLocation = new LatLng(37.3382082, -121.8863286);
        createLocationMarker(sanJoseLocation, "Health");

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
    private void createLocationMarker(LatLng location, String eventType ){
        switch(eventType) {
            case "Health":
                mMap.addMarker( new MarkerOptions()
                        .position(location)
                        .title(eventType)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_mask)));
                break;
            case "Clothe":
                mMap.addMarker( new MarkerOptions()
                        .position(location)
                        .title(eventType)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_clothe)));
                break;
            case "Food":
                mMap.addMarker( new MarkerOptions()
                        .position(location)
                        .title(eventType)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_meal)));
                break;
            default:
                mMap.addMarker( new MarkerOptions()
                        .position(location)
                        .title(eventType)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_box)));
        }
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

        locationCallback = new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(final LocationResult locationResult) {
                GeoFire geoFire = new GeoFire(mDataGeoFire);

                if (mMap != null) {
                    GeoQuery geoQuery = null;
                    if (geoQuery == null) {
                        geoQuery = geoFire.queryAtLocation(new GeoLocation(locationResult.getLastLocation().getLatitude(),
                                locationResult.getLastLocation().getLongitude()), 0.1f);
                    } else {
                        geoQuery.setCenter(new GeoLocation(locationResult.getLastLocation().getLatitude(),
                                locationResult.getLastLocation().getLongitude()));
                    }

                    geoQuery.addGeoQueryEventListener(MapActivity.this);

                    getEventsInRadiusUpdate(new GeoLocation(locationResult.getLastLocation().getLatitude(),
                            locationResult.getLastLocation().getLongitude()));

                }
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

    public void getEventsInRadiusUpdate(final GeoLocation location) {
        GeoFire geoFire = new GeoFire(mDataGeoFire);

        // get geoFire data within 25 kilometers
        GeoQuery geoQuery = null;

        if (geoQuery == null) {
            geoQuery = geoFire.queryAtLocation(location, 25);
        } else {
            geoQuery.setCenter(location);
        }

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
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
//                    currentLocation = location;
//                    usersData.child(mUID).child("currentLocation")
//                            .addValueEventListener(new ValueEventListener() {
//                                @Override
//                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                                    Double lat = dataSnapshot.child("0").getValue(Double.class);
//                                    Double lng = dataSnapshot.child("1").getValue(Double.class);
//
//                                    if (distance(lat, lng, currentLocation.getLatitude(), currentLocation.getLongitude()) > 10) {
//                                        usersData.child(mUID).child("currentLocation").child("0").setValue(currentLocation.getLatitude());
//                                        usersData.child(mUID).child("currentLocation").child("1").setValue(currentLocation.getLongitude());
//                                        map.clear();
//                                        listView.clear();
//                                    }
//                                }
//
//                                @Override
//                                public void onCancelled(@NonNull DatabaseError databaseError) {
//
//                                }
//                            });
                }

            }
        });


        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(final String key, GeoLocation location) {
                Query locationDataQuery = FirebaseDatabase.getInstance().getReference("Events").orderByKey().equalTo(key);
                locationDataQuery.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //The dataSnapshot should hold the actual data about the location
                        for (DataSnapshot s : dataSnapshot.getChildren()){
                            Events event  = s.getValue(Events.class);
                            double latitude = event.getL().get(0);
                            double longitude = event.getL().get(1);
                            List<String> categories = event.getCategories();
                            String category;
                            if (categories.size() > 1) {
                                category = "others";
                            } else {
                                category = categories.get(0);
                                Log.i("category", String.valueOf(categories.get(0)));
                            }

                            LatLng location = new LatLng(latitude, longitude);
                            mMap.addMarker(new MarkerOptions().position(location).title(s.getKey())
                                    .icon(BitmapDescriptorFactory.fromBitmap(getCategoryIcon(category))));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
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
        });
    }

    public Bitmap getCategoryIcon (String cat) {
        int height = 100;
        int width = 100;
        int icon = 0;

        if (cat.toLowerCase().equals("food")) {
            icon = R.mipmap.ic_food;
        } else if (cat.toLowerCase().equals("health care")) {
            icon =  R.mipmap.ic_essential;
        } else if (cat.toLowerCase().equals("essential")) {
            icon = R.mipmap.ic_essential;
        } else if (cat.toLowerCase().equals("clothes")) {
            icon = R.mipmap.ic_clothes;
        } else if (cat.toLowerCase().equals("others")) {
            icon = R.mipmap.ic_others;
        }

        BitmapDrawable bitmapdraw = (BitmapDrawable) ContextCompat.getDrawable(MapActivity.this, icon);
        Bitmap b = bitmapdraw.getBitmap();
        Bitmap marker = Bitmap.createScaledBitmap(b, width, height, false);

        return marker;
    }

    private Bitmap textToBitmap (String text) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(70);
        float baseline = -paint.ascent();
        int width = (int) (paint.measureText(text) + 0.5f);
        int height = (int) (baseline + paint.descent() + 0.5f);
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawText(text, 0, baseline, paint);
        return image;
    }
}