package com.firestorage.mhmdlogan.firestorage;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button btnChoose, btnUpload;
    private Uri filePath;
    private final int PICK_AUD_REQUEST = 71;
    FirebaseStorage storage;
    StorageReference storageReference;
    DatabaseReference mDataReference;
    ArrayList<String> al = new ArrayList<>();
    ArrayList<String> al2 = new ArrayList<>();
    ListView songsList;
    MediaPlayer player;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        player = new MediaPlayer();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        songsList = (ListView)findViewById(R.id.songsList);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Opening Exploler", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                chooseAud();
            }
        });

        mDataReference = FirebaseDatabase.getInstance().getReference("audios");

        mDataReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                al = collectnms((Map<String,Object>) dataSnapshot.getValue());
                al2 = collecturls((Map<String,Object>) dataSnapshot.getValue());
                songsList.setVisibility(View.VISIBLE);

                songsList.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, al));

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        songsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "Playing wait seconds ...", Toast.LENGTH_LONG).show();
                try {
                    player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    if (player.isPlaying()){
                        player.stop();
                        player.reset();
                    }
                        player.setDataSource(MainActivity.this, Uri.parse(al2.get(position)));
                        player.prepare();
                        player.start();
                } catch(Exception e) {
                    System.out.println(e.toString());
                }
            }
        });

    }

    private ArrayList<String> collecturls(Map<String, Object> value) {
        ArrayList<String> urls = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Object> entry : value.entrySet()){

            //Get user map
            Map singleUser = (Map) entry.getValue();
            urls.add((String) singleUser.get("url"));
            //Toast.makeText(MapsActivity.this, lats.get(i) +"|"+lngs.get(i), Toast.LENGTH_SHORT).show();

            i++;
        }
        return urls;

    }

    private ArrayList<String> collectnms(Map<String, Object> value) {
        ArrayList<String> nms = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Object> entry : value.entrySet()){

            //Get user map
            Map singleUser = (Map) entry.getValue();
            nms.add((String) singleUser.get("name"));
            //Toast.makeText(MapsActivity.this, lats.get(i) +"|"+lngs.get(i), Toast.LENGTH_SHORT).show();

            i++;
        }
        return nms;
    }


    private void chooseAud() {
        Intent intent = new Intent();
        intent.setType("audio/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Aud"), PICK_AUD_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_AUD_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null )
        {
            Toast.makeText(this, "Got data", Toast.LENGTH_SHORT).show();
            filePath = data.getData();
        }
        uploadAud();
    }

    private void uploadAud() {

        Toast.makeText(this, "start up proccess", Toast.LENGTH_SHORT).show();
        if(filePath != null)
        {
            Toast.makeText(this, "Uploading...", Toast.LENGTH_LONG).show();
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            StorageReference ref = storageReference.child("audios/"+ UUID.randomUUID().toString());
            ref.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            String name = taskSnapshot.getMetadata().getName();
                            String url = taskSnapshot.getDownloadUrl().toString();

                            // use Firebase Realtime Database to store [name + url]
                            writeNewAudInfoToDB(name, url);

                            mDataReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    al = collectnms((Map<String,Object>) dataSnapshot.getValue());
                                    al2 = collecturls((Map<String,Object>) dataSnapshot.getValue());
                                    songsList.setVisibility(View.VISIBLE);

                                    songsList.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, al));
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });

                            songsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    Toast.makeText(MainActivity.this, "Playing wait seconds ...", Toast.LENGTH_LONG).show();
                                    try {
                                        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                                        if (player.isPlaying()){
                                            player.stop();
                                            player.reset();
                                        }
                                            player.setDataSource(MainActivity.this, Uri.parse(al2.get(position)));
                                            player.prepare();
                                            player.start();
                                    } catch(Exception e) {
                                        System.out.println(e.toString());
                                    }
                                }
                            });

                            Toast.makeText(MainActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                    .getTotalByteCount());
                            progressDialog.setMessage("Uploaded "+(int)progress+"%");
                        }
                    });
        }
    }

    private void writeNewAudInfoToDB(String name, String url) {
        UploadInfo info = new UploadInfo(name, url);

        String key = mDataReference.push().getKey();
        mDataReference.child(key).setValue(info);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
