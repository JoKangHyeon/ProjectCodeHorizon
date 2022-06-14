package jokanghyeon.inhatc.projectcodehorizon;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
/*

Variant: de

 */

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    EditText editID;
    EditText editPass;
    Button btnLogin;
    Button btnRegister;
    ImageButton btnGoogle;

    private FirebaseAuth mAuth;
    GoogleSignInClient mGoogleSigninClient;

    TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);

        mAuth = FirebaseAuth.getInstance();

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
        }
        else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},1);
        }


        if(mAuth.getCurrentUser()!=null){
            Intent mapsIntent = new Intent(LoginActivity.this, MapsActivity.class);
            startActivity(mapsIntent);
            return;
        }


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(Utility.getInstance().GetMetadata(LoginActivity.this, "LoginActivity.API_KEY"))
                .requestEmail()
                .build();
        mGoogleSigninClient = GoogleSignIn.getClient(this, gso);

        setContentView(R.layout.activity_login);

        editID = findViewById(R.id.editID);
        editPass = findViewById(R.id.editPass);

        btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(this);

        btnRegister = findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();

        tvError = findViewById(R.id.tvError);

        btnGoogle = findViewById(R.id.btnGoogle);
        btnGoogle.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

        if(view == btnLogin){
            mAuth.signInWithEmailAndPassword(editID.getText().toString(),editPass.getText().toString()).addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    Intent mapsIntent = new Intent(LoginActivity.this, MapsActivity.class);
                    editID.setText("");
                    editPass.setText("");
                    startActivity(mapsIntent);
                }else{
                    tvError.setText("로그인에 실패했습니다.");
                }
            });
        }else if(view==btnRegister){
            Intent registerIntent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(registerIntent);
        }else if(view==btnGoogle){
            Intent siginInIntent = mGoogleSigninClient.getSignInIntent();

            startActivityForResult(siginInIntent, 1);

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == 1) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);

                AuthCredential auth = GoogleAuthProvider.getCredential(account.getIdToken(),null);
                mAuth.signInWithCredential(auth).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            FirebaseDatabase.getInstance().getReference().child("users").child(mAuth.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if(!snapshot.exists())
                                        Utility.getInstance().AccountInit("NewUser",getDrawable(R.drawable.basecamp));
                                    Intent mapsIntent = new Intent(LoginActivity.this, MapsActivity.class);
                                    startActivity(mapsIntent);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });

                        }else{
                            tvError.setText("로그인에 실패했습니다.");
                        }
                    }
                });

            } catch (ApiException e) {
            }
        }
    }
}
