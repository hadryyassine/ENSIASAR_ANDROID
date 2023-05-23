package com.example.campuscompass;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

//import com.google.vr.sdk.widgets.pano.VrPanoramaView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class ExploreActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    Button explore;
    Spinner source, dest;
    int curFloor;
    int selectedLevel=0;
    boolean scanDone=false;
    String []places = {"Service",
            "Grand amphi",
            "Administration",
            "Internat filles",
            "Amphi3 & Salles L3x",
            "Amphi4 & salles L2x",
            "Terrains sports",
            "Amphi4 & salles L2x",
            "Terrain gazoné",
            "Amphi4 & salles L2x",
            "Service internat"
    };
    int []placesLevels={2,2,2,2,2,2,2,2,2,2,3,3,3,3,3,3,3,3,3,4,4,4,4,4,4,4,4,4,4,4,4,4,5,5,5,5,5,5,5,5,5,5,5,5};

    Location src=null,desti=null;
    Location newSrc;

    Deque<Location> traverse=new LinkedList<Location>();
    Deque<Location> smallest=new LinkedList<Location>();
    Button[] pills = new Button[1];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);
        makeLocations();
        explore=findViewById(R.id.showPath);

        explore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i=new Intent(getApplicationContext(),PathActivity.class);
                startActivity(i);
            }
        });
        source = findViewById(R.id.source);
        dest = findViewById(R.id.dest);

        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, places);
        source.setAdapter(sourceAdapter);
        ArrayAdapter<String> destAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, places);
        dest.setAdapter(destAdapter);

        source.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ((TextView) adapterView.getChildAt(0)).setTextColor(Color.BLACK);
                src=getLocation(places[i],placesLevels[i]);
                if(src !=null && desti!=null){
                    resetInRoute();
                    setRoute(src,desti);
                    Bundle bundle = new Bundle();
                    bundle.putInt("level",src.getLevel());
                    Floor f=new Floor();
                    f.setArguments(bundle);
                    replaceFragment(f);
                    CurrentPointer.current=src;
                    for (int j = 0; j < pills.length; j++) {
                        pills[j].setBackgroundResource(R.drawable.pill_tab);
                    }
                    pills[src.getLevel()-2].setBackgroundResource(R.drawable.selected_pill);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.i("GTOUTOUT", "Nothing Selected");
            }
        });

        dest.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ((TextView) adapterView.getChildAt(0)).setTextColor(Color.BLACK);
                desti=getLocation(places[i],placesLevels[i]);
                if(src !=null && desti!=null){
                    resetInRoute();
                    setRoute(src,desti);
                    Bundle bundle = new Bundle();
                    bundle.putInt("level",src.getLevel());
                    Floor f=new Floor();
                    f.setArguments(bundle);
                    replaceFragment(f);
                    for (int j = 0; j < pills.length; j++) {
                            pills[j].setBackgroundResource(R.drawable.pill_tab);
                    }
                    pills[src.getLevel()-2].setBackgroundResource(R.drawable.selected_pill);
                    CurrentPointer.current=src;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.i("GTOUTOUT", "Nothing Selected");
            }
        });

        for (int i = 0; i < pills.length; i++) {
            pills[i] = findViewById(getResources().getIdentifier("pill" + (i + 1), "id", getPackageName()));
            final int index = i;
            pills[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (int j = 0; j < pills.length; j++) {
                        if (j == index) {
                            selectedLevel = index+2;
                            pills[j].setBackgroundResource(R.drawable.selected_pill);
                                Bundle bundle = new Bundle();
                                bundle.putInt("level",selectedLevel);
                                Floor f=new Floor();
                                f.setArguments(bundle);
                                replaceFragment(f);
                        } else {
                            pills[j].setBackgroundResource(R.drawable.pill_tab);
                        }
                    }
                }
            });
        }

    }

    private void replaceFragment(Fragment f) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.frame,f);
        ft.commit();
    }

    private void scanSuccess() {
        if(scanDone)
            return;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        curFloor= 2;
            Bundle bundle = new Bundle();
            bundle.putInt("level",curFloor);
            Floor f=new Floor();
            f.setArguments(bundle);
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.frame,f);
            ft.commit();
            pills[selectedLevel-2].setBackgroundResource(R.drawable.selected_pill);

        scanDone=true;
    }


    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with Wi-Fi scan
