package jokanghyeon.inhatc.projectcodehorizon;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.sql.Time;
import java.util.Calendar;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener{

    EditText editId;
    EditText editPass;
    EditText editPassRepeat;
    EditText editNickname;
    Button btnRegister;

    TextView tvError;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        editId = findViewById(R.id.editRegisterID);
        editPass = findViewById(R.id.editRegisterPass);
        editPassRepeat = findViewById(R.id.editRegisterPassRepeat);
        editNickname = findViewById(R.id.editRegisterNickname);

        tvError = findViewById(R.id.tvError);

        btnRegister = findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if(view == btnRegister){
            if(!editPass.getText().toString().equals(editPassRepeat.getText().toString()) ) {
                tvError.setText("비밀번호가 일치하지 않습니다.\n다시 입력해주세요.");
                return;
            }

            if(editPass.getText().toString().length()<6){
                tvError.setText("비밀번호가 너무 짧습니다.\n6자 이상으로 입력해주세요");
                return;
            }

            auth.createUserWithEmailAndPassword(editId.getText().toString(),editPass.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()) {
                        FirebaseDatabase fb = FirebaseDatabase.getInstance();

                        DatabaseReference userRef= fb.getReference().child("users").child(auth.getUid());

                        userRef.child("TimeLeft").setValue(24);
                        userRef.child("LastConnected").setValue(Calendar.getInstance().getTime());
                        userRef.child("Nickname").setValue(editNickname.getText().toString());

                        Toast.makeText(getBaseContext(), "Finish", Toast.LENGTH_LONG).show();
                        finish();
                    }else{
                        try {
                            throw task.getException();
                        }catch (FirebaseAuthInvalidUserException exception){
                            tvError.setText("올바르지 않은 이메일입니다.");
                        }catch (FirebaseAuthUserCollisionException exception){
                            tvError.setText("이미 가입된 이메일입니다.");
                        }catch (Exception e){
                            tvError.setText("알 수 없는 오류가 발생하였습니다.");
                        }
                    }
                }
            });
        }
    }
}