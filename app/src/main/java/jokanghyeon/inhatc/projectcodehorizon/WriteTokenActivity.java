package jokanghyeon.inhatc.projectcodehorizon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class WriteTokenActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnCancel;
    private Button btnAddImage;
    private Button btnFinish;

    private TextView tvHour;
    private TextView tvError;

    private EditText editHour;
    private EditText editTitle;
    private EditText editText;

    private ImageView ib1;
    private ImageView ib2;
    private ImageView ib3;
    private List<Bitmap> bitmapList;

    private FirebaseDatabase mDB;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_token);

        btnCancel = findViewById(R.id.btnWriteCancel);
        btnCancel.setOnClickListener(this);

        btnAddImage = findViewById(R.id.btnWriteAddImage);
        btnAddImage.setOnClickListener(this);

        btnFinish = findViewById(R.id.btnWriteFinish);
        btnFinish.setOnClickListener(this);

        tvHour = findViewById(R.id.tvWriteHour);
        tvError = findViewById(R.id.tvWriteError);

        editHour = findViewById(R.id.editWriteHour);
        editTitle = findViewById(R.id.editWriteTitle);
        editText = findViewById(R.id.editWriteText);

        ib1=findViewById(R.id.ivWriteImage1);
        ib1.setOnClickListener(this);
        ib2=findViewById(R.id.ivWriteImage2);
        ib2.setOnClickListener(this);
        ib3=findViewById(R.id.ivWriteImage3);
        ib3.setOnClickListener(this);

        bitmapList = new ArrayList<>();

        Utility.getInstance().SetTextViewMaxTimeSelf(tvHour);

        bitmapList.add(null);
        bitmapList.add(null);
        bitmapList.add(null);

        mDB = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onClick(View view) {
        if(view==btnAddImage){
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");

            startActivityForResult(Intent.createChooser(intent,"????????? ???????????????"),2);
        }else if(view==btnCancel){
            finish();
        }else if(view==btnFinish){
            if(editHour.getText().length()==0){
                tvError.setText("????????? ????????? ????????? ?????????.");
                return;
            }

            mDB.getReference().child("users").child(mAuth.getUid()).child("private").child("TimeLeft").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if(task.isSuccessful()){
                        long timeHave = (long)task.getResult().getValue();
                        int timeUse = Integer.parseInt(editHour.getText().toString());

                        if(timeUse==0){
                            tvError.setText("????????? 0?????? ????????? ??? ????????????.");
                            return;
                        }

                        if(timeUse>timeHave){
                            tvError.setText("????????? ??? ?????? ????????? ???????????????.");
                            return;
                        }

                        if(editTitle.getText().toString().length()<=5){
                            tvError.setText("????????? ?????? ????????????. 6??? ???????????? ??????????????????.");
                            return;
                        }

                        if(editText.getText().toString().length()<10){
                            tvError.setText("????????? ?????? ????????????. 10??? ???????????? ??????????????????.");
                            return;
                        }

                        LatLng position = (LatLng) getIntent().getExtras().get("position");
                        Utility.getInstance().RequestUploadToken(editTitle.getText().toString(), editText.getText().toString(), bitmapList.get(0), bitmapList.get(1), bitmapList.get(2),position, timeUse);
                        finish();
                    }else{
                        tvError.setText("??? ??? ?????? ????????? ??????????????????.");
                    }
                }
            });
        }else if(view==ib1){
            int index = 0;
            if(bitmapList.get(index)==null){
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(WriteTokenActivity.this);
            builder.setTitle("???????????? ?????????????????????????");
            builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ib1.setImageResource(0);
                    bitmapList.set(index,null);
                }
            });
            builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });

            builder.create().show();
        }else if(view==ib2){
            int index = 1;
            if(bitmapList.get(index)==null){
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(WriteTokenActivity.this);
            builder.setTitle("???????????? ?????????????????????????");
            builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ib1.setImageResource(0);
                    bitmapList.set(index,null);
                }
            });
            builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
        }else if(view==ib3){
            int index = 2;
            if(bitmapList.get(index)==null){
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(WriteTokenActivity.this);
            builder.setTitle("???????????? ?????????????????????????");
            builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ib1.setImageResource(0);
                    bitmapList.set(index,null);
                }
            });
            builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == 2) {
            try {
                InputStream is = getContentResolver().openInputStream(data.getData());
                Bitmap bitmap = BitmapFactory.decodeStream(is);

                int index;

                for(index=0;index<bitmapList.size();index++){
                    if(bitmapList.get(index)==null){
                        break;
                    }
                }

                if(index==bitmapList.size()){
                    AlertDialog.Builder builder = new AlertDialog.Builder(WriteTokenActivity.this);
                    builder.setTitle("???????????? ?????? ????????????");
                    builder.setMessage("???????????? 3????????? ?????? ???????????????.");
                    builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    builder.create().show();
                    return;
                }

                bitmapList.set(index,bitmap);
                switch (index){
                    case 0:
                        ib1.setImageBitmap(bitmap);
                        break;
                    case 1:
                        ib2.setImageBitmap(bitmap);
                        break;
                    case 2:
                        ib3.setImageBitmap(bitmap);
                        break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}