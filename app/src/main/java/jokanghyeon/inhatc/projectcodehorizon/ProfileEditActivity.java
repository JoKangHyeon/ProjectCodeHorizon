package jokanghyeon.inhatc.projectcodehorizon;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;

import java.io.InputStream;

public class ProfileEditActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnCancel;
    private Button btnChangeProfilePic;
    private Button btnFinish;
    private Button btnDeleteAccount;


    private ImageView ivProfile;

    private EditText editNickname;
    private EditText editPass;
    private EditText editRepeatPass;

    private TextView tvError;

    private FirebaseAuth mAuth;
    private FirebaseDatabase mDB;
    private FirebaseStorage mStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        btnCancel = findViewById(R.id.btnEditCancel);
        btnCancel.setOnClickListener(this);

        btnChangeProfilePic = findViewById(R.id.btnEditProfileChange);
        btnChangeProfilePic.setOnClickListener(this);

        btnFinish = findViewById(R.id.btnEditFinish);
        btnFinish.setOnClickListener(this);

        btnDeleteAccount = findViewById(R.id.btnEditDeleteAccount);
        btnDeleteAccount.setOnClickListener(this);

        ivProfile = findViewById(R.id.ivEditProfileImage);

        editNickname = findViewById(R.id.editEditNickname);
        editPass = findViewById(R.id.editEditPass);
        editRepeatPass = findViewById(R.id.editEditRepeatPass);

        tvError = findViewById(R.id.tvEditError);

        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseDatabase.getInstance();
        mStorage = FirebaseStorage.getInstance();


        Utility.getInstance().SetTextViewNickname(mAuth.getUid(),editNickname);
        Utility.getInstance().SetProfilePic(mAuth.getUid(),ivProfile);

        if(mAuth.getCurrentUser().getProviderId().equals("google.com")){
            editPass.setEnabled(false);
            editRepeatPass.setEnabled(false);
        }
    }

    @Override
    public void onClick(View view) {
        if(view==btnCancel){
            finish();
        }else if(view==btnChangeProfilePic){
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");

            startActivityForResult(Intent.createChooser(intent,"프로필 사진을 골라주세요"),2);
        }else if(view==btnFinish){
            if(editNickname.getText().toString().length()<=1){
                tvError.setText("닉네임이 너무 짧습니다. 2자 이상으로 입력해주세요");
                return;
            }else if(editNickname.getText().toString().length()>=10){
                tvError.setText("닉네임이 너무 깁니다. ");
                return;
            }

            if(editPass.getText().toString().length()!=0 || editRepeatPass.getText().toString().length()!=0){

                if(editPass.getText().toString().length()<=6){
                    tvError.setText("비밀번호가 너무 짧습니다 7자 이상으로 입력해주세요.");
                    return;
                }

                if(editPass.getText().toString().equals(editRepeatPass.getText().toString())){
                    mAuth.getCurrentUser().updatePassword(editPass.getText().toString());
                }else{
                    tvError.setText("비밀번호가 일치하지 않습니다.");
                    return;
                }

            }

            mDB.getReference().child("users").child(mAuth.getUid()).child("public").child("Nickname").setValue(editNickname.getText().toString());

            finish();
        }else if(view == btnDeleteAccount){

            Utility.getInstance().RemoveData();

            if(mAuth.getCurrentUser().getProviderId().equals("google.com")) {

                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(Utility.getInstance().GetMetadata(ProfileEditActivity.this, "LoginActivity.API_KEY"))
                        .requestEmail()
                        .build();
                GoogleSignIn.getClient(ProfileEditActivity.this, gso).signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mAuth.getCurrentUser().delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                mAuth.signOut();
                                PackageManager packageManager = getPackageManager();
                                Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());
                                ComponentName componentName = intent.getComponent();
                                Intent mainIntent = Intent.makeRestartActivityTask(componentName);
                                startActivity(mainIntent);
                                Runtime.getRuntime().exit(0);
                            }
                        });
                    }
                });
            }else{
                mAuth.getCurrentUser().delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        AuthUI.getInstance().signOut(ProfileEditActivity.this).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                PackageManager packageManager = getPackageManager();
                                Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());
                                ComponentName componentName = intent.getComponent();
                                Intent mainIntent = Intent.makeRestartActivityTask(componentName);
                                startActivity(mainIntent);
                                Runtime.getRuntime().exit(0);
                            }
                        });
                    }
                });
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK && requestCode==2){
            try {
                InputStream is = getContentResolver().openInputStream(data.getData());
                Bitmap bitmap = BitmapFactory.decodeStream(is);

                int width = bitmap.getWidth();
                int height = bitmap.getHeight();

                if(width<height){
                   int startFrom = (height-width)/2;

                   bitmap = Bitmap.createBitmap(bitmap,0,startFrom,width,width);
                }else{
                    int startFrom = (width-height)/2;
                    bitmap = Bitmap.createBitmap(bitmap,startFrom,0,height,height);
                }

                if(bitmap.getWidth()>1024){
                    bitmap = Bitmap.createScaledBitmap(bitmap,1024,1024,true);
                }

                Utility.getInstance().UploadPrpfileImage(bitmap,ivProfile);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        super.onActivityResult(requestCode,resultCode,data);
    }
}