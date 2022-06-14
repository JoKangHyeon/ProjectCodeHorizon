package jokanghyeon.inhatc.projectcodehorizon;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReadTokenActivity extends AppCompatActivity implements View.OnClickListener {

    RecyclerView recyclerView;

    Button btnFinish;
    Button btnAddTime;
    Button btnAddReply;

    TextView tvTitle;
    TextView tvTimeLeft;
    TextView tvText;
    TextView tvTimeHave;
    TextView tvWriter;

    EditText editHour;
    EditText editReply;

    List<ImageView> ivList;

    FirebaseDatabase mDB;

    String tokenId;

    ReplyAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_token);

        recyclerView = findViewById(R.id.recyclerViewReply);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnFinish = findViewById(R.id.btnReadFinish);
        btnFinish.setOnClickListener(this);

        btnAddTime = findViewById(R.id.btnReadTimeAdd);
        btnAddTime.setOnClickListener(this);

        btnAddReply = findViewById(R.id.btnReadAddReply);
        btnAddReply.setOnClickListener(this);

        tvTitle = findViewById(R.id.tvReadTitleValue);
        tvTimeLeft = findViewById(R.id.tvReadTimeLeft);
        tvText = findViewById(R.id.tvReadText);
        tvTimeHave = findViewById(R.id.tvReadTimeHave);
        tvWriter = findViewById(R.id.tvReadWriterValue);

        editHour = findViewById(R.id.editReadHour);
        editReply = findViewById(R.id.editReadReply);

        ivList = new ArrayList<>();
        ivList.add(findViewById(R.id.ivReadImage1));
        ivList.add(findViewById(R.id.ivReadImage2));
        ivList.add(findViewById(R.id.ivReadImage3));

        mDB = FirebaseDatabase.getInstance();
        tokenId = (String) getIntent().getExtras().get("id");


        mDB.getReference("users").child(FirebaseAuth.getInstance().getUid()).child("private").child("TimeLeft").get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
            @Override
            public void onSuccess(DataSnapshot dataSnapshot) {
                long timeLeft = (long) dataSnapshot.getValue();
                tvTimeHave.setText("/"+timeLeft);
            }
        });

        mDB.getReference().child("map_data").child(tokenId).get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
            @Override
            public void onSuccess(DataSnapshot dataSnapshot) {
                mDB.getReference().child("users").child((String) dataSnapshot.child("Creater").getValue()).child("public").child("Nickname").get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                    @Override
                    public void onSuccess(DataSnapshot dataSnapshot) {
                        tvWriter.setText((String)dataSnapshot.getValue());
                    }
                });

                tvTitle.setText((String)dataSnapshot.child("ThreadTitle").getValue());
                tvText.setText((String)dataSnapshot.child("ThreadText").getValue());
                long imageCount;
                try {
                    imageCount = (long)dataSnapshot.child("ImageCount").getValue();
                }
                catch (NullPointerException e){
                    imageCount=0;
                }

                final long MAX_FILE_SIZE = 1024*1024*30;
                FirebaseStorage fbs = FirebaseStorage.getInstance();
                StorageReference sr = fbs.getReference().child("map");
                for(int i=0;i<imageCount;i++){
                    ImageView target=  ivList.get(i);
                    sr.child(tokenId).child(i+"").getBytes(MAX_FILE_SIZE).addOnSuccessListener(bytes -> {
                        Bitmap ret = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                        target.setImageBitmap(ret);
                    }).addOnFailureListener(e -> target.setImageResource(0));
                }

                tvTimeLeft.setText( (long)dataSnapshot.child("TimeLeft").getValue() + "시간\n남음" );
            }
        });

        mDB.getReference().child("map_data").child(tokenId).child("Threads").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Reply> replyList = new ArrayList<>();
                for(DataSnapshot childSnapshot : snapshot.getChildren() ){
                    Reply reply = new Reply((String)childSnapshot.child("Writer").getValue(), (String)childSnapshot.child("Data").getValue(),childSnapshot.getKey());
                    replyList.add(reply);
                }

                Collections.reverse(replyList);
                recyclerView.setAdapter(new ReplyAdapter(replyList));

                //public Reply(String uid,  String text, Time time){
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void onClick(View view) {
        if(view==btnAddReply){
            if(editReply.getText().toString().length()<=5){
                AlertDialog.Builder builder = new AlertDialog.Builder(ReadTokenActivity.this);
                builder.setTitle("덧글이 너무 짧습니다. 6자 이상으로 입력해주세요.");
                builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                builder.create().show();
                return;
            }

            DatabaseReference threadRef= mDB.getReference().child("map_data").child(tokenId).child("Threads").push();
            Map<String,Object> data= new HashMap<>();

            data.put("Writer",FirebaseAuth.getInstance().getUid());
            data.put("Data",editReply.getText().toString());
            data.put("UploadDate", Calendar.getInstance().getTime());

            threadRef.setValue(data);

            editReply.setText("");
        }else if(view==btnFinish){
            finish();
        }else if(view==btnAddTime){

            int timeAdd = Integer.parseInt( editHour.getText().toString());

            if(timeAdd<=0){
                return;
            }

            mDB.getReference("users").child(FirebaseAuth.getInstance().getUid()).child("private").child("TimeLeft").get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                @Override
                public void onSuccess(DataSnapshot dataSnapshot) {
                    long timeLeft = (long) dataSnapshot.getValue();

                    if(timeLeft>timeAdd){
                        mDB.getReference().child("map_data").child(tokenId).child("TimeLeft").get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                            @Override
                            public void onSuccess(DataSnapshot dataSnapshot) {
                                long newTime = (long)dataSnapshot.getValue() + timeAdd;
                                mDB.getReference().child("map_data").child(tokenId).child("TimeLeft").setValue(newTime).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        mDB.getReference("users").child(FirebaseAuth.getInstance().getUid()).child("private").child("TimeLeft").setValue(timeLeft-timeAdd).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                tvTimeLeft.setText(newTime + "시간\n남음");
                                                tvTimeHave.setText("/"+(timeLeft-timeAdd));
                                                editHour.setText("");
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }else{
                        AlertDialog.Builder builder = new AlertDialog.Builder(ReadTokenActivity.this);
                        builder.setTitle("시간이 부족합니다.");
                        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        builder.create().show();
                    }
                }
            });
        }
    }


    //RecyclerView Adapter Class
    private class Reply {
        private String text;
        private String writerUID;
        private String replyID;

        public Reply(String uid, String text, String replyID){
            this.text = text;
            this.writerUID = uid;
            this.replyID=replyID;
        }

    }



    public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ReplyHolder>{

        private List<Reply> replyList;

        public ReplyAdapter(List<Reply> replyList){
            this.replyList = replyList;
        }

        @NonNull
        @Override
        public ReplyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = inflater.inflate(R.layout.recycler_contents,parent,false);
            ReplyAdapter.ReplyHolder rh = new ReplyHolder(view);

            return rh;
        }

        @Override
        public void onBindViewHolder(@NonNull ReplyHolder holder, int position) {
            holder.Setup(replyList.get(position));
        }

        @Override
        public int getItemCount() {
            return replyList.size();
        }

        class ReplyHolder extends RecyclerView.ViewHolder {
            TextView tvWriter;
            TextView tvText;
            Button btnDel;

            String writerUID;

            public void Setup(Reply reply){

                tvText.setText(reply.text);
                this.writerUID = reply.writerUID;
                FirebaseDatabase.getInstance().getReference("users").child(writerUID).child("public").child("Nickname").get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                    @Override
                    public void onSuccess(DataSnapshot dataSnapshot) {
                        tvWriter.setText((String)dataSnapshot.getValue());
                    }
                });
                if(writerUID.equals(FirebaseAuth.getInstance().getUid())){
                    btnDel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //mDB.getReference().child("map_data").child(tokenId).child("Threads").child(reply.replyID).child("Data").setValue(null);
                            //mDB.getReference().child("map_data").child(tokenId).child("Threads").child(reply.replyID).child("UploadDate").setValue(null);
                            mDB.getReference().child("map_data").child(tokenId).child("Threads").child(reply.replyID).removeValue();
                        }
                    });
                }else{
                    btnDel.setVisibility(View.GONE);
                }
            }

            public ReplyHolder(@NonNull View itemView) {
                super(itemView);

                this.tvWriter=itemView.findViewById(R.id.tvReplyWriter);
                this.tvText=itemView.findViewById(R.id.tvReplyText);

                this.btnDel  = itemView.findViewById(R.id.btnReplyDel);

                this.writerUID = writerUID;
            }


        }
    }
}

