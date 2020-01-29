package com.me.musicreality;

import android.Manifest;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.rendering.ViewSizer;
import com.google.ar.sceneform.ux.TransformableNode;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    static MediaPlayer myMediaPlayer;
    // Arfragment part
    CustomFragment arFragment;
    // Others
    ListView myListViewForSongs;
    String[] items;
    Button btn_next, btn_previous, btn_pause;
    Button backButton;
    TextView songTextLabel;
    SeekBar songSeekbar;
    int _position = 0;
    String sname;
    ArrayList<File> _mySongs;
    String _songName;

    Thread updateSeekBar;

    ViewFlipper flipper;

    View playerView;
    View songListView;

    boolean isPlaced;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isPlaced = false;

        // initializing arfragment
        arFragment = (CustomFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);

        // To hide the HandMotion
        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getPlaneDiscoveryController().setInstructionView(null);

        // setting on tap
        if (isPlaced == false) {
            arFragment.setOnTapArPlaneListener(((hitResult, plane, motionEvent) -> {
                createViewRenderable(hitResult.createAnchor());
            }));
        }


    }

    private void createViewRenderable(Anchor anchor) {
        ViewRenderable
                .builder()
                .setView(this, R.layout.view_augmented)
                .build()
                .thenAccept(viewRenderable -> {
                    addToScene(viewRenderable, anchor);
                });
    }

    private void addToScene(ViewRenderable viewRenderable, Anchor anchor) {

        flipper = viewRenderable.getView().findViewById(R.id.flipper);

        playerView = View.inflate(this, R.layout.player_view, null);
        songListView = View.inflate(this, R.layout.list_song_view, null);
        flipper.addView(songListView);
        flipper.addView(playerView);
        playerView.setVisibility(View.INVISIBLE);

        myListViewForSongs = songListView.findViewById(R.id.mySongListView);

        runtimePermission();

        viewRenderable.setVerticalAlignment(ViewRenderable.VerticalAlignment.CENTER);
        viewRenderable.setSizer(new ViewSizer() {
            @Override
            public Vector3 getSize(View view) {
                return new Vector3(1f, 1f, 1f);
            }
        });

        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
        transformableNode.setParent(anchorNode);
        transformableNode.setRenderable(viewRenderable);
        anchorNode.setLocalPosition(new Vector3(0f, 10f, 0f));
        anchorNode.setLocalRotation(new Quaternion(new Vector3(1f, 0f, 0f), .9f));
        Log.d("MusicReality", "" + anchorNode.getLocalRotation().toString() + "  ");
        arFragment.getArSceneView().getScene().addChild(anchorNode);
        transformableNode.select();
        // hide plane dots
        arFragment.getArSceneView().getPlaneRenderer().setVisible(false);
        isPlaced = true;


    }

    public void runtimePermission() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        display();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    public ArrayList<File> findSong(File file) {
        if (file != null) {
            ArrayList<File> arrayList = new ArrayList<>();
            File[] files = file.listFiles();
            if (files != null) {
                Log.d("Music Reality", "Not Null encountered");
                for (File singleFile : files) {
                    if (singleFile.isDirectory() && !singleFile.isHidden()) {
                        arrayList.addAll(findSong(singleFile));
                    } else {
                        if (singleFile.getName().endsWith(".mp3") ||
                                singleFile.getName().endsWith(".wav")) {
                            arrayList.add(singleFile);
                        }
                    }
                }


                return arrayList;
            } else {
                Log.d("Music Reality", "Encoundtered null");
                Toast.makeText(this, "Hello null", Toast.LENGTH_SHORT).show();
                return null;
            }
        }
        return null;
    }

    void display() {
        final ArrayList<File> mySongs = findSong(Environment.getExternalStorageDirectory());
        if (mySongs != null) {
            Log.d("Music Reality", "" + mySongs.size());

            items = new String[mySongs.size()];
            for (int i = 0; i < mySongs.size(); i++) {
                items[i] = mySongs.get(i).getName().replace(".mp3", "").replace(".wav", "");
            }
            ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
            myListViewForSongs.setAdapter(myAdapter);

            myListViewForSongs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    String songName = myListViewForSongs.getItemAtPosition(position).toString();
                    _position = position;
                    _mySongs = mySongs;
                    _songName = songName;
                    toggleVisibility();
                    FlipAction(position);

                }
            });
        }
    }

    private void toggleVisibility() {
        if (songListView != null && playerView != null) {
            if (songListView.getVisibility() == View.VISIBLE && playerView.getVisibility() == View.INVISIBLE) {
                songListView.setVisibility(View.INVISIBLE);
                playerView.setVisibility(View.VISIBLE);
            } else {
                songListView.setVisibility(View.VISIBLE);
                playerView.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void FlipAction(int position) {

        getSupportActionBar().setTitle("Now playing");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        backButton = playerView.findViewById(R.id.backButton);
        btn_next = playerView.findViewById(R.id.next);
        btn_previous = playerView.findViewById(R.id.previous);
        btn_pause = playerView.findViewById(R.id.pause);
        songTextLabel = playerView.findViewById(R.id.songLabel);
        songSeekbar = playerView.findViewById(R.id.seekBar);


        updateSeekBar = new Thread() {
            @Override
            public void run() {
                int totalDuration = myMediaPlayer.getDuration();
                int currentPosition = 0;
                while (currentPosition < totalDuration) {
                    try {
                        sleep(500);
                        currentPosition += myMediaPlayer.getCurrentPosition();
                        songSeekbar.setProgress(currentPosition);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };

        if (myMediaPlayer != null) {
            myMediaPlayer.stop();
            myMediaPlayer.release();

        }
        sname = _mySongs.get(_position).toString();
        String songName = _songName;
        songTextLabel.setText(songName);

        songTextLabel.setSelected(true);


        Uri u = Uri.parse(_mySongs.get(position).toString());

        myMediaPlayer = MediaPlayer.create(getApplicationContext(), u);
        myMediaPlayer.start();
        songSeekbar.setMax(myMediaPlayer.getDuration());

        updateSeekBar.start();

        songSeekbar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.colorPrimary),
                PorterDuff.Mode.MULTIPLY);
        songSeekbar.getThumb().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);


        songSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                myMediaPlayer.seekTo(seekBar.getProgress());
            }
        });

        btn_pause.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                songSeekbar.setMax(myMediaPlayer.getDuration());
                if (myMediaPlayer.isPlaying()) {
                    btn_pause.setBackgroundResource(R.drawable.icon_pause);
                    myMediaPlayer.pause();
                } else {
                    btn_pause.setBackgroundResource(R.drawable.icon_play);
                    myMediaPlayer.start();
                }
            }
        }));

        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myMediaPlayer.stop();
                myMediaPlayer.release();
                _position = ((_position + 1) % _mySongs.size());

                Uri u = Uri.parse(_mySongs.get(_position).toString());
                myMediaPlayer = MediaPlayer.create(playerView.getContext(), u);
                sname = _mySongs.get(_position).getName();
                songTextLabel.setText(sname);
                myMediaPlayer.start();


            }
        });

        btn_previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myMediaPlayer.stop();
                myMediaPlayer.release();
                _position = (_position - 1);
                if (_position < 0) {
                    _position = _mySongs.size() - 1;
                }

                Uri u = Uri.parse(_mySongs.get(_position).toString());
                myMediaPlayer = MediaPlayer.create(playerView.getContext(), u);
                sname = _mySongs.get(_position).getName();
                songTextLabel.setText(sname);
                myMediaPlayer.start();

            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleVisibility();
            }
        });


    }

}
