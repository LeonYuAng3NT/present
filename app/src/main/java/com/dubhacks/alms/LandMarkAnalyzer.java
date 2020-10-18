package com.dubhacks.alms;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.os.Build;
import android.util.SparseIntArray;
import android.view.Surface;

import com.dubhacks.alms.Model.Events;
import com.dubhacks.alms.Model.GeoEvents;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import static android.content.Context.CAMERA_SERVICE;

public class LandMarkAnalyzer  {

    private final DatabaseReference mDataEvent = FirebaseDatabase.getInstance().getReference("Events");
    private final DatabaseReference  mDataGeoFire = FirebaseDatabase.getInstance().getReference("GeoFire");



    public void analyze(@NonNull Bitmap bitmap) {

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        // Pass image to an ML Kit Vision API
        ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
        labeler.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                    @Override
                    public void onSuccess(List<ImageLabel> labels) {

                        Events newEvent =  new Events();
                        newEvent.setLocationName(labels.get(0).getText());
                        newEvent.setName("Event on " + labels.get(0).getText());
                        DatabaseReference addData = mDataEvent.push();
                        String key = addData.getKey();
                        // add event to database
                        addData.setValue(newEvent);
                        //GeoEvents geoEvents = new GeoEvents(l, g);
                        //mDataGeoFire.child(key).setValue(geoEvents);


                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        // ...
                    }
                });
    }
}
