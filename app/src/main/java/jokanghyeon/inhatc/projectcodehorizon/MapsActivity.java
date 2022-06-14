package jokanghyeon.inhatc.projectcodehorizon;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import jokanghyeon.inhatc.projectcodehorizon.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private LocationManager lm;

    private ImageView ivProfile;

    private TextView tvNickname;
    private TextView tvHour;

    private Button btnMenuOpen;
    private Button btnMenuClose;
    private Button btnEditProfile;
    private Button btnLogOut;
    private Button btnWrite;

    private DrawerLayout drawerLayout;

    private FusedLocationProviderClient mFusedLocationProviderClient;

    private FirebaseAuth mAuth;
    private FirebaseDatabase mDB;

    private LatLng currentPosition;

    private List<Marker> markerList=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        lm= (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);

        ivProfile = findViewById(R.id.ivMapProfileImage);
        tvNickname = findViewById(R.id.tvMapNickname);
        tvHour = findViewById(R.id.tvMapHour);

        btnMenuOpen = findViewById(R.id.btnMapMenuOpen);
        btnMenuOpen.setOnClickListener(this);

        btnMenuClose = findViewById(R.id.btnMapMenuClose);
        btnMenuClose.setOnClickListener(this);

        btnEditProfile = findViewById(R.id.btnMapEditProfile);
        btnEditProfile.setOnClickListener(this);

        btnLogOut = findViewById(R.id.btnMapLogout);
        btnLogOut.setOnClickListener(this);

        btnWrite = findViewById(R.id.btnMapWrite);
        btnWrite.setOnClickListener(this);

        drawerLayout = findViewById(R.id.drawerLayoutMap);

        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseDatabase.getInstance();

        mDB.getReference().child("users").child(mAuth.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                SetScreen();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void SetScreen(){
        Utility.getInstance().SetProfilePic(mAuth.getUid(),ivProfile);
        Utility.getInstance().SetTextViewTimeSelf(tvHour);
        Utility.getInstance().SetTextViewNickname(mAuth.getUid(),tvNickname);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},1);
            return;
        }

        mMap.moveCamera(CameraUpdateFactory.zoomTo(18));

        mMap.setMyLocationEnabled(true);

        mMap.getUiSettings().setAllGesturesEnabled(false);

        startGetLocation();

        mDB.getReference().child("map_data").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(markerList != null){
                    for(Marker m : markerList){
                        m.remove();
                    }
                }

                markerList = new ArrayList<>();

                for(DataSnapshot sn: snapshot.getChildren()){
                    SetMarker(new LatLng((double) sn.child("latitude").getValue(), (double) sn.child("longitude").getValue()),sn.getKey());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                OpenToken((String)marker.getTag());
                return false;
            }
        });
    }

    private void OpenToken(String key){
        Intent intent = new Intent(MapsActivity.this,ReadTokenActivity.class);
        intent.putExtra("id",key);
        startActivity(intent);
    }

    private void SetMarker(LatLng position, String key){

        //Drawable -> Bitmap
        BitmapDrawable drawable =(BitmapDrawable)getDrawable(R.drawable.marker);
        Bitmap bitmap = drawable.getBitmap();

        bitmap = Bitmap.createScaledBitmap(bitmap,100,100,true);

        Marker marker = mMap.addMarker(new MarkerOptions().
                position(position).icon(BitmapDescriptorFactory.fromBitmap(bitmap)));

        marker.setTag(key);
        markerList.add(marker);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startGetLocation();
    }

    private void startGetLocation() {
        try {

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(20 * 1000);

            mFusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    Location location = locationResult.getLastLocation();
                    currentPosition = new LatLng(location.getLatitude(),
                            location.getLongitude());
                    CameraUpdate update = CameraUpdateFactory.newLatLng(currentPosition);
                    mMap.moveCamera(update);
                }
            }, Looper.getMainLooper());

        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    @Override
    public void onClick(View view) {
        if(view==btnMenuOpen){
            drawerLayout.openDrawer(GravityCompat.END,true);
        }else if(view==btnMenuClose){
            drawerLayout.closeDrawer(GravityCompat.END,true);
        }else if(view==btnEditProfile){
            Intent intent = new Intent(MapsActivity.this,ProfileEditActivity.class);
            startActivity(intent);
        }else if(view==btnLogOut){
            if(mAuth.getCurrentUser() == null){
                finish();
            }
            if(mAuth.getCurrentUser().getProviderId()=="google.com"){
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(Utility.getInstance().GetMetadata(MapsActivity.this, "LoginActivity.API_KEY"))
                        .requestEmail()
                        .build();
                GoogleSignIn.getClient(MapsActivity.this, gso).signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        AuthUI.getInstance().signOut(MapsActivity.this).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                finish();
                            }
                        });

                    }
                });
            }else{
                AuthUI.getInstance().signOut(MapsActivity.this).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        finish();
                    }
                });
            }
        }else if(view==btnWrite){
            Intent intent = new Intent(MapsActivity.this,WriteTokenActivity.class);
            intent.putExtra("position",currentPosition);
            startActivity(intent);
        }
    }
}