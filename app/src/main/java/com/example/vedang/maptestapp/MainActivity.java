package com.example.vedang.maptestapp;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {


    List<String> referenceList;
    List<String> roomNameArray;
    List<String> roomRef;
    InstantAutoCompleteTextView source;
    InstantAutoCompleteTextView destination;
    Button btnSubmit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getRooms();
        source = (InstantAutoCompleteTextView) findViewById(R.id.source);
        destination = (InstantAutoCompleteTextView) findViewById(R.id.destination);
        ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,roomNameArray);
        source.setAdapter(adapter);
        source.setThreshold(0);
        destination.setAdapter(adapter);
        destination.setThreshold(0);
        addListenerOnButton();
    }


    // get the selected dropdown list value
    public void addListenerOnButton() {
        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!source.getText().toString().equals("") && !source.getText().toString().equals("")) {
                    Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                    Bundle bundle = new Bundle();
                    int m = roomNameArray.indexOf(source.getText().toString());
                    int n = roomNameArray.indexOf(destination.getText().toString());
                    int o = referenceList.indexOf(roomRef.get(m));
                    int p = referenceList.indexOf(roomRef.get(n));
                    bundle.putInt("source", o);
                    bundle.putInt("destination", p);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
                else {
                    Toast.makeText(MainActivity.this,"Fields Cannot be Empty",Toast.LENGTH_SHORT).show();
                }
            }

        });
    }

    private void getRooms(){
        String json = null;
        try{
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
            for(int j = 0;j< features.length();j++){
                featureArray.add(j,features.getJSONObject(j));
            }
            //Create a 2d list of String to store Co-ordinates
            referenceList = new ArrayList<String>();
            //Create a String List to store ref
            roomNameArray = new ArrayList<String>();
            //Create a String List to store Rooms
            roomRef = new ArrayList<String>();
            int i = 0;
            int k = 0;
            int n = 0;
            while(i < featureArray.size()){
                if(featureArray.get(i).getJSONObject("geometry").getString("type").equals("Point")) {
                    referenceList.add(k, featureArray.get(i).getJSONObject("properties").getString("ref"));
                    k++;
                }
                i++;
            }
            int f = 0;
            while(n < featureArray.size()){
                if(featureArray.get(n).getJSONObject("geometry").getString("type").equals("LineString")) {
                    roomNameArray.add(featureArray.get(n).getJSONObject("properties").getString("name"));
                    roomRef.add(featureArray.get(n).getJSONObject("properties").getString("refpt"));
                }
                n++;
            }


        }catch(Exception e){

        }
    }

}
