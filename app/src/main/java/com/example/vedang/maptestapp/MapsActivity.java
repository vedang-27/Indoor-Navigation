package com.example.vedang.maptestapp;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineString;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.data.geojson.GeoJsonPoint;
import com.google.maps.android.data.geojson.GeoJsonPointStyle;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import math.geom2d.line.Line2D;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,BeaconConsumer,RangeNotifier {

    private GoogleMap mMap;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private BeaconManager mBeaconManager;
    private BackgroundPowerSaver backgroundPowerSaver;
    private static final String TAG = "MyActivity";
    List<JSONArray> coordinateArray;
    List<String> referenceArray;
    List<JSONArray> polygonCoordinatesArray;

    List<Vertex> vertexList;
    double extrapLength;

    List<String> beaconIdList = new ArrayList<String>();
    List<Double> beaconDistanceList = new ArrayList<Double>();

    Button startNavig;
    GeoJsonLayer layer;
    List<String> roomRef;
    List<String> roomNameArray;
    List<Vertex> routeVertexList;
    List<String> allBeaconUidList;
    List<String> allBeaconRefList;
    List<JSONArray> allBeaconCoordinateList;
    List<JSONArray> routeCoordinateArray;
    ArrayList<LatLng> latLngArray;
    boolean realtimenavflag = false;
    boolean done_flag = false;

    //for navigation
    int first = 0, second = 1, third = 2;
    String node1, node2;
    double d1 = 0, d2 = 0, D1 = 0, D2 = 0;
    int i_node1, i_node2, i_node0;
    List<String> routeBeaconRefList;
    List<String> routeBeaconUIdList;
    List<JSONArray> routeBeaconCoordinateList;
    //end
    TextView directions;
    MediaPlayer left,right,straight;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map1);
        left = MediaPlayer.create(MapsActivity.this,R.raw.left);
        right = MediaPlayer.create(MapsActivity.this,R.raw.right);
        straight = MediaPlayer.create(MapsActivity.this,R.raw.straight);
        backgroundPowerSaver = new BackgroundPowerSaver(this);
        directions = (TextView) findViewById(R.id.directions);
        jsonConvert();
        verifyBluetooth();
        getLocationAccess();
        makeVertexList();
        setExtrapLength();
        setVertexAdjacencies();
        //Make List of Vertexes , Set Maximum Adjacency Length and Set Adjacencies
        setUpMap();
        setStartButtonListener();
    }


    private void setUpMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        orientCamera(5,30);
        startDemo();
    }

    private void orientCamera(int coordinateArrayindex,float tilt){
        try {
            double x = Double.parseDouble(coordinateArray.get(coordinateArrayindex).get(1).toString());
            double y = Double.parseDouble(coordinateArray.get(coordinateArrayindex).get(0).toString());
            LatLng hospitalLocation = new LatLng(x,y);
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(hospitalLocation)      // Sets the center of the map to Mountain View
                    .zoom(19)                   // Sets the zoom
                    .bearing((float) calculateBearing())               // Sets the orientation of the camera to east
                    .tilt(tilt)                   // Sets the tilt of the camera to 30 degrees
                    .build();
            getMap().moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            //getMap().setMinZoomPreference(20.0f);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private double calculateBearing(){
        try {
            double x1 = Double.parseDouble(coordinateArray.get(0).get(1).toString());
            double y1 = Double.parseDouble(coordinateArray.get(0).get(0).toString());
            double x2 = Double.parseDouble(coordinateArray.get(17).get(1).toString());
            double y2 = Double.parseDouble(coordinateArray.get(17).get(0).toString());
            double dLon = (y2-y1);
            double y = Math.sin(dLon) * Math.cos(x2);
            double x = Math.cos(x1)*Math.sin(x2) - Math.sin(x1)*Math.cos(x2)*Math.cos(dLon);
            double bearing = Math.toDegrees((Math.atan2(y, x)));
            bearing = (360 - ((bearing + 360) % 360));
            return bearing;
        }catch (Exception e){}
    return 90;
    }

    protected void startDemo() {
        // Alternate approach of loading a local GeoJSON file.
        retrieveFileFromResource();
    }

    private void retrieveFileFromResource() {
        try {
            layer = new GeoJsonLayer(getMap(), R.raw.cool, this);
            addGeoJsonLayerToMap(layer);
        } catch (IOException e) {

        } catch (JSONException e) {

        }

    }

    private void addGeoJsonLayerToMap(GeoJsonLayer layer) {
        layer.addLayerToMap();
        //get coordinateArray referenceArray polygonCoordinateArray

        int k = 0;
        try {
            latLngArray = new ArrayList<LatLng>();
            //List of Latitudes and Longitudes
            GeoJsonLineString routeString = new GeoJsonLineString(latLngArray);
            //Line String feature
            Dijkstra routeDijkstra = new Dijkstra();

            //---------------
            Intent intent = getIntent();
            Bundle extrasBundle = intent.getExtras();
            int source = extrasBundle.getInt("source",0);
            int destination = extrasBundle.getInt("destination",0);

            //Find out which vertex to be used
            routeVertexList = new ArrayList<Vertex>();
            routeVertexList = routeDijkstra.getRoute(vertexList.get(source),vertexList.get(destination));
            setRouteUIdList();
            //Get Route
            routeCoordinateArray = new ArrayList<JSONArray>();
            int a = 0;
            while(a < routeVertexList.size()){
                int b = 0;
                while(b < referenceArray.size()){
                    if(routeVertexList.get(a).name.equals(referenceArray.get(b))){
                        routeCoordinateArray.add(coordinateArray.get(b));
                    }
                    b++;
                }
                a++;
            }
            //Get Coordinates of Route Points from Coordinate Array
            while (k < routeCoordinateArray.size()) {
                double xcord = Double.parseDouble(routeCoordinateArray.get(k).get(1).toString());
                double ycord = Double.parseDouble(routeCoordinateArray.get(k).get(0).toString());
                latLngArray.add(new LatLng(xcord, ycord));
                //Get coordinates from Coordinate array and add to lineString Array
                k++;
            }
            GeoJsonFeature route = new GeoJsonFeature(routeString, null, null, null);
            //Create a new Line String Feature
            GeoJsonLineStringStyle routeStringStyle = new GeoJsonLineStringStyle();
            routeStringStyle.setColor(Color.RED);
            routeStringStyle.setWidth((float) 15.0);
            route.setLineStringStyle(routeStringStyle);
            //Set Feature Style

            GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
            pointStyle.setVisible(false);

            for(GeoJsonFeature feature : layer.getFeatures()){
                if(feature.getGeometry().getGeometryType().equals("Point")){
                    feature.setPointStyle(pointStyle);
                }
            }
            //Hide Points

            setStartPointStyle(layer);
            setEndpointStyle(layer);
            //Set start and end Point Styles

            ArrayList<LatLng> sourceLatLngArray = new ArrayList<LatLng>();
            GeoJsonLineString sourcePolygonString = new GeoJsonLineString(sourceLatLngArray);
            GeoJsonLineStringStyle sourceRoomStyle = new GeoJsonLineStringStyle();
            sourceRoomStyle.setPolygonFillColor(R.color.sourceRoomColor);

            //---
            layer.addFeature(route);
            //Add Feature to Layer

        } catch (JSONException e) {
            e.printStackTrace();
        }
        //15.49596778090 73.83344165502
        // Demonstrate receiving features via GeoJsonLayer clicks.

        /*layer.setOnFeatureClickListener(new GeoJsonLayer.GeoJsonOnFeatureClickListener() {
            @Override
            public void onFeatureClick(Feature feature) {
                    Toast.makeText(MapsActivity.this,
                            "Feature clicked: " + feature.getProperty("ref"),
                            Toast.LENGTH_SHORT).show();

            }

        });*/

    }

    private void setStartPointStyle(GeoJsonLayer layer){
        GeoJsonPointStyle startPointStyle = new GeoJsonPointStyle();
        BitmapDescriptor startPointIcon = BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        startPointStyle.setIcon(startPointIcon);
        startPointStyle.setVisible(true);
        GeoJsonPoint startPoint = new GeoJsonPoint(latLngArray.get(0));
        GeoJsonFeature startPointFeature = new GeoJsonFeature(startPoint,null,null,null);
        startPointFeature.setPointStyle(startPointStyle);
        layer.addFeature(startPointFeature);
    }

    private void setEndpointStyle(GeoJsonLayer layer){
        GeoJsonPointStyle endPointStyle = new GeoJsonPointStyle();
        BitmapDescriptor endPointIcon = BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
        endPointStyle.setIcon(endPointIcon);
        endPointStyle.setVisible(true);
        GeoJsonPoint endPoint = new GeoJsonPoint(latLngArray.get(latLngArray.size() - 1));
        GeoJsonFeature endPointfeature = new GeoJsonFeature(endPoint,null,null,null);
        endPointfeature.setPointStyle(endPointStyle);
        layer.addFeature(endPointfeature);
    }

    public GoogleMap getMap() {
        return mMap;
    }

    private void setStartButtonListener(){
        startNavig = (Button) findViewById(R.id.start);
        startNavig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //detectBeaconPoints(layer);
                orientCamera(2,70);


                /*
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                       // realTimeNav();
                    }
                };
                Thread mythread = new Thread(runnable);
                mythread.start();
                */


                realtimenavflag = true;
            }
        });

    }

    private void noDisplayLevels(String level,GeoJsonLayer layer){
        for(GeoJsonFeature feature : layer.getFeatures()){
            if(!(feature.hasProperty("level"))){
                if (!(feature.getProperty("level").equals(level))){
                    GeoJsonPointStyle levelPointstyle = new GeoJsonPointStyle();
                    levelPointstyle.setVisible(false);
                }
            }
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        Region region = new Region("all-beacons-region", null, null, null);
        try {
            mBeaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mBeaconManager.addRangeNotifier(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        // Detect the main Eddystone-UID frame:
        mBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        mBeaconManager.bind(this);
        mBeaconManager.setForegroundBetweenScanPeriod(25);
        mBeaconManager.setForegroundScanPeriod(50);
        backgroundPowerSaver = new BackgroundPowerSaver(this);
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        for (Beacon beacon : beacons) {
            if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
                // This is a Eddystone-UID frame
                if(!(beaconIdList.contains(beacon.getId1().toString()))) {
                    beaconIdList.add(beacon.getId1().toString());
                    beaconDistanceList.add(beacon.getDistance());
                }

                int i = beaconIdList.indexOf(beacon.getId1().toString());
                if(i >= 0) {
                    beaconDistanceList.set(i, beacon.getDistance());
                }

                /**/
                Identifier namespaceId = beacon.getId1();
                Identifier instanceId = beacon.getId2();
                Double distance = beacon.getDistance();
                //Toast.makeText(MapsActivity.this, "Beacon Detected " + namespaceId + "Instance " + instanceId + "Distance of" + distance, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Beacon Detected " + namespaceId + "Instance " + instanceId + "Distance of" + distance);
                Log.d(TAG, beaconIdList.toString());
                Log.d(TAG, beaconDistanceList.toString());

            }
        }
        Log.d(TAG, "-----------------------------------------here1");

        //now one iteration of dynamic location: frequency same as BLE updates
        if(realtimenavflag && !done_flag) {
            //start if
            Log.d(TAG, "-----------------------------------------here2");
            if (beaconIdList.size() >= 2 ) {
                Log.d(TAG, "-----------------------------------------here3");
                node1 = routeBeaconUIdList.get(first);
                node2 = routeBeaconUIdList.get(second);

                i_node1 = beaconIdList.indexOf(node1);
                i_node2 = beaconIdList.indexOf(node2);


                d1 = D1;
                d2 = D2;

                D1 = beaconDistanceList.get(i_node1);
                D2 = beaconDistanceList.get(i_node2);

                //instead
                Log.d(TAG, String.valueOf(d1));
                Log.d(TAG, String.valueOf(d2));
                Log.d(TAG, String.valueOf(D1));
                Log.d(TAG, String.valueOf(D2));
                Log.d(TAG, "-----------------------------------------here");


                //end

                //algo

                if (((D1 - d1 > 0) && (D2 - d2 < 0))) {
                    Log.d(TAG, "You are on the right path1");
                } else {
                    //Toast.makeText(MapsActivity.this, "You are going in the opposite direction", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "You are on the opp path1");
                }
                double zeta = 0.3; // To be determined

                if (Math.abs(D1 - d1) - Math.abs(D2 - d2) < zeta) {
                    Log.d(TAG, "You are on the right path2");
                } else {
                    //Toast.makeText(MapsActivity.this, "You are going off course", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "You are on the wrong path2");
                }
                int k;
                double buffer = 0.04; //To be determined
                for (k = 0; k < beaconIdList.size(); k++) {
                    if (beaconDistanceList.get(k) < buffer) {
                        if (beaconIdList.get(k).equals(node1)) {
                            Log.d(TAG, "-");
                        } else if (beaconIdList.get(k).equals(node2)) {
                            first++;
                            second++;
                            if (second < routeBeaconRefList.size()) {
                                Log.d(TAG,"------------------------------Triggered");
                                if(first!=1) {
                                    new Thread() {
                                        public void run() {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    removePrevPosition(node1, layer);
                                                }
                                            });
                                        }
                                    }.start();
                                }
                                node1 = routeBeaconUIdList.get(first);
                                node2 = routeBeaconUIdList.get(second);

                                i_node1 = beaconIdList.indexOf(node1);
                                i_node2 = beaconIdList.indexOf(node2);

                                D1 = beaconDistanceList.get(i_node1);
                                D2 = beaconDistanceList.get(i_node2);

                                new Thread() {
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                setCurrentPosition(node1,layer);
                                                int prev = first - 1;
                                                int turn = leftRightTurn(prev, first, second);
                                                voiceout(turn);
                                            }
                                        });
                                    }
                                }.start();


                                //start
                                //end


                                //Remove Segment
                                //Change bearing
                            } else {
                                Log.d(TAG, "------------------------------------------------------------------You have reached destination");
                                new Thread() {
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                setCurrentPosition(node2,layer);
                                                directions.setText(R.string.Destination);
                                            }
                                        });
                                    }
                                }.start();
                                //Toast.makeText(MapsActivity.this, "You have reached the destination", Toast.LENGTH_SHORT).show();
                                done_flag = true;
                                break;
                            }
                        } else {
                            Toast.makeText(MapsActivity.this, "You have reached the wrong node", Toast.LENGTH_SHORT).show();
                        }
                    }

                }

                //algo ends
            }
            //if ends

        }

        //ends
    }

    private void detectBeaconPoints(GeoJsonLayer layer){
        for(GeoJsonFeature feature : layer.getFeatures()){
           if(feature.hasProperty("man_made")) {
                   if (feature.getProperty("man_made").equals("beacon")) {
                       GeoJsonPointStyle beaconPointStyle = new GeoJsonPointStyle();
                       beaconPointStyle.setVisible(true);
                       beaconPointStyle.setTitle("Beacon");
                       feature.setPointStyle(beaconPointStyle);
                   }
           }
        }
        setStartPointStyle(layer);
        setStartPointStyle(layer);
    }

    private void jsonConvert() {
        //Parse GeoJson file to find point ref and their co-ordinates and create separate arrays for both of them
        String json = null;
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.cool);
            //getResources().getIdentifier("raw/nit2",
            //"raw", getPackageName()));
            //Get File
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, "UTF-8");
            //Convert Json File to String
            JSONObject jsonObject = new JSONObject(json);
            JSONArray features = jsonObject.getJSONArray("features");
            //Create JsonObject of the json string and parse to get Features Json array
            List<JSONObject> featureArray = new ArrayList<JSONObject>();
            for (int j = 0; j < features.length(); j++) {
                featureArray.add(j, features.getJSONObject(j));
            }
            //Create an array of JsonObjects under Features JsonArray
            coordinateArray = new ArrayList<JSONArray>();
            //Create a 2d list of String to store Co-ordinates
            referenceArray = new ArrayList<String>();
            //Create a String List to store ref
            roomRef = new ArrayList<String>();
            int i = 0;
            int k = 0;
            while (i < featureArray.size()) {
                if (featureArray.get(i).getJSONObject("geometry").getString("type").equals("Point")) {
                    referenceArray.add(k, featureArray.get(i).getJSONObject("properties").getString("ref"));
                    coordinateArray.add(k, featureArray.get(i).getJSONObject("geometry").getJSONArray("coordinates"));
                    k++;
                }
                i++;
            }
            int a = 0;
            allBeaconUidList = new ArrayList<String>();
            allBeaconRefList = new ArrayList<String>();
            allBeaconCoordinateList = new ArrayList<JSONArray>();
            while (a < featureArray.size()){
                if (featureArray.get(a).getJSONObject("geometry").getString("type").equals("Point")){
                   if(featureArray.get(a).getJSONObject("properties").getString("man_made").equals("beacon")) {
                    allBeaconUidList.add(featureArray.get(a).getJSONObject("properties").getString("uid"));
                    allBeaconRefList.add(featureArray.get(a).getJSONObject("properties").getString("ref"));
                    allBeaconCoordinateList.add(featureArray.get(a).getJSONObject("geometry").getJSONArray("coordinates"));
                   }
                }
                a++;
            }
            //Get all reference points and their coordinates
            polygonCoordinatesArray = new ArrayList<JSONArray>();
            roomRef = new ArrayList<String>();
            roomNameArray = new ArrayList<String>();
            //List of all Coordinate Points
            int m = 0;
            int n = 0;
            while (m < featureArray.size()){
                if(featureArray.get(m).getJSONObject("geometry").getString("type").equals("LineString")){
                    polygonCoordinatesArray.add(n, featureArray.get(m).getJSONObject("geometry").getJSONArray("coordinates"));
                    roomRef.add(featureArray.get(m).getJSONObject("properties").getString("refpt"));
                }
                m++;
            }
        } catch (Exception e) {

        }
    }

    public void makeVertexList(){
        vertexList = new ArrayList<Vertex>();
        int i = 0;
        while(i < referenceArray.size()){
            vertexList.add(new Vertex(referenceArray.get(i)));
            i++;
        }
    }

    public void setVertexAdjacencies(){
        ArrayList<Line2D> polygonLineList = new ArrayList<Line2D>();
        int n = 0;
        try {
            while (n < polygonCoordinatesArray.size()) {
                double x1 = Double.parseDouble(polygonCoordinatesArray.get(n).getJSONArray(0).get(1).toString());
                double y1 = Double.parseDouble(polygonCoordinatesArray.get(n).getJSONArray(0).get(0).toString());
                double x2 = Double.parseDouble(polygonCoordinatesArray.get(n).getJSONArray(1).get(1).toString());
                double y2 = Double.parseDouble(polygonCoordinatesArray.get(n).getJSONArray(1).get(0).toString());
                double x3 = Double.parseDouble(polygonCoordinatesArray.get(n).getJSONArray(2).get(1).toString());
                double y3 = Double.parseDouble(polygonCoordinatesArray.get(n).getJSONArray(2).get(0).toString());
                double x4 = Double.parseDouble(polygonCoordinatesArray.get(n).getJSONArray(3).get(1).toString());
                double y4 = Double.parseDouble(polygonCoordinatesArray.get(n).getJSONArray(3).get(0).toString());
                Line2D line1 = new Line2D(x1,y1,x2,y2);
                Line2D line2 = new Line2D(x2,y2,x3,y3);
                Line2D line3 = new Line2D(x3,y3,x4,y4);
                Line2D line4 = new Line2D(x4,y4,x1,y1);
                polygonLineList.add(line1);
                polygonLineList.add(line2);
                polygonLineList.add(line3);
                polygonLineList.add(line4);
                n++;
            }
        }catch (Exception e){

        }

        int k = 0;
        try {
            while (k < coordinateArray.size()) {
                int m = 0;
                while (m < coordinateArray.size()) {
                    int p = 0;
                    boolean intersect = false;
                    double xcord1 = Double.parseDouble(coordinateArray.get(k).get(1).toString());
                    double ycord1 = Double.parseDouble(coordinateArray.get(k).get(0).toString());
                    double xcord2 = Double.parseDouble(coordinateArray.get(m).get(1).toString());
                    double ycord2 = Double.parseDouble(coordinateArray.get(m).get(0).toString());
                    Line2D referencePointLine = new Line2D(xcord1,ycord1,xcord2,ycord2);
                    while (!intersect && (p < polygonLineList.size())){
                        intersect = Line2D.intersects(referencePointLine, polygonLineList.get(p));
                        p++;
                    }
                    double referencePLLength = 10000*referencePointLine.length();
                    if(!intersect && (referencePLLength <= extrapLength ) && (referencePLLength > 0)){
                        vertexList.get(k).adjacencies.add(new Edge(vertexList.get(m)));
                    }
                    m++;
                }
                k++;
            }
        }catch (Exception e){

        }
    }

    public void setExtrapLength(){
        try{
            double xcord1 = Double.parseDouble(coordinateArray.get(1).get(1).toString());
            double ycord1 = Double.parseDouble(coordinateArray.get(1).get(0).toString());
            double xcord2 = Double.parseDouble(coordinateArray.get(3).get(1).toString());
            double ycord2 = Double.parseDouble(coordinateArray.get(3).get(0).toString());
            Line2D line2D =  new Line2D(xcord1,ycord1,xcord2,ycord2);
            double length = line2D.length();
            extrapLength = 1.35*10000*length;

        }catch (Exception e) {

        }
    }

    private void verifyBluetooth() {

        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth not enabled");
                builder.setMessage("Please enable bluetooth in settings and restart this application.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                        System.exit(0);
                    }
                });
                builder.show();
            }
        } catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth LE not available");
            builder.setMessage("Sorry, this device does not support Bluetooth LE.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                    System.exit(0);
                }

            });
            builder.show();

        }

    }

    private void getLocationAccess() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons in the background.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @TargetApi(23)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSION_REQUEST_COARSE_LOCATION);
                    }

                });
                builder.show();
            }
        }
    }


    private void setRouteUIdList(){
        int a = 0;
        routeBeaconRefList = new ArrayList<>();
        routeBeaconUIdList = new ArrayList<>();
        routeBeaconCoordinateList = new ArrayList<JSONArray>();

        while (a < routeVertexList.size()) {
            int b = 0;
            while (b < allBeaconRefList.size()) {
                if (routeVertexList.get(a).name.equals(allBeaconRefList.get(b))) {
                    routeBeaconRefList.add(allBeaconRefList.get(b));
                    routeBeaconUIdList.add(allBeaconUidList.get(b));
                    routeBeaconCoordinateList.add(allBeaconCoordinateList.get(b));
                }
                b++;
            }
            a++;
        }
    }

    private void setCurrentPosition(String uid,GeoJsonLayer layer){
        int uidindex = routeBeaconUIdList.indexOf(uid);
        try{
            double xcord = Double.parseDouble(routeBeaconCoordinateList.get(uidindex).get(1).toString());
            double ycord = Double.parseDouble(routeBeaconCoordinateList.get(uidindex).get(0).toString());
            LatLng currentLatLng = new LatLng(xcord,ycord);
            GeoJsonPointStyle currentPointStyle = new GeoJsonPointStyle();
            BitmapDescriptor currentPointIcon = BitmapDescriptorFactory
                    .defaultMarker(BitmapDescriptorFactory.HUE_CYAN);
            currentPointStyle.setIcon(currentPointIcon);
            currentPointStyle.setVisible(true);
            GeoJsonPoint startPoint = new GeoJsonPoint(currentLatLng);
            GeoJsonFeature currentPointFeature = new GeoJsonFeature(startPoint,null,null,null);
            currentPointFeature.setPointStyle(currentPointStyle);
            layer.addFeature(currentPointFeature);

        }catch (Exception e){}


    }

    private void removePrevPosition(String uid,GeoJsonLayer layer){
        int uidindex = routeBeaconUIdList.indexOf(uid);
        try{
            double xcord = Double.parseDouble(routeBeaconCoordinateList.get(uidindex).get(1).toString());
            double ycord = Double.parseDouble(routeBeaconCoordinateList.get(uidindex).get(0).toString());
            LatLng prevLatLng = new LatLng(xcord,ycord);
            GeoJsonPointStyle prevPointStyle = new GeoJsonPointStyle();
            prevPointStyle.setVisible(false);
            GeoJsonPoint startPoint = new GeoJsonPoint(prevLatLng);
            GeoJsonFeature currentPointFeature = new GeoJsonFeature(startPoint,null,null,null);
            currentPointFeature.setPointStyle(prevPointStyle);
            layer.addFeature(currentPointFeature);

        }catch (Exception e){}

    }

    public void voiceout(int turn){
        if(turn==-1){
            left.start();
            directions.setText(R.string.Left_turn);
        }
        else if(turn==1){
            right.start();
            directions.setText(R.string.Right_turn);
        }
        else  if(turn==0)
        {
            straight.start();
            directions.setText(R.string.Straight);
        }
        else
        {
            Log.d(TAG,"FAIL");
        }
    }

    private int leftRightTurn(int p0,int p1, int p2){
        try {
            double x1 = Double.parseDouble(routeBeaconCoordinateList.get(p0).get(1).toString());
            double y1 = Double.parseDouble(routeBeaconCoordinateList.get(p0).get(0).toString());
            double x2 = Double.parseDouble(routeBeaconCoordinateList.get(p1).get(1).toString());
            double y2 = Double.parseDouble(routeBeaconCoordinateList.get(p1).get(0).toString());
            double x3 = Double.parseDouble(routeBeaconCoordinateList.get(p2).get(1).toString());
            double y3 = Double.parseDouble(routeBeaconCoordinateList.get(p2).get(0).toString());
            Voiceassist voiceassist = new Voiceassist();
            int turn = voiceassist.voice(x1,y1,x2,y2,x3,y3);

            return turn;
        }catch (Exception e){

        }
        return 2;
    }

}