//                wifiManager.startScan();
                scanSuccess();
            } else {
                // Permission denied, show an error message
                Toast.makeText(this, "Location permission required to scan for Wi-Fi networks", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void makeLocations() {
        {
            String[] places = {
                    "LH306",
                    "LH308",
                    "LH309",
                    "LH309",
                    "LH310",
                    "LH311",
                    "LH312",
                    "EC Staffroom",
                    "CS Staffroom",
                    "Texas Instruments Lab"};
            Location node0 = new Location("Balcony", new ArrayList<String>(Arrays.asList("Balcony", places[4], places[1])), new ArrayList<Integer>(Arrays.asList(PlacePosition.left, PlacePosition.bottomLeft, PlacePosition.topLeft)),R.drawable.three0, 3, false, 180f, null, null, null, null, null);
            Location node1 = new Location("Internat Garçon", new ArrayList<String>(Arrays.asList(places[6], places[8])), new ArrayList<Integer>(Arrays.asList(PlacePosition.bottomLeft, PlacePosition.right)), R.drawable.three1, 3, false, 0f, null, null, null, null, null);
            Location node2 = new Location("LH311", new ArrayList<String>(Arrays.asList(places[5], places[8])), new ArrayList<Integer>(Arrays.asList(PlacePosition.bottomRight, PlacePosition.bottomLeft)), R.drawable.three2, 3, false, 0f, null, null, null, null, null);
            Location node3 = new Location("Salles TPs", new ArrayList<String>(Arrays.asList(places[3], "Salles TPs")), new ArrayList<Integer>(Arrays.asList(PlacePosition.bottomRight, PlacePosition.right)), R.drawable.three3, 3, false, 0f, null, null, null, null, null);
            Location node4 = new Location("TI Lab", new ArrayList<String>(Arrays.asList(places[9], places[7])), new ArrayList<Integer>(Arrays.asList(PlacePosition.right, PlacePosition.bottomRight)), R.drawable.three4, 3, false, 45f, null, null, null, null, null);
            Location node5 = new Location("LH306", new ArrayList<String>(Arrays.asList(places[0], places[7])), new ArrayList<Integer>(Arrays.asList(PlacePosition.left, PlacePosition.bottomRight)), R.drawable.three4, 3, false, 135f, null, null, null, null, null);
            Location stairs1 = new Location("Stairs1", new ArrayList<>(), new ArrayList<>(), R.drawable.stairs1, 3, true, 0f, null, null, null, null, null);
            Location stairs2 = new Location("Stairs2", new ArrayList<>(), new ArrayList<>(), R.drawable.stairs2, 3, true, 0f, null, null, null, null, null);
            makeConnections(node0,node1,node2,node3,node4,node5,stairs1,stairs2);
            node0.setAngle(180);
            node3.setAngle(180);
            node2.setAngle(-90);
            stairs1.setAngle(180);
            LevelPointer.levels[3] = node0;
        }
        {

            String []places = {
                    "Internat Garçon",
                    "LH302",
                    "Service",
                    "Grand amphi",
                    "Administration",
                    "Internat filles",
                    "Amphi3 & Salles L3x",
                    "Amphi4 & salles L2x",
                    "Terrains sports",
                    "Amphi4 & salles L2x",
                    "Terrain gazoné",
                    "I&P Lab"};
            Location node0 = new Location(
                    "Balcony",
                    new ArrayList<String>(Arrays.asList(places[5], places[8])),
                    new ArrayList<Integer>(Arrays.asList(PlacePosition.bottomLeft, PlacePosition.topRight)),
                    R.drawable.agora,
                    2,
                    false,
                    180f,
                    null,
                    null,
                    null,
                    null,
                    null);
            Location node1 = new Location(
                    "Amphi4 & salles L2x",
                    new ArrayList<String>(Arrays.asList(places[7], "-", "-")),
                    new ArrayList<Integer>(Arrays.asList(PlacePosition.bottomLeft, PlacePosition.right, PlacePosition.bottomRight)),
                    R.drawable.agora ,
                    2,
                    false,
                    0f,
                    null,
                    null,
                    null,
                    null,
                    null);
            Location node2 = new Location(
                    "Amphi3 & Salles L3x",
                    new ArrayList<String>(Arrays.asList(places[6], "-")),
                    new ArrayList<Integer>(Arrays.asList(PlacePosition.bottomRight,
                            PlacePosition.left)),
                    R.drawable.agora,
                    2, false, 0f, null, null, null, null, null);
            Location node3 = new Location("Salles TPs",
                    new ArrayList<String>(Arrays.asList(places[4], places[10], "Salles TPs")),
                    new ArrayList<Integer>(Arrays.asList(PlacePosition.topRight, PlacePosition.bottomRight, PlacePosition.right)),
                    R.drawable.sallestps,
                    2, false, 45f, null, null, null, null, null);
            Location node4 = new Location("Grand amphi",
                    new ArrayList<String>(Arrays.asList(places[11], places[2], places[3])),
                    new ArrayList<Integer>(Arrays.asList(PlacePosition.right, PlacePosition.topLeft, PlacePosition.right)),
                    R.drawable.grand_amphi,
                    2, false, 135f, null, null, null, null, null);
            Location node5 = new Location("Internat Garçon",
                    new ArrayList<String>(Arrays.asList(places[0])),
                    new ArrayList<Integer>(Arrays.asList(PlacePosition.left, PlacePosition.bottomRight)),
                    R.drawable.internatgarcon,
                    2, false, 135f, null, null, null, null, null);
            Location stairs1 = new Location(
                    "Stairs1",
                    new ArrayList<>(),
                    new ArrayList<>(),
                    R.drawable.parking,
                    2, true, 0f, null, null, null, null, null);
            Location stairs2 = new Location("Stairs2",
                    new ArrayList<>(), new ArrayList<>(),
                    R.drawable.agora, 2, true, 0f, null, null, null, null, null);
            node0.setAngle(180);
            node1.setAngle(180);
            stairs1.setAngle(180);
            node3.setAngle(-90);
            makeConnections(node0,node1,node2,node3,node4,node5,stairs1,stairs2);

            LevelPointer.levels[2] = node0;
        }

        //connecting stairs
        LevelPointer.levels[2].getStairs().setUp(LevelPointer.levels[3].getStairs());
        LevelPointer.levels[2].getStairs().setUpAngle(-170);

        LevelPointer.levels[3].getStairs().setDown(LevelPointer.levels[2].getStairs());
        LevelPointer.levels[3].getStairs().setDownAngle(170);

        LevelPointer.levels[2].getRight().getStairs().setUp(LevelPointer.levels[3].getRight().getStairs());
        LevelPointer.levels[2].getRight().setUpAngle(-170);
        LevelPointer.levels[3].getRight().getStairs().setDown(LevelPointer.levels[2].getRight().getStairs());
        LevelPointer.levels[3].getRight().getStairs().setUpAngle(170);
    }
    public static int getImageResourceId(Context context, String imageName) {
        return context.getResources().getIdentifier(imageName, "drawable", context.getPackageName());
    }

    private void makeConnections(Location node0,Location node1,Location node2,Location node3,Location node4,Location node5,Location stairs1,Location stairs2){
        node0.setLeft(node1);
        node0.setRight(node5);
        node0.setBack(node3);

        node1.setRight(node0);
        node1.setBack(node2);

        node2.setFront(node1);
        node2.setRight(node3);

        node3.setFront(node0);
        node3.setLeft(node2);
        node3.setRight(node4);

        node4.setLeft(node3);
        node4.setFront(node5);

        node5.setBack(node4);
        node5.setLeft(node0);

        node0.setStairs(stairs1);
        node5.setStairs(stairs2);
        node4.setStairs(stairs2);

        stairs1.setFront(node0);

        stairs2.setFront(node5);
        stairs2.setBack(node4);
    }

    private Location getLocation(String src,int level){
        Location node =LevelPointer.levels[level];

        if(node.getPlaces().contains(src))
            return node;
        node=node.getLeft();
        if(node.getPlaces().contains(src))
            return node;
        node=node.getBack();
        if(node.getPlaces().contains(src))
            return node;
        node=node.getRight();

        if(node.getPlaces().contains(src))
            return node;
        node=node.getRight();

        if(node.getPlaces().contains(src))
            return node;

        if(node.getPlaces().contains(src))
            return node.getFront();
        return null;
    }

    private void setRoute(Location src,Location desti){
        // if there are in same level
        if(src.getLevel()==desti.getLevel()){
            findSmallestRoute(src,desti);
            smallest.forEach(element->{
                element.setInRoute(true);
            });
        }
        else{
            findSmallestRouteStairs(src);
            smallest.forEach(element->{
                element.setInRoute(true);
            });
            smallest.clear();
            smallest.addFirst(null);
            smallest.addFirst(null);
            smallest.addFirst(null);
            smallest.addFirst(null);
            smallest.addFirst(null);
            smallest.addFirst(null);
            while (newSrc.getLevel()!=desti.getLevel()){
//                Log.d("hello", "setRoute: "+newSrc.getName()+" "+newSrc.getLevel()+" "+newSrc.getDown().toString());
                if(newSrc.getLevel()<desti.getLevel()){
                    newSrc=newSrc.getUp();
                    newSrc.setInRoute(true);
                }
                else{
                    newSrc=newSrc.getDown();
                    newSrc.setInRoute(true);
                }
            }
            findSmallestRoute(newSrc,desti);
            smallest.forEach(element->{
                element.setInRoute(true);
            });
        }
    }

    private void findSmallestRoute(Location src,Location dest){

        traverse.addLast(src);
        src.setInRoute(true);

        if(src==dest){
            if(smallest.size()>traverse.size())
            {
                smallest.clear();

                traverse.forEach(element->{
                    smallest.addFirst(element);
                });
            }
            src.setInRoute(false);
            traverse.removeLast();
            return;
        }

        if(src.getLeft()!=null && !src.getLeft().getInRoute())
            findSmallestRoute(src.getLeft(),dest);

        if(src.getRight()!=null && !src.getRight().getInRoute())
            findSmallestRoute(src.getRight(),dest);

        if(src.getBack()!=null && !src.getBack().getInRoute())
            findSmallestRoute(src.getBack(),dest);

        if(src.getFront()!=null && !src.getFront().getInRoute())
            findSmallestRoute(src.getFront(),dest);
        src.setInRoute(false);
        traverse.removeLast();
        return;
    }

    private void findSmallestRouteStairs(Location srcc){
        Log.d("ksfd", "findSmallestRouteStairs: "+srcc.getName());
        traverse.addLast(srcc);
        srcc.setInRoute(true);

        if(srcc.getStairs()!=null){
            if(smallest.size()>traverse.size())
            {
                smallest.clear();
                traverse.forEach(element->{
                    smallest.addFirst(element);
                });
                smallest.addFirst(srcc.getStairs());
                newSrc=srcc.getStairs();
            }
            srcc.setInRoute(false);
            traverse.removeLast();
            return;
        }

        if(srcc.getLeft()!=null && !srcc.getLeft().getInRoute())
            findSmallestRouteStairs(srcc.getLeft());

        if(srcc.getRight()!=null && !srcc.getRight().getInRoute())
            findSmallestRouteStairs(srcc.getRight());

        if(srcc.getBack()!=null && !srcc.getBack().getInRoute())
            findSmallestRouteStairs(srcc.getBack());

        if(srcc.getFront()!=null && !srcc.getFront().getInRoute())
            findSmallestRouteStairs(srcc.getFront());
        srcc.setInRoute(false);
        traverse.removeLast();
        return;
    }




    private void resetInRoute(){
        int []arr={2,3};
        traverse.clear();
        smallest.clear();
        smallest.addFirst(null);
        smallest.addFirst(null);
        smallest.addFirst(null);
        smallest.addFirst(null);
        smallest.addFirst(null);
        smallest.addFirst(null);
        for(int i=0;i<arr.length;i++){
            Location node=LevelPointer.levels[arr[i]];
            if(node==null)
                continue;
            node.setInRoute(false);
            node.getBack().setInRoute(false);
            node.getLeft().setInRoute(false);
            node.getRight().setInRoute(false);
            node.getRight().getBack().setInRoute(false);
            node.getLeft().getBack().setInRoute(false);
            node.getStairs().setInRoute(false);
            node.getRight().getStairs().setInRoute(false);
        }

    }
}
