package com.dubhacks.alms;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TimePicker;

import com.firebase.geofire.core.GeoHash;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.Calendar;

public class AddEventActivity extends AppCompatActivity {

    private EditText name, url, startTime, date, description;
    private TextInputLayout locationLayOut, editNameLayOut, urlLayOut, dateLayOut, descriptionLayOut;
    private ChipGroup chipGroup;
    private Button btnAddEvent;
    private DatabaseReference mDataEvent, mDataGeoFire, mDataScore, mDataUsers;
    private FirebaseUser currentUser;
    private FirebaseAuth mAuth;
    private AutocompleteSupportFragment autocompleteFragment;
    private String savedPlace = null, g, amPm, namePlace;
    private Double savedLat, savedLong;
    private GeoHash geoHash;
    private TimePickerDialog timePickerDialog;
    private Calendar calendar;
    private int currentHour, currentMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        name = findViewById(R.id.editName);
        url = findViewById(R.id.url);
        startTime = findViewById(R.id.startTime);
        date = findViewById(R.id.date);
        description = findViewById(R.id.description);
        chipGroup = findViewById(R.id.chipGroup);

        calendar = Calendar.getInstance();
        currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        currentMinute = calendar.get(Calendar.MINUTE);

        // connect to firebase
        mDataEvent = FirebaseDatabase.getInstance().getReference("Events");
        mDataGeoFire = FirebaseDatabase.getInstance().getReference("GeoFire");
        mDataUsers = FirebaseDatabase.getInstance().getReference("Users");

        // get API key
        String apiKey = "AIzaSyC9Kkq_pgVRQmH1BxHZHdRmHFXljNbsB1k";

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }

        // Initialize the AutocompleteSupportFragment.
        autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME));
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                namePlace = String.valueOf(place.getName());
                savedPlace = String.valueOf(place.getAddress());
                savedLat = place.getLatLng().latitude;
                savedLong = place.getLatLng().longitude;
                geoHash = new GeoHash(place.getLatLng().latitude, place.getLatLng().longitude);
            }

            @Override
            public void onError(Status status) {
                Log.i("TAG", "An error occurred: " + status);
            }
        });

        // make sure the savedPlace value is updated when clear Text in the place autocomplete
        View clearButton = autocompleteFragment.getView().findViewById(R.id.places_autocomplete_clear_button);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autocompleteFragment.setText("");
                savedPlace = null;
            }
        });


        // one click handle function
        startTime.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    timePickerDialog = new TimePickerDialog(AddEventActivity.this, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            if (hourOfDay >= 12) {
                                amPm = "PM";
                            } else {
                                amPm = "AM";
                            }
                            startTime.setText(String.format("%02d:%02d", hourOfDay, minute) + amPm);
                        }
                    }, currentHour, currentMinute, false);

                    timePickerDialog.show();
                }
            }
        });

        // double click handle
        startTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timePickerDialog = new TimePickerDialog(AddEventActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        if (hourOfDay >= 12) {
                            amPm = "PM";
                        } else {
                            amPm = "AM";
                        }
                        startTime.setText(String.format("%02d:%02d", hourOfDay, minute) + amPm);
                    }
                }, currentHour, currentMinute, false);

                timePickerDialog.show();
            }
        });

        //create a list of closed day
        String[] cats = new String[]{"Food", "HealthCare",
                "Essential", "Clothes"};

        // display list of closed days in chip
        for(String cat: cats) {
            catDisplay(cat);
        }
    }

    private void catDisplay(final String tag) {
        final Chip chip = new Chip(AddEventActivity.this);
        ChipDrawable chipDrawable = ChipDrawable.createFromAttributes(AddEventActivity.this,
                null, 0, R.style.Widget_MaterialComponents_Chip_Entry);
        chip.setChipDrawable(chipDrawable);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setCloseIconVisible(false);
        chip.setPadding(60, 10, 60,10);
        chip.setText(tag);

        chipGroup.addView(chip);
    }
}