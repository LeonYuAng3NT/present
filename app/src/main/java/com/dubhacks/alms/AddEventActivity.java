package com.dubhacks.alms;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import com.dubhacks.alms.Model.Events;
import com.dubhacks.alms.Model.GeoEvents;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class AddEventActivity extends AppCompatActivity {

    private EditText name, url, startTime, date, description;
    private TextInputLayout locationLayOut, editNameLayOut, urlLayOut, dateLayOut, startTimeLayOut, descriptionLayOut;
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
    private DatePickerDialog datePickerDialog;
    private Calendar calendar;
    private List<String> categories = new ArrayList<>();
    private List<Double> l = new ArrayList<>();
    private int currentHour, currentMinute;
    private String nameStr, urlStr, startStr, dateStr, descriptionStr, locationStr;
    private boolean error = true;

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

        locationLayOut = findViewById(R.id.locationLayOut);
        editNameLayOut = findViewById(R.id.editNameLayOut);
        urlLayOut = findViewById(R.id.urlLayOut);
        startTimeLayOut = findViewById(R.id.startTimeLayOut);
        dateLayOut = findViewById(R.id.dateLayOut);
        descriptionLayOut = findViewById(R.id.descriptionLayOut);
        btnAddEvent = findViewById(R.id.btnAdd);

        calendar = Calendar.getInstance();
        currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        currentMinute = calendar.get(Calendar.MINUTE);

        // connect to firebase
        mDataEvent = FirebaseDatabase.getInstance().getReference("Events");
        mDataGeoFire = FirebaseDatabase.getInstance().getReference("GeoFire");
        mDataUsers = FirebaseDatabase.getInstance().getReference("Users");

        // get API key
        String apiKey = "AIzaSyC5tVRK4noWWEw7LgrfRpZ2LvM_otKNt7A";

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

        date.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    final Calendar cldr = Calendar.getInstance();
                    int day = cldr.get(Calendar.DAY_OF_MONTH);
                    int month = cldr.get(Calendar.MONTH);
                    int year = cldr.get(Calendar.YEAR);
                    datePickerDialog = new DatePickerDialog(AddEventActivity.this, new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            date.setText(String.format("%02d/%02d/%04d", (monthOfYear + 1), dayOfMonth, year));
                        }
                    }, year, month, day);
                    datePickerDialog.show();
                }
            }
        });

        date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar cldr = Calendar.getInstance();
                int day = cldr.get(Calendar.DAY_OF_MONTH);
                int month = cldr.get(Calendar.MONTH);
                int year = cldr.get(Calendar.YEAR);
                datePickerDialog = new DatePickerDialog(AddEventActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        date.setText(String.format("%02d/%02d/%04d", (monthOfYear + 1), dayOfMonth, year));
                    }
                }, year, month, day);
                datePickerDialog.show();
            }
        });

        //create a list of closed day
        String[] cats = new String[]{"Food", "HealthCare",
                "Essential", "Clothes"};

        // display list of closed days in chip
        for(String cat: cats) {
            catDisplay(cat);
        }

        //button register function
        btnAddEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateData() == false) {
                    saveInformation();
                    openMapsActivity();
                }

            }
        });
    }

    public void openMapsActivity() {
        Intent intent = new Intent(AddEventActivity.this, MapActivity.class);
        startActivity(intent);
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

        chip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b == true) {
                    categories.add(((ChipDrawable) chip.getChipDrawable()).getText().toString());
                } else {
                    categories.remove(((ChipDrawable) chip.getChipDrawable()).getText().toString());
                }
            }
        });

        chipGroup.addView(chip);
    }



    private boolean validateData() {
        nameStr = name.getText().toString().trim();
        startStr = startTime.getText().toString();
        dateStr = date.getText().toString();
        descriptionStr = description.getText().toString();

        if (savedPlace == null) {
            locationLayOut.setError("Location can't be empty");
            error = true;
        } else {
            locationLayOut.setError(null);
            error = false;
        }

        if (TextUtils.isEmpty(nameStr) || nameStr == null) {
            editNameLayOut.setError("Name can't be empty");
            error = true;
        } else {
            editNameLayOut.setError(null);
            error = false;
        }

        if (TextUtils.isEmpty(startStr) || startStr == null) {
            startTimeLayOut.setError("Start time can't be empty");
            error = true;
        } else {
            startTimeLayOut.setError(null);
            error = false;
        }

        if (TextUtils.isEmpty(dateStr) || dateStr == null) {
            dateLayOut.setError("Date can't be empty");
            error = true;
        } else {
            dateLayOut.setError(null);
            error = false;
        }

        if (TextUtils.isEmpty(descriptionStr) || descriptionStr == null) {
            descriptionLayOut.setError("Description can't be empty");
            error = true;
        } else {
            descriptionLayOut.setError(null);
            error = false;
        }

        return error;
    }

    private void saveInformation() {
        urlStr = url.getText().toString().trim();

        l.add(savedLat);
        l.add(savedLong);
        g = geoHash.getGeoHashString();
        if (urlStr == null || urlStr.isEmpty()) {
            urlStr = "";
        }
        Events event = new Events(nameStr, namePlace, savedPlace,
                l, urlStr, startStr, dateStr, categories, descriptionStr);
        GeoEvents geoEvents = new GeoEvents(l, g);
        DatabaseReference addData = mDataEvent.push();
        String key = addData.getKey();
        // add event to database
        addData.setValue(event);
        // add event to user account
//        mDataUsers.child(currentUser.getUid()).child("eventCreated").child(key).setValue(true);
        // add geo information of the events into geo Fire
        mDataGeoFire.child(key).setValue(geoEvents);
    }


}