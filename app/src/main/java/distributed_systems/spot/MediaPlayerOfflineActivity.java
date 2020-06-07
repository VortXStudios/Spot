package distributed_systems.spot;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import distributed_systems.spot.Code.Value;

public class MediaPlayerOfflineActivity extends AppCompatActivity {

    private ImageButton play;
    private ImageButton stop;
    private ImageButton forward;
    private ImageButton backward;
    private TextView titleOfSong;
    private TextView elapsedTime;
    private TextView remainedTime;
    private SeekBar songPrgs;
    private MediaPlayer mp;
    private boolean initialized;
    private Handler hdlr ;
    private  int oTime =0, sTime =0, eTime =0, fTime = 3000, bTime = 3000;
    private ImageView imageView;
    private String trackName;
    private MediaMetadataRetriever media;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player_offline);
        hdlr = new Handler();
        stop = (ImageButton) findViewById(R.id.imageButton9);
        play = (ImageButton) findViewById(R.id.imageButton8);
        forward = (ImageButton) findViewById(R.id.imageButton10);
        backward = (ImageButton) findViewById(R.id.imageButton7);
        songPrgs = (SeekBar) findViewById(R.id.seekBar2);
        titleOfSong = (TextView) findViewById(R.id.textView6);
        elapsedTime = (TextView) findViewById(R.id.textView4);
        remainedTime = (TextView) findViewById(R.id.textView5);
        imageView = (ImageView) findViewById(R.id.imageView);

    }

    @Override
    public void onStart(){
        super.onStart();
        trackName = (String) getIntent().getStringExtra("Music file name");
        Log.e("check",trackName);
        imageView.setVisibility(View.INVISIBLE);
        initialized = true;
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(initialized){
                    mp = new MediaPlayer();
                    final List<Value> allTracks = new ArrayList<>();
                    final List<Integer> allTracksDuration = new ArrayList<>();
                    initialized = false;
                    titleOfSong.setVisibility(View.VISIBLE);
                    titleOfSong.setText(trackName.substring(0,trackName.indexOf(".mp3")));
                    playMp3();
                    setTimer();
                    mp.start();
                    play.setImageResource(android.R.drawable.ic_media_pause);
                    mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            stopPlaying();
                        }
                    });
                }
                else{
                    if (mp.isPlaying()) {
                        if (mp != null) {
                            mp.pause();
                            // Changing button image to play button
                            play.setImageResource(android.R.drawable.ic_media_play);
                        }
                    }
                    else {
                        if (mp != null && !initialized) {
                            mp.start();
                            // Changing button image to pause button
                            play.setImageResource(android.R.drawable.ic_media_pause);
                        }
                    }

                }
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopPlaying();
            }
        });

        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mp!=null) {
                    if ((sTime + fTime) <= eTime) {
                        sTime = sTime + fTime;
                        mp.seekTo(sTime);
                    } else {
                        Toast.makeText(getApplicationContext(), "Cannot jump forward 3 seconds", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        backward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mp!=null) {
                    if ((sTime - bTime) > 0) {
                        sTime = sTime - bTime;
                        mp.seekTo(sTime);
                    } else {
                        Toast.makeText(getApplicationContext(), "Cannot jump backward 3 seconds", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        songPrgs.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                if(mp!=null){
                    mp.seekTo(progressChangedValue);
                    sTime = mp.getCurrentPosition();
                    elapsedTime.setText(String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(sTime),
                            TimeUnit.MILLISECONDS.toSeconds(sTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(sTime))) );
                    songPrgs.setProgress(sTime);
                }

            }
        });
    }


    private void setTimer(){
        eTime = mp.getDuration();
        Log.e("check", String.valueOf(eTime));
        sTime = mp.getCurrentPosition();
        if(oTime == 0){
            songPrgs.setMax(eTime);
            oTime =1;
        }
        remainedTime.setText(String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(eTime),
                TimeUnit.MILLISECONDS.toSeconds(eTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS. toMinutes(eTime))) );
        elapsedTime.setText(String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(sTime),
                TimeUnit.MILLISECONDS.toSeconds(sTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS. toMinutes(sTime))) );
        songPrgs.setProgress(sTime);
        hdlr.postDelayed(UpdateSongTime, 100);
    }

    private Runnable UpdateSongTime = new Runnable() {
        @Override
        public void run() {
            sTime = mp.getCurrentPosition();
            elapsedTime.setText(String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(sTime),
                    TimeUnit.MILLISECONDS.toSeconds(sTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(sTime))) );
            songPrgs.setProgress(sTime);
            hdlr.postDelayed(this, 100);
        }
    };




    private void playMp3() {
        try {
            mp.reset();
            String filepath = "/saved songs/";
            String path = getExternalFilesDir(filepath).getAbsolutePath()+"/"+trackName;
            FileInputStream fis = new FileInputStream(new File(path));
            mp.setDataSource(fis.getFD());
            mp.prepare();
            media = new MediaMetadataRetriever();
            media.setDataSource(fis.getFD());
            byte [] embeddedPicture = media.getEmbeddedPicture();
            if(embeddedPicture!=null) {
                InputStream is = new ByteArrayInputStream(embeddedPicture);
                Bitmap b = BitmapFactory.decodeStream(is);
                imageView.setImageBitmap(Bitmap.createScaledBitmap(b, 200, 200, false));

            }else
            {
                imageView.setImageResource(R.drawable.music); //any default cover resourse folder
            }
            imageView.setAdjustViewBounds(true);
            imageView.setVisibility(View.VISIBLE);
        } catch (IOException ex) {
            String s = ex.toString();
            ex.printStackTrace();
        }
    }



    private void stopPlaying(){
        // If media player is not null then try to stop it
        if(mp!=null){
            hdlr.removeCallbacks(UpdateSongTime);
            mp.stop();
            mp.release();
            mp = null;
            media.release();
            media =null;
            sTime = 0;
            play.setImageResource(android.R.drawable.ic_media_play);
            Toast.makeText(MediaPlayerOfflineActivity.this, "Stop player...", Toast.LENGTH_SHORT).show();
        }
        initialized = true;
    }

    @Override
    protected void onStop(){
        stopPlaying();
        super.onStop();
    }


    @Override
    public void onBackPressed() {
        stopPlaying();
        Intent i = new Intent(getApplicationContext(), StartActivity.class);
        startActivityForResult(i, 0);
    }

}
