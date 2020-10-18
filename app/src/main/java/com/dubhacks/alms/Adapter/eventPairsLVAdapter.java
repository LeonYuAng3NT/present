package com.dubhacks.alms.Adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.dubhacks.alms.Model.Events;
import com.dubhacks.alms.R;
import com.google.firebase.database.core.utilities.Pair;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class eventPairsLVAdapter extends ArrayAdapter<Pair<String, Events>> {
    private final Activity context;
    private final List<Pair<String, Events>> eventList;
    private final Location currentLocation;

    public eventPairsLVAdapter(Activity context,  List<Pair<String, Events>> eventList, Location currentLocation) {
        super(context, R.layout.event, eventList);
        this.context = context;
        this.eventList = eventList;
        this.currentLocation = currentLocation;
    }

    public void addListItemToAdapter(ArrayList<Pair<String, Events>> list) {
        //Add list to current array list of data
        eventList.addAll(list);

        //Notify UI
        this.notifyDataSetChanged();
    }
    @Override
    public int getCount() {
        return eventList.size();
    }

    @Override
    public Pair<String, Events> getItem(int position) {
        return eventList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    @SuppressLint("RestrictedApi")
    public View getView(int position, View view, ViewGroup parent) {

        // connect to xml files
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.event_list, null,true);

        TextView eventName = rowView.findViewById(R.id.nameList);
        TextView eventDistance = rowView.findViewById(R.id.distance);
        TextView eventTime = rowView.findViewById(R.id.eventTime);
        TextView eventLocation = rowView.findViewById(R.id.eventLocation);
        ImageView iconView = rowView.findViewById(R.id.icon);

        // calculate distance from current location to the event
        @SuppressLint("RestrictedApi") double dist = distance(currentLocation.getLatitude(), currentLocation.getLongitude(),
                eventList.get(position).getSecond().getL().get(0), eventList.get(position).getSecond().getL().get(1));
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);


        Log.i("List", String.valueOf(eventList.get(position).getSecond().name));
        eventName.setText(eventList.get(position).getSecond().name);
        eventDistance.setText(df.format(dist) +" mi");

        String category;
        if (eventList.get(position).getSecond().categories.size() > 1) {
            category = "others";
        } else {
            category = eventList.get(position).getSecond().categories.get(0);
        }

        iconView.setImageBitmap(getCategoryIcon(category));
        eventTime.setText("Date: " + eventList.get(position).getSecond().date + "                    "
                + "Start time: "+ eventList.get(position).getSecond().startTime);
        eventLocation.setText(eventList.get(position).getSecond().locationName);

        return rowView;

    }

    /**
     * This function calculates the distance between two points based on their longitude and latitude
     * @param lat1 double
     * @param lon1 double
     * @param lat2 double
     * @param lon2 double
     * @return dist double
     */
    private static double distance(double lat1, double lon1, double lat2, double lon2) {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        }
        else {
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;
            return (dist);
        }
    }


    /**
     * This function gets icon depending on the category input
     * @param cat String
     * @return marker bitmap
     */
    public Bitmap getCategoryIcon (String cat) {
        int height = 100;
        int width = 100;
        int icon = 0;

        if (cat.toLowerCase().equals("food")) {
            icon = R.mipmap.ic_food;
        } else if (cat.toLowerCase().equals("health care")) {
            icon =  R.mipmap.ic_health;
        } else if (cat.toLowerCase().equals("essential")) {
            icon = R.mipmap.ic_essential;
        } else if (cat.toLowerCase().equals("clothes")) {
            icon = R.mipmap.ic_clothes;
        } else if (cat.toLowerCase().equals("others")) {
            icon = R.mipmap.ic_others;
        }

        BitmapDrawable bitmapdraw = (BitmapDrawable) ContextCompat.getDrawable(context, icon);
        Bitmap b = bitmapdraw.getBitmap();
        Bitmap marker = Bitmap.createScaledBitmap(b, width, height, false);

        return marker;
    }

    /**
     * This function creates red stroke around icon
     * @param bitmap bitmap
     * @return output bitmap
     */
    private Bitmap addBorder(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int radius = Math.min(h / 2, w / 2);
        Bitmap output = Bitmap.createBitmap(w + 8, h + 8, Bitmap.Config.ARGB_8888);

        Paint p = new Paint();
        p.setAntiAlias(true);

        Canvas c = new Canvas(output);
        c.drawARGB(0, 0, 0, 0);
        p.setStyle(Paint.Style.FILL);

        c.drawCircle((w / 2) + 4, (h / 2) + 4, radius, p);

        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        c.drawBitmap(bitmap, 4, 4, p);
        p.setXfermode(null);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(Color.RED);
        p.setStrokeWidth(5);
        c.drawCircle((w / 2) + 4, (h / 2) + 4, radius, p);

        return output;
    }


    /**
     * This function decreases the opacity of the icon bitmap
     * @param bitmap Bitmap
     * @param opacity int
     * @return Bitmap
     */
    private Bitmap adjustOpacity(Bitmap bitmap, int opacity)
    {
        Bitmap mutableBitmap = bitmap.isMutable()
                ? bitmap
                : bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        int colour = (opacity & 0xFF) << 24;
        canvas.drawColor(colour, PorterDuff.Mode.DST_IN);
        return mutableBitmap;
    }
}