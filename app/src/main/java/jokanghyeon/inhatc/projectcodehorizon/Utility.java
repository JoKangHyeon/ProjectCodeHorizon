package jokanghyeon.inhatc.projectcodehorizon;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.HashMap;

public class Utility {

    private static Utility instance;
    private final FirebaseDatabase mDB;
    private final FirebaseAuth mAuth;
    private final FirebaseStorage mStorage;

    private Utility(){
        mDB = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mStorage = FirebaseStorage.getInstance();
    }

    public static Utility getInstance(){
        if(instance==null){
            instance = new Utility();
        }
        return instance;
    }

    public String GetMetadata(Context cont, String key){
        String token="";

        try {
            ApplicationInfo ai = cont.getPackageManager().getApplicationInfo(cont.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            token = bundle.getString(key);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return token;
    }

    final long MAX_FILE_SIZE = 1024*1024*10;
    public void SetProfilePic(String uid, ImageView iv){
        FirebaseStorage fbs = FirebaseStorage.getInstance();
        StorageReference sr = fbs.getReference().child(uid);

        sr.getBytes(MAX_FILE_SIZE).addOnSuccessListener(bytes -> {
            Bitmap ret = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
            iv.setImageBitmap(ret);
        }).addOnFailureListener(e -> iv.setImageResource(R.drawable.basecamp));
    }

    public void SetTextViewNickname(String uid, TextView tv){
        DatabaseReference ref = mDB.getReference().child("users").child(uid).child("public").child("Nickname");

        ref.get().addOnSuccessListener(dataSnapshot -> tv.setText((String)dataSnapshot.getValue())).
                addOnFailureListener(e -> tv.setText("알 수 없는 유저"));
    }

    public void SetTextViewTimeSelf(TextView tv){
        DatabaseReference ref = mDB.getReference().child("users").child(mAuth.getUid()).child("private").child("TimeLeft");

        ref.get().addOnSuccessListener(dataSnapshot -> {
            tv.setText( (long)dataSnapshot.getValue() + "/24" );
        }).addOnFailureListener(e -> {
            tv.setText("오류");
        });
    }

    public void SetTextViewMaxTimeSelf(TextView tv){
        DatabaseReference ref = mDB.getReference().child("users").child(mAuth.getUid()).child("private").child("TimeLeft");
        ref.get().addOnSuccessListener(dataSnapshot -> {
            tv.setText("/" +(long)dataSnapshot.getValue());
        }).addOnFailureListener(e -> {
            tv.setText("오류");
        });
    }

    public void AccountInit(String Nickname, Drawable drawable){
        StorageReference fbStorageRef = mStorage.getReference();

        DatabaseReference userRef= mDB.getReference().child("users").child(mAuth.getUid());

        userRef.child("private").child("TimeLeft").setValue(24);
        userRef.child("public").child("Nickname").setValue(Nickname);

        Bitmap map = ((BitmapDrawable)drawable).getBitmap();
        UploadPrpfileImage(map);
    }


    public void UploadPrpfileImage(Bitmap bitmap, ImageView reset){
        StorageReference fbStorageRef = mStorage.getReference();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100,bos);
        byte[] bytes = bos.toByteArray();
        fbStorageRef.child(mAuth.getUid()).putBytes(bytes).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                if(reset!=null){
                    SetProfilePic(mAuth.getUid(),reset);
                }
            }
        });
    }

    public void UploadPrpfileImage(Bitmap bitmap){
        UploadPrpfileImage(bitmap,null);
    }

    public void RemoveData(){
        mDB.getReference().child("users").child(mAuth.getUid()).child("private").child("TimeLeft").setValue(null);
        mDB.getReference().child("users").child(mAuth.getUid()).child("public").child("Nickname").setValue(null);
        mStorage.getReference().child(mAuth.getUid()).delete();
    }

    public void RequestUploadToken(String title, String text, Bitmap img1, Bitmap img2, Bitmap img3, LatLng position, int time){
        DatabaseReference ref = mDB.getReference("map_data").push();
        HashMap<String,Object> data = new HashMap<>();

        data.put("TimeLeft",(long)time);
        data.put("Creater",mAuth.getUid());
        data.put("ThreadTitle",title);
        data.put("ThreadText",text);
        data.put("UploadDate", Calendar.getInstance().getTime());

        data.put("latitude",position.latitude);
        data.put("longitude",position.longitude);

        int count =0;

        if(img1!=null){
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            img1.compress(Bitmap.CompressFormat.PNG,100,bos);
            byte[] bytes = bos.toByteArray();
            mStorage.getReference().child("map").child(ref.getKey()).child(count+"").putBytes(bytes);
            count++;
        }

        if(img2!=null){
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            img2.compress(Bitmap.CompressFormat.PNG,100,bos);
            byte[] bytes = bos.toByteArray();
            mStorage.getReference().child("map").child(ref.getKey()).child(count+"").putBytes(bytes);
            count++;
        }

        if(img3!=null){
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            img3.compress(Bitmap.CompressFormat.PNG,100,bos);
            byte[] bytes = bos.toByteArray();
            mStorage.getReference().child("map").child(ref.getKey()).child(count+"").putBytes(bytes);
            count++;
        }

        data.put("ImageCount",count);

        ref.setValue(data).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                mDB.getReference().child("users").child(mAuth.getUid()).child("private").child("TimeLeft").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        if(task.isSuccessful()){
                            mDB.getReference().child("users").child(mAuth.getUid()).child("private").child("TimeLeft").setValue((long)task.getResult().getValue() - time );
                        }
                    }
                });
            }
        });
    }
}
