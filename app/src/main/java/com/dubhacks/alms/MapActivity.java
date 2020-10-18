package com.dubhacks.alms;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dubhacks.alms.Adapter.EventInfoAdapter;
import com.dubhacks.alms.Adapter.eventPairsLVAdapter;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    private FloatingActionButton btnAdd, btnUser;
    private TextView name, location;
    private ListView  eventsList, listInfo;
    private ChipGroup chipGroup;
    private PopupWindow pw;
    private List<Pair<String, Events>> listView = new ArrayList<>();
    private Button btnLoadMore;
    private ImageButton btnList;
    private View ftView;
    private eventPairsLVAdapter adapter;
    private Handler mHandler;
    private int mCurrentPage = 1;
    private int mItemPerRow = 6;
    private int countList = 0;
    public boolean isLoading = false;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        //getting the logged in owner info
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // check if current User is existed or not
        btnAdd = findViewById(R.id.fab_add);
        btnUser = findViewById(R.id.fab_user);
        if (currentUser != null){
            String email = currentUser.getEmail().charAt(0) + "";
            btnUser.setImageBitmap(textToBitmap(email.toUpperCase()));
            btnUser.setVisibility(View.VISIBLE);
            btnAdd.setVisibility(View.VISIBLE);
        } else {
            btnAdd.setVisibility(View.GONE);
            btnUser.setVisibility(View.GONE);
        }

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        btnAdd = findViewById(R.id.fab_add);
        name = findViewById(R.id.eventName);
        location = findViewById(R.id.eventLocation);
        listInfo = findViewById(R.id.listInfo);
        dragLayout = findViewById(R.id.sliding_layout);
        chipGroup = findViewById(R.id.chipGroup);
        btnList = findViewById(R.id.btnList);

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

        // btnUser function
        btnUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog alertDialog = new AlertDialog.Builder(MapActivity.this).create();
                alertDialog.setTitle("Sign Out");
                alertDialog.setMessage("Do you want to sign out?");
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mAuth.signOut();
                                SendUserToLoginActivity();
                            }
                        });
                alertDialog.show();
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAddActivity();
            }
        });

        btnList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiatePopupWindow(v);
            }
        });

        // dragLayout click function
        dragLayout.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addBtnMove();
                dragLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
            }
        });

        // retrieve current Location from the phone
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fetchLastLocation();

    }

    public void addBtnMove() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        btnAdd.setY(height - dpToPx(MapActivity.this, 126));
        zoomControls.setY(height - dpToPx(MapActivity.this, 126));
        locationButton.setY(height - dpToPx(MapActivity.this, 176));
    }

    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float sizePx = (dp * displayMetrics.density);
        return Math.round(sizePx);
    }

    @Override
    public void onBackPressed() {
        if (currentUser == null) {
            super.onBackPressed();
        }
    }

    private void SendUserToLoginActivity() {
        Intent loginIntent = new Intent(MapActivity.this, LoginActivity.class);
        startActivity(loginIntent);
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
    public boolean onMarkerClick(Marker marker){
        getEventInfo(marker.getTitle());
        return true;
    }

    public void getEventInfo(final String eventKey) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        btnAdd.setY(height - dpToPx(MapActivity.this, 212));
        zoomControls.setY(height - dpToPx(MapActivity.this, 212));
        locationButton.setY(height - dpToPx(MapActivity.this, 262));

        dragLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        ViewGroup.LayoutParams params = listInfo.getLayoutParams();
        params.height = 1000;
        listInfo.setLayoutParams(params);
        // display information from markers title as event id
        final Query locationDataQuery = FirebaseDatabase.getInstance().getReference("Events")
                .orderByKey().equalTo(eventKey);
        locationDataQuery.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot s : dataSnapshot.getChildren()) {
                    final Events event = s.getValue(Events.class);
                    String nametoSetName = event.name.length() >= 30
                            ? event.name.substring(0,30)+"...":event.name;
                    name.setText(nametoSetName);

                    String nametoSetLoc = event.locationName.length() >=35
                            ? event.locationName.substring(0,35)+"...":event.locationName;
                    location.setText(nametoSetLoc);

                    chipGroup.removeAllViews();
                    for (String tag : event.categories) {
                        chipDisplay(tag);
                    }

                    // get name, location and category from firebase
                    String address = "", url = "", hours = "", description ="", date ="";
                    address = event.address;

                    if (event.url == null || event.url.isEmpty()) {
                        url = "";
                    } else {
                        if (!event.url.startsWith("www.")) {
                            url = "https://www." + event.url.toLowerCase();
                        } else if (!event.url.startsWith("https://")){
                            url = "https://" + event.url.toLowerCase();
                        }
                    }

                    hours = event.startTime;
                    date = event.date;
                    description = event.description;

                    if (event.url.equals("")) {
                        final String[] eventInfo = {address, hours, date, description};
                        Integer[] imgid = {
                                R.drawable.ic_location,
                                R.drawable.ic_hours,
                                R.drawable.ic_date,
                                R.drawable.ic_description,
                        };

                        EventInfoAdapter adapter = new EventInfoAdapter(
                                MapActivity.this, eventInfo, imgid);
                        listInfo.setAdapter(adapter);

                        listInfo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view,
                                                    int position, long id) {
                                if (position == 0) {
                                    String selectedItem = "http://maps.google.co.in/maps?q=" + eventInfo[position];
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(selectedItem));
                                    startActivity(intent);
                                }
                            }
                        });


                    } else {
                        final String[] eventInfo = {address, url, hours, date, description};
                        Integer[] imgid = {
                                R.drawable.ic_location,
                                R.drawable.ic_url,
                                R.drawable.ic_hours,
                                R.drawable.ic_date,
                                R.drawable.ic_description,
                        };

                        EventInfoAdapter adapter = new EventInfoAdapter(
                                MapActivity.this, eventInfo, imgid);
                        listInfo.setAdapter(adapter);

                        listInfo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view,
                                                    int position, long id) {
                                if (position == 1) {
                                    String selectedItem = eventInfo[position];
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(selectedItem));
                                    startActivity(intent);
                                } else if (position == 0) {
                                    String selectedItem = "http://maps.google.co.in/maps?q=" + eventInfo[position];
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(selectedItem));
                                    startActivity(intent);
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void initiatePopupWindow(View v) {
        try {
            //We need to get the instance of the LayoutInflater, use the context of this activity
            LayoutInflater inflater = (LayoutInflater) MapActivity.this
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //Inflate the view from a predefined XML layout
            View layout = inflater.inflate(R.layout.events_list,
                    (ViewGroup) findViewById(R.id.popup_layout));
            // create all screen PopupWindow
            pw = new PopupWindow(layout, LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT, true);
            // display the popup in the center
            pw.showAtLocation(v, Gravity.CENTER, 0, 0);

            Log.i("List", String.valueOf(listView));

            // sort the list by distance
            Collections.sort(listView, new Comparator<Pair<String, Events>>() {
                public int compare(Pair<String, Events> event1, Pair<String, Events> event2) {
                    @SuppressLint("RestrictedApi") double lat1 = event1.getSecond().getL().get(0);
                    @SuppressLint("RestrictedApi") double lon1 = event1.getSecond().getL().get(1);
                    @SuppressLint("RestrictedApi") double lat2 = event2.getSecond().getL().get(0);
                    @SuppressLint("RestrictedApi") double lon2 = event2.getSecond().getL().get(1);

                    double distanceToPlace1 = distance(currentLocation.getLatitude(),
                            currentLocation.getLongitude(), lat1, lon1);
                    double distanceToPlace2 = distance(currentLocation.getLatitude(),
                            currentLocation.getLongitude(), lat2, lon2);
                    if (distanceToPlace1 > distanceToPlace2) return 1;
                    if (distanceToPlace1 < distanceToPlace2) return -1;
                    return 0;
                }
            });

            ImageButton mapButton = layout.findViewById(R.id.btnList);
            mapButton.setOnClickListener(mapView);

            eventsList = layout.findViewById(R.id.eventsList);
            ftView = inflater.inflate(R.layout.footer_view, null);
            btnLoadMore = layout.findViewById(R.id.btnLoadMore);
            mHandler = new MyHandler();
            mCurrentPage = 1;
            countList = 0;
            adapter = new eventPairsLVAdapter(this, getData(mCurrentPage), currentLocation);
            eventsList.setAdapter(adapter);

            eventsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @SuppressLint("RestrictedApi")
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    pw.dismiss();
                    Log.i("key", listView.get(position).getFirst());
                    getEventInfo(listView.get(position).getFirst());
                }
            });

            eventsList.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    //Check when scroll to last item in listview, in this tut, init data in listview = 7 item

                    if (view.getLastVisiblePosition() == totalItemCount - 1 && listView.size() >= 6 && isLoading == false) {

                        btnLoadMore.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mCurrentPage++;
                                Thread thread = new ThreadGetMoreData(mCurrentPage);
                                //Start thread
                                thread.start();
                            }
                        });
                    }
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private View.OnClickListener mapView = new View.OnClickListener() {
        public void onClick(View v) {
            pw.dismiss();
        }
    };

    public class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    //Add loading view during search processing
                    eventsList.addFooterView(ftView);
                    break;
                case 1:
                    //Update data adapter and UI
                    adapter.addListItemToAdapter((ArrayList<Pair<String, Events>>) msg.obj);
                    //Remove loading view after update listview
                    eventsList.removeFooterView(ftView);
                    isLoading = false;
                    break;
                default:
                    break;
            }
        }
    }

    public class ThreadGetMoreData extends Thread {

        private int currentPage;

        public ThreadGetMoreData(int currentPage) {
            this.currentPage = currentPage;
        }

        @Override
        public void run() {

            if (countList <= listView.size()) {
                //Add footer view after get data
                mHandler.sendEmptyMessage(0);
                //Search more data
                ArrayList<Pair<String, Events>> lstResult = getData(currentPage);
                //Delay time to show loading footer when debug, remove it when release
                Log.i("current", String.valueOf(lstResult));
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //Send the result to Handle
                Message msg = mHandler.obtainMessage(1, lstResult);
                mHandler.sendMessage(msg);
            }
        }
    }


    private ArrayList<Pair<String, Events>> getData(int page) {
        List<Pair<String, Events>> subList = new ArrayList<>();
        for (int i = 0; i < mItemPerRow; i++) {
            countList++;
            if (countList <= listView.size()) {
                subList.add(listView.get(i + ((page - 1) * mItemPerRow)));
            }
        }
        return new ArrayList<>(subList);
    }

    private void chipDisplay(final String tag) {
        final Chip chip = new Chip(MapActivity.this);
        ChipDrawable chipDrawable = ChipDrawable.createFromAttributes(MapActivity.this,
                null, 0, R.style.Widget_MaterialComponents_Chip_Entry);
        chip.setChipDrawable(chipDrawable);
        chip.setCheckable(false);
        chip.setClickable(false);
        chip.setCloseIconVisible(false);
        chip.setPadding(60, 10, 60, 10);

        switch (tag.toLowerCase()) {
            case "food":
                chip.setChipIconResource(R.mipmap.ic_food);
                break;
            case "health care":
                chip.setChipIconResource(R.mipmap.ic_health);
                break;
            case "clothes":
                chip.setChipIconResource(R.mipmap.ic_clothes);
                break;
            case "essential":
                chip.setChipIconResource(R.mipmap.ic_essential);
                break;
        }

        String tagU = tag.substring(0, 1).toUpperCase()
                + tag.substring(1);
        chip.setText(tagU);
        chipGroup.addView(chip);
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
                    currentLocation = location;
                    Double lat = currentLocation.getLatitude();
                    Double lng = currentLocation.getLongitude();
                    if (distance(lat, lng, currentLocation.getLatitude(), currentLocation.getLongitude()) > 10) {
                        mMap.clear();
                        listView.clear();
                    }
                }

            }
        });


        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(final String key, GeoLocation location) {
                Query locationDataQuery = FirebaseDatabase.getInstance().getReference("Events").orderByKey().equalTo(key);
                locationDataQuery.addValueEventListener(new ValueEventListener() {
                    @SuppressLint("RestrictedApi")
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //The dataSnapshot should hold the actual data about the location
                        for (DataSnapshot s : dataSnapshot.getChildren()){
                            Events event  = s.getValue(Events.class);
                            double latitude = event.getL().get(0);
                            double longitude = event.getL().get(1);
                            List<String> categories = event.getCategories();
                            listView.add(new Pair<>(s.getKey(), event));
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

    private static double distance(double lat1, double lon1, double lat2, double lon2) {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        } else {
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;
            return (dist);
        }
    }
}