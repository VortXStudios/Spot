package distributed_systems.spot;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import distributed_systems.spot.Code.ArtistName;
import distributed_systems.spot.Code.Info;
import distributed_systems.spot.Code.Value;


//TODO ftiakse ta catch na mhn kanoun print alla log.e
//TODO REFRESH BUTTON

public class MainActivity extends AppCompatActivity {

    private Button btn1;
    private Button btn2;
    private ListView viewArtists;
    private ListView viewSongs;
    //private boolean hasChoose = false;
    private String selectedArtist;
    private String selectedSong;
    private ImageButton play;
    private ImageButton stop;
    private ImageButton forward;
    private ImageButton backward;
    private ImageButton refresh;
    private TextView titleOfSong;
    private TextView elapsedTime;
    private TextView remainedTime;
    private SeekBar songPrgs;
    private MediaPlayer mp;
    private boolean initialized;
    private Handler hdlr ;
    private int pos;
    private ArrayList<Value> allTracks;
    ArrayList<Integer> allTracksDuration;
    private int pass = 0;
    private  int oTime =0, sTime =0, eTime =0, fTime = 3000, bTime = 3000, sTime2=0;
    private MediaPlayer mpNext;
    private int playerNow;
    private AsyncTaskRunner runner;
    private  BlockingQueue<String> inputQueue;
    private boolean isExit;
    private String stage;
    private ImageView cover;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hdlr = new Handler();
        btn1 = (Button) findViewById(R.id.button);
        btn2 = (Button) findViewById(R.id.button2);
        viewArtists = (ListView) findViewById(R.id.viewArtists);
        viewSongs = (ListView) findViewById(R.id.viewSongs);
        stop = (ImageButton) findViewById(R.id.imageButton3);
        play = (ImageButton) findViewById(R.id.imageButton2);
        forward = (ImageButton) findViewById(R.id.imageButton4);
        backward = (ImageButton) findViewById(R.id.imageButton);
        refresh = (ImageButton) findViewById(R.id.imageButton6);
        songPrgs = (SeekBar) findViewById(R.id.seekBar);
        titleOfSong = (TextView) findViewById(R.id.textView);
        elapsedTime = (TextView) findViewById(R.id.textView2);
        remainedTime = (TextView) findViewById(R.id.textView3);
        cover = (ImageView) findViewById(R.id.imageView3);
    }
    //TODO SET VISIBLE AND CLICKABLE PLAYER BUTTONS AND UI AFTER USER CHOOSE A SONG
    public void onStart() {
        super.onStart();
        String ip = (String) getIntent().getStringExtra("ip");
        int port = (int) getIntent().getIntExtra("port",5000);
        mp = null;
        mpNext = null;
        allTracks = new ArrayList<>();
        allTracksDuration = new ArrayList<>();
        isExit = false;
        viewArtists.setVisibility(View.INVISIBLE);
        viewSongs.setVisibility(View.INVISIBLE);
        btn1.setVisibility(View.INVISIBLE);
        btn2.setVisibility(View.INVISIBLE);
        btn1.setClickable(false);
        btn2.setClickable(false);
        disablePlayerUI();
        stage = "init";
        //final BlockingQueue<String> inputQueue = new LinkedBlockingDeque<>();
        inputQueue = new LinkedBlockingDeque<>();
        final BlockingQueue<Value> chunksQueue = new LinkedBlockingDeque<>();
        List<BlockingQueue> list = new ArrayList<>();
        inputQueue.add(ip);
        inputQueue.add(Integer.toString(port));
        list.add(inputQueue);
        list.add(chunksQueue);
        runner = new AsyncTaskRunner();
        runner.execute(list);
        //state = false;
        selectedArtist = null;
        selectedSong = null;
        viewArtists.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,long arg3) {
                view.setSelected(true);
                view.setActivated(true);
                //viewArtists.setItemChecked(position, true);
                selectedArtist =(String) (viewArtists.getItemAtPosition(position));

            }
        });
        //TODO CHECK THAT USER HAD SELECTED BEFORE PUSH BUTTON
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //hasChoose = true;
                inputQueue.clear();
                chunksQueue.clear();
                if(selectedArtist!=null) {
                    try {
                        inputQueue.put(selectedArtist);
                        stage = "artist";
                        selectedArtist = null;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    Toast.makeText(MainActivity.this, "Not selected artist", Toast.LENGTH_SHORT).show();
                }
            }
        });

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    inputQueue.put("");
                    stage = "init";
                    inputQueue.clear();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        viewSongs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,long arg3) {
                view.setSelected(true);
                view.setActivated(true);
                selectedSong =(String) (viewSongs.getItemAtPosition(position));

            }
        });
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectedSong!=null) {
                    //hasChoose = true;
                    if (mp != null) {
                        try {
                            mp.release();
                            mp = null;
                            play.setImageResource(android.R.drawable.ic_media_play);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (mpNext != null) {
                        try {
                            mpNext.release();
                            mpNext = null;
                            play.setImageResource(android.R.drawable.ic_media_play);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    mp = new MediaPlayer();
                    mpNext = new MediaPlayer();
                    initialized = true;
                    playerNow=0;
                    chunksQueue.clear();
                    inputQueue.clear();
                    //state=false;
                    try {
                        inputQueue.put(selectedSong);
                        stage = "song";
                        enablePlayerUI();
                        titleOfSong.setText(selectedSong);
                        selectedSong = null;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    Toast.makeText(MainActivity.this, "Not selected song", Toast.LENGTH_SHORT).show();
                }
            }
        });
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(initialized){
                    pos=0;
                    allTracks.clear();
                    allTracksDuration.clear();
                    pass=0;
                    oTime = 0;
                    sTime2 = 0;
                    initialized = false;
                    //TODO MAYBE INITIALIZE HERE MediaPlayer
                    //mp = new MediaPlayer();
                    //mpNext = new MediaPlayer();
                    //DOESN'T PLAY WITH MEDIAPLAYER SET IN ASYNCTASK
                    //AsyncTaskMediaPlayer media = new AsyncTaskMediaPlayer();
                    //media.execute(chunksQueue);
                    try {
                        Value value = (Value) chunksQueue.take();
                        Log.e("check", "take track");
                        allTracks.add(value);
                        setCover(value);
                        playMp3(value.getMusicFile().getMusicFileExtract(),0);
                        setTimer(value.getMusicFile().getOverallDuration());
                        mp.start();
                        play.setImageResource(android.R.drawable.ic_media_pause);
                        value = (Value) chunksQueue.take();
                        Log.e("check", "take track");
                        allTracks.add(value);
                        playMp3(value.getMusicFile().getMusicFileExtract(),1);
                        mp.setNextMediaPlayer(mpNext);
                        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mediaPlayer) {
                                sTime2 += mp.getCurrentPosition();
                                Log.e("check", String.valueOf(mp.getCurrentPosition()));
                                Log.e("check", "1");
                                pass++;
                                playerNow = 1;
                                if(mp!=null){
                                    if(pos<0){
                                        Log.e("check", "pos");
                                        pos++;
                                        int p = allTracks.size()-1+pos;
                                        playNextChunk(allTracks.get(p),0);
                                        mpNext.setNextMediaPlayer(mp);
                                    }
                                    else {
                                        try {
                                            allTracksDuration.add(mp.getCurrentPosition());
                                            Value value = (Value) chunksQueue.take();
                                            Log.e("check", "take track");
                                            allTracks.add(value);
                                            //!value.getFailure()
                                            if (!value.getFailure()) {
                                                Log.e("check", "2");
                                                playNextChunk(value, 0);
                                                mpNext.setNextMediaPlayer(mp);
                                            } else {
                                                Log.e("check", "3");
                                                play.setImageResource(android.R.drawable.ic_media_play);
                                                endOfSong(0);
                                            }
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                            }
                        });
                        mpNext.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mediaPlayer) {
                                if(mpNext!=null){
                                    sTime2 += mpNext.getCurrentPosition();
                                    Log.e("check", String.valueOf(mpNext.getCurrentPosition()));
                                    pass++;
                                    Log.e("check", "1 next");
                                    playerNow = 0;
                                    if(pos<0){
                                        Log.e("check", "pos next");
                                        pos++;
                                        int p = allTracks.size()-1+pos;
                                        playNextChunk(allTracks.get(p),1);
                                        mp.setNextMediaPlayer(mpNext);
                                    }
                                    else {
                                        try {
                                            allTracksDuration.add(mpNext.getCurrentPosition());
                                            Value value = (Value) chunksQueue.take();
                                            Log.e("check", "take track");
                                            allTracks.add(value);
                                            //!value.getFailure()
                                            if (!value.getFailure()) {
                                                Log.e("check", "2 next");
                                                playNextChunk(value, 1);
                                                mp.setNextMediaPlayer(mpNext);
                                            } else {
                                                Log.e("check", "3 next");
                                                play.setImageResource(android.R.drawable.ic_media_play);
                                                endOfSong(1);
                                            }
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    if (mp.isPlaying()) {
                        if (mp != null) {
                            mp.pause();
                            // Changing button image to play button
                            play.setImageResource(android.R.drawable.ic_media_play);
                        }
                    }
                    else if(mpNext.isPlaying()){
                        if (mpNext != null) {
                            mpNext.pause();
                            // Changing button image to play button
                            play.setImageResource(android.R.drawable.ic_media_play);
                        }
                    }
                    else {
                        // Resume song
                        if(playerNow==0) {
                            if (mp != null && !initialized) {
                                mp.start();
                                // Changing button image to pause button
                                play.setImageResource(android.R.drawable.ic_media_pause);
                            }
                        }
                        else{
                            if (mpNext != null && !initialized) {
                                mpNext.start();
                                // Changing button image to pause button
                                play.setImageResource(android.R.drawable.ic_media_pause);
                            }

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

                if(playerNow==0 ) {
                    if(mp!=null) {
                        if ((sTime + fTime) <= eTime) {
                            sTime = sTime + fTime;
                            mp.seekTo(sTime);
                        } else {
                            Toast.makeText(getApplicationContext(), "Cannot jump forward 3 seconds", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                else if(playerNow==1 ) {
                    if(mpNext!=null) {
                        if ((sTime + fTime) <= eTime) {
                            sTime = sTime + fTime;
                            mpNext.seekTo(sTime);
                        } else {
                            Toast.makeText(getApplicationContext(), "Cannot jump forward 3 seconds", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });

        backward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int back = sTime - bTime;
                if(playerNow==0 ) {
                    if(mp!=null) {
                        if (back > 0) {
                            sTime = sTime - bTime;
                            mp.seekTo(sTime);
                        } else {
                            Log.e("check","back pos");
                            Log.e("check","back "+(allTracks.size()-3+pos));
                            if(allTracks.size()-3+pos>=0) {
                                pos--;
                                Log.e("check","pos value: "+pos);
                                int size = allTracks.size()-2+pos;
                                playMp3(allTracks.get(size).getMusicFile().getMusicFileExtract(), 1);
                                playerNow = 1;
                                mp.stop();
                                updateTimer();
                                int x = allTracksDuration.get(allTracksDuration.size()+ pos) + back;
                                Log.e("check","seek to: "+x);
                                mpNext.seekTo(x);
                                mpNext.start();
                                //pos++;
                                size = allTracks.size()-1+pos;
                                playMp3(allTracks.get(size).getMusicFile().getMusicFileExtract(),0);
                                mpNext.setNextMediaPlayer(mp);
                            }
                            else {
                                Toast.makeText(getApplicationContext(), "Cannot jump backward 3 seconds", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
                else if(playerNow==1 ) {
                    if(mpNext!=null) {
                        if (back > 0) {
                            sTime = sTime - bTime;
                            mpNext.seekTo(sTime);
                        } else {
                            Log.e("check","back pos next");
                            Log.e("check","back next "+(allTracks.size()-3+pos));
                            if(allTracks.size()-3+pos>=0) {
                                pos--;
                                Log.e("check","pos value: "+pos);
                                int size = allTracks.size()-2+pos;
                                playMp3(allTracks.get(size).getMusicFile().getMusicFileExtract(), 0);
                                playerNow = 0;
                                mpNext.stop();
                                updateTimer();
                                int x = allTracksDuration.get(allTracksDuration.size()+ pos) + back;
                                Log.e("check","seek to: "+x);
                                mp.seekTo(x);
                                mp.start();
                                //pos++;
                                size = allTracks.size()-1+pos;
                                playMp3(allTracks.get(size).getMusicFile().getMusicFileExtract(),1);
                                mp.setNextMediaPlayer(mpNext);
                            }
                            else{
                                Toast.makeText(getApplicationContext(), "Cannot jump backward 3 seconds", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        });
    }

    private void updateTimer(){
        if(pos<0){
            int sum =0;
            for(int i=0; i<allTracksDuration.size()+pos;i++){
                sum+=allTracksDuration.get(i);
            }
            sTime2 = sum;
        }
    }

    //TODO
    private void setTimer(long overallDuration){
        eTime = (int) overallDuration;
        Log.e("check eTime", String.valueOf(eTime));
        if(playerNow==0){
            sTime = mp.getCurrentPosition();
        }
        else{
            sTime = mpNext.getCurrentPosition();
        }
        if(oTime == 0){
            songPrgs.setMax(eTime);
            oTime =1;
        }
        remainedTime.setText(String.format("%d:%02d ", TimeUnit.MILLISECONDS.toMinutes(eTime),
                TimeUnit.MILLISECONDS.toSeconds(eTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS. toMinutes(eTime))) );
        elapsedTime.setText(String.format("%d:%02d ", TimeUnit.MILLISECONDS.toMinutes(sTime),
                TimeUnit.MILLISECONDS.toSeconds(sTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS. toMinutes(sTime))) );
        songPrgs.setProgress(sTime);
        hdlr.postDelayed(UpdateSongTime, 100);
    }

    private Runnable UpdateSongTime = new Runnable() {
        @Override
        public void run() {
            int sTime3 = 0;
            if(playerNow==0) {
                sTime = mp.getCurrentPosition();
                sTime3 = sTime2 + mp.getCurrentPosition();
            }
            else{
                sTime = mpNext.getCurrentPosition();
                sTime3 = sTime2 + mpNext.getCurrentPosition();
            }
            if(pass==0) {
                elapsedTime.setText(String.format("%d:%02d ", TimeUnit.MILLISECONDS.toMinutes(sTime),
                        TimeUnit.MILLISECONDS.toSeconds(sTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(sTime))) );
                songPrgs.setProgress(sTime);
            }
            else{
                elapsedTime.setText(String.format("%d:%02d ", TimeUnit.MILLISECONDS.toMinutes(sTime3),
                        TimeUnit.MILLISECONDS.toSeconds(sTime3) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(sTime3))) );
                songPrgs.setProgress(sTime3);
            }
            hdlr.postDelayed(this, 100);
        }
    };

    private void endOfSong(int flag){
        hdlr.removeCallbacks(UpdateSongTime);
        if(flag==0) {
            Log.e("check","end");
            mp.release();
            mp=null;
            mpNext=null;
        }
        else {
            Log.e("check","end next");
            mpNext.release();
            mp = null;
            mpNext = null;
        }
    }

    private void setCover(Value v){
        MediaMetadataRetriever media = new MediaMetadataRetriever();
        media.setDataSource(new StreamMediaDataSource(v.getMusicFile().getMusicFileExtract()));
        byte [] embeddedPicture = media.getEmbeddedPicture();
        if(embeddedPicture!=null) {
            InputStream is = new ByteArrayInputStream(embeddedPicture);
            Bitmap b = BitmapFactory.decodeStream(is);
            cover.setImageBitmap(Bitmap.createScaledBitmap(b, 56, 65, false));

        }
        else
        {
            cover.setImageResource(R.drawable.music); //any default cover resourse folder
        }
        cover.setAdjustViewBounds(true);
        cover.setVisibility(View.VISIBLE);
    }

    private void playNextChunk(Value value,int flag) {
        if(flag==0) {
            try {
                mp.stop();
                mp.reset();
                mp.setDataSource(new StreamMediaDataSource(value.getMusicFile().getMusicFileExtract()));
                mp.prepare();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            try {
                mpNext.stop();
                mpNext.reset();
                mpNext.setDataSource(new StreamMediaDataSource(value.getMusicFile().getMusicFileExtract()));
                mpNext.prepare();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void playMp3(byte[] chunk,int flag) {
        if(flag==0) {
            try {
                mp.reset();
                mp.setDataSource(new StreamMediaDataSource(chunk));
                mp.prepare();
                //mp.start();
            } catch (IOException ex) {
                String s = ex.toString();
                ex.printStackTrace();
            }
        }
        else{
            try {
                mpNext.reset();
                mpNext.setDataSource(new StreamMediaDataSource(chunk));
                mpNext.prepare();
                //mpNext.start();
            } catch (IOException ex) {
                String s = ex.toString();
                ex.printStackTrace();
            }
        }
    }

    private void playTempMp3(byte[] mp3SoundByteArray) {
        try {
            // create temp file that will hold byte array
            File tempMp3 = File.createTempFile("kurchina", "mp3", getCacheDir());
            tempMp3.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempMp3);
            fos.write(mp3SoundByteArray);
            fos.close();

            // resetting mediaplayer instance to evade problems
            mp.reset();

            // In case you run into issues with threading consider new instance like:
            // MediaPlayer mediaPlayer = new MediaPlayer();

            // Tried passing path directly, but kept getting
            // "Prepare failed.: status=0x1"
            // so using file descriptor instead
            FileInputStream fis = new FileInputStream(tempMp3);
            mp.setDataSource(fis.getFD());
            mp.prepare();
            mp.start();
        } catch (IOException ex) {
            String s = ex.toString();
            ex.printStackTrace();
        }
    }

    private void stopPlaying(){
        // If media player is not null then try to stop it
        int fin = 0;
        if(mp!=null){
            hdlr.removeCallbacks(UpdateSongTime);
            mp.stop();
            mp.release();
            mp = null;
            fin=1;
        }
        if(mpNext!=null){
            mpNext.stop();
            mpNext.release();
            mpNext = null;
            fin=2;
        }
        if(fin>0) {
            sTime = 0;
            Toast.makeText(MainActivity.this, "Stop player...", Toast.LENGTH_SHORT).show();
            play.setImageResource(android.R.drawable.ic_media_play);
        }
        initialized = true;
    }

    private void disablePlayerUI(){
        cover.setVisibility(View.INVISIBLE);
        play.setClickable(false);
        play.setVisibility(View.INVISIBLE);
        stop.setClickable(false);
        stop.setVisibility(View.INVISIBLE);
        forward.setClickable(false);
        forward.setVisibility(View.INVISIBLE);
        backward.setClickable(false);
        backward.setVisibility(View.INVISIBLE);
        songPrgs.setVisibility(View.INVISIBLE);
        titleOfSong.setVisibility(View.INVISIBLE);
        elapsedTime.setVisibility(View.INVISIBLE);
        remainedTime.setVisibility(View.INVISIBLE);
    }

    private void enablePlayerUI(){
        titleOfSong.setVisibility(View.VISIBLE);
        play.setVisibility(View.VISIBLE);
        play.setClickable(true);
        elapsedTime.setVisibility(View.VISIBLE);
        String t = "00:00";
        elapsedTime.setText(t);
        remainedTime.setVisibility(View.VISIBLE);
        remainedTime.setText(t);
        songPrgs.setProgress(0);
        songPrgs.setVisibility(View.VISIBLE);
        stop.setVisibility(View.VISIBLE);
        stop.setClickable(true);
        forward.setVisibility(View.VISIBLE);
        forward.setClickable(true);
        backward.setVisibility(View.VISIBLE);
        backward.setClickable(true);
    }

    @Override
    protected void onStop(){
        isExit = true;
        Log.e("check","onDestroy");
        try {
            inputQueue.clear();
            inputQueue.put("exit");
            stage="exit";
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        runner.cancel(true);
        super.onStop();
    }

    //TODO ALSO MUST INFORM BROKER THAT I LEAVE
    /*
    @Override
    protected void onDestroy(){
        isExit = true;
        runner.cancel(true);
        Log.e("check","onDestroy");
        try {
            inputQueue.put("exit");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }*/

    @Override
    public void onBackPressed() {
        isExit = true;
        Log.e("check","onBackPressed");
        try {
            inputQueue.clear();
            inputQueue.put("exit");
            stage="exit";
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        runner.cancel(true);
        Intent i = new Intent(getApplicationContext(), StartActivity.class);
        startActivityForResult(i, 0);
    }



    private class AsyncTaskRunner extends AsyncTask<List<BlockingQueue>, Object, String> {

        private boolean notified;
        private String[] connectedBroker;
        private int inState;
        private Socket requestSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String ip;
        private int port;


        @Override
        protected String doInBackground(List<BlockingQueue>... params) {
            String artist = null;
            Info info = null;
            String song = null;
            int flag=0;
            //int counter=0;
            notified = false;
            requestSocket = null;
            out = null;
            in = null;
            if(isNetworkAvailable()) {
                try {
                    this.setIp((String) params[0].get(0).take());
                    this.setPort(Integer.parseInt((String) params[0].get(0).take()));
                    requestSocket = new Socket(ip, port);
                    out = new ObjectOutputStream(requestSocket.getOutputStream());
                    in = new ObjectInputStream(requestSocket.getInputStream());
                    Log.e("check", "connected");
                    out.writeObject("Consumer");
                    out.flush();
                    while (true) {
                        out.writeObject(stage);
                        if (stage.equalsIgnoreCase("init")) {
                            inState = 0;
                            out.writeObject("Wake up");
                            out.flush();
                            Log.e("check", "send1");
                            info = (Info) in.readObject();
                            Log.e("check", "take info");
                            while (info == null) {
                                if (isCancelled() || isExit) {
                                    Log.e("check", "cancel 1");
                                    inState = -1;
                                    break;
                                }
                                out.writeObject(null);
                                out.flush();
                                info = (Info) in.readObject();
                            }
                            if (inState == -1) {
                                out.writeObject("exit");
                                out.flush();
                                break;
                            }
                            out.writeObject("ok");
                            out.flush();
                            for (ArtistName a : info.getListOfBrokersInfo().keySet()) {
                                Log.e("Artists in Info", a.getArtistName());
                            }
                            publishProgress(info, 0);
                /*
                synchronized(this){
                    while(!notified){
                        try{
                            this.wait();
                        }
                        catch(InterruptedException e){ }
                    }
                }
                hasChoose = false;
                */

                            artist = (String) params[0].get(0).take();
                            if (isCancelled() || isExit) {
                                Log.e("check", "cancel 0");
                                inState = 1;
                                out.writeObject("exit");
                                out.flush();
                                break;
                            }
                        } else if (stage.equalsIgnoreCase("artist")) {
                            ArtistName artistName = new ArtistName(artist);
                            Log.e("check", "continue");
                            String[] cb = findCorrespondingBroker(artistName, info);
                            List<String> broker = register(cb, artistName);
                            stage = "song";
                            Log.e("check", "continue2");
                            if (broker != null) {
                                if (!(broker.get(0).equalsIgnoreCase("this"))) {
                                    out.writeObject("You are not my comrade");
                                    out.flush();
                                    disconnect(requestSocket, in, out);
                                    requestSocket = new Socket(broker.get(1), Integer.parseInt(broker.get(2)));
                                    out = new ObjectOutputStream(requestSocket.getOutputStream());
                                    in = new ObjectInputStream(requestSocket.getInputStream());
                                    out.writeObject("Consumer");
                                    out.flush();
                                    System.out.println("Success to change and connected to " + broker.get(0));
                                    out.writeObject("Already up");
                                    out.flush();
                                }

                                out.writeObject("Register");
                                out.flush();
                                //get a specific identification for this client
                                out.writeObject("Consumer");
                                out.flush();
                                if (isCancelled() || isExit) {
                                    Log.e("check", "cancel 1");
                                    inState = 2;
                                    out.writeObject(null);
                                    out.flush();
                                    break;
                                }
                                out.writeObject(artistName);
                                out.flush();
                                List<String> allSongs = (ArrayList<String>) in.readObject();
                                if (allSongs == null) {
                                    Log.e("check", "here");
                                    stage = "init";
                                    continue;
                                }
                                if (flag != 2) {
                                    publishProgress(allSongs, 1);
                                }
                            /*
                    synchronized(this){
                        while(!notified){
                            try{
                                this.wait();
                            }
                            catch(InterruptedException e){ }
                        }
                     }
                    hasChoose = false;
                    */
                                song = (String) params[0].get(0).take();
                                if (isCancelled() || isExit) {
                                    Log.e("check", "cancel 2");
                                    inState = 3;
                                    out.writeObject("exit");
                                    out.flush();
                                    break;
                                }
                                if (stage.equalsIgnoreCase("artist")) {
                                    artist = song;
                                }

                            } else {
                                stage = "init";
                            }
                        } else if (stage.equalsIgnoreCase("song")) {
                            boolean fstop = false;
                            out.writeObject(song);
                            out.flush();
                            Value v = (Value) in.readObject();
                            stage = "chunks";
                            out.writeObject("ok");
                            out.flush();
                            if (v.getFailure()) {
                                String m = "Failure -> Possibly there is not song with this name or there is not this publisher in system";
                                Toast.makeText(MainActivity.this, m, Toast.LENGTH_SHORT).show();
                                mp = null;
                                mpNext = null;
                            }
                        /*TODO ISWS NA SKAEI PRIN STEILEI OLA TA CHUNKS AMA PX EXW EPILEKSEI ENA NEO SONG NA PARW
                            H ENA NEO ARTIST AYTO THA GINETAI ME TO NA STELNEI MYNHMA KATALHLO STON BROKER px STOP kai ekeinos tha kleinei
                            sundesh me publisher
                         */
                            else {
                                params[0].get(1).put(v);
                                while (true) {
                                    v = (Value) in.readObject();
                                    System.out.println(v);
                                    //System.out.println(v.getFailure());
                                    if (v == null) {
                                        v = new Value();
                                        v.setFailure(true);
                                        params[0].get(1).put(v);
                                        //params[0].get(1).put(v);
                                        out.writeObject("ok");
                                        break;
                                    }

                                    if (!stage.equalsIgnoreCase("chunks")) {
                                        out.writeObject("stop");
                                        fstop = true;
                                        break;
                                    }
                                    out.writeObject("ok");
                                    params[0].get(1).put(v);
                                }
                            }
                            if (!fstop) {
                                while (true) {
                                    //Log.e("check","in loop");
                                    if (isCancelled() || isExit) {
                                        Log.e("check", "cancel 3");
                                        inState = 4;
                                        break;
                                    }
                                    if (mp == null && mpNext == null) {
                                        Log.e("async", "end");
                                        break;
                                    }
                                    if (!stage.equalsIgnoreCase("chunks")) {
                                        break;
                                    }
                                }
                            }
                            if (isCancelled() || isExit) {
                                Log.e("check", "cancel 3.1");
                                inState = 4;
                            }

                            if (inState == 4) {
                                out.writeObject("exit");
                                out.flush();
                                break;
                            }
                            Log.e("check", stage);
                            if (stage.equalsIgnoreCase("chunks")) {
                                stage = "init";
                                flag = 0;
                                publishProgress(null, 3);
                                Thread.sleep(3000);
                            } else if (stage.equalsIgnoreCase("song")) {
                                stage = "artist";
                                flag = 2;
                                //song = (String) params[0].get(0).poll();
                            } else if (stage.equalsIgnoreCase("artist")) {
                                flag = 0;
                                artist = (String) params[0].get(0).poll();
                            }
                            out.writeObject("keep");
                            out.flush();
                            Log.e("check", stage);
                        }
                    }
                } catch (UnknownHostException unknownHost) {
                    System.err.println("You are trying to connect to an unknown host!");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    Log.e("check", "finally async");
                    try {
                        if (in != null)
                            in.close();
                        if (out != null)
                            out.close();
                        if (requestSocket != null)
                            requestSocket.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
            else{
                publishProgress(null,-1);
            }
            return null;
        }


        /*
        @Override
        protected void onCancelled(){
            Log.e("check","onCancelled async");
            super.onCancelled();
            cancel(true);
        }*/

        @Override
        protected void onPostExecute(String result) {

        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onProgressUpdate(Object... text) {
            //synchronized(this) {

            int flag = (int) text[1];
            if (flag == 0) {
                viewSongs.setVisibility(View.INVISIBLE);
                btn2.setClickable(false);
                btn2.setVisibility(View.INVISIBLE);
                disablePlayerUI();
                Info info = (Info) text[0];
                List<String> artists = new ArrayList<>();
                for (ArtistName a : info.getListOfBrokersInfo().keySet()) {
                    artists.add(a.getArtistName());
                }
                Log.e("check","onProgressUpdate0");
                ArrayAdapter adapter = new ArrayAdapter<String>(MainActivity.this, R.layout.activity_listview, artists);
                viewArtists.setAdapter(adapter);
                viewArtists.setVisibility(View.VISIBLE);
                btn1.setClickable(true);
                btn1.setVisibility(View.VISIBLE);
            }
            if (flag == 1) {
                Log.e("check","onProgressUpdate1");
                List<String> songs = (ArrayList<String>) text[0];
                ArrayAdapter adapter = new ArrayAdapter<String>(MainActivity.this, R.layout.activity_listview, songs);
                viewSongs.setAdapter(adapter);
                viewSongs.setVisibility(View.VISIBLE);
                btn2.setClickable(true);
                btn2.setVisibility(View.VISIBLE);
                disablePlayerUI();
            }
            if(flag==2){
                disablePlayerUI();
            }
            if(flag==-1){
                Toast.makeText(MainActivity.this,"Not internet connection", Toast.LENGTH_LONG).show();
            }
                /*while(true){
                    if(hasChoose)
                        break;
                }
                notified = true;
                this.notify();
            }*/
        }

        private void disconnect(Socket requestSocket, ObjectInputStream in, ObjectOutputStream out){
            System.out.println("Closing this connection : " + requestSocket);
            if(requestSocket!=null) {
                try {
                    requestSocket.close();
                    System.out.println("Connection closed");
                } catch (Exception e) {
                    System.err.println("Failed to disconnect");
                    e.printStackTrace();
                }
            }
            try {
                if(out!=null)
                    out.close();
                if(in!=null)
                    in.close();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        private String[] findCorrespondingBroker(ArtistName artistName,Info info) {
            //boolean isOK = false;
            String[] cb = null;
            for (ArtistName a : info.getListOfBrokersInfo().keySet()) {
                if (a.getArtistName().equalsIgnoreCase(artistName.getArtistName())) {
                    //isOK = true;
                    cb = info.getListOfBrokersInfo().get(a).clone();
                    break;
                }
            }
            return cb;
        }

        //method to register with the capable broker
        private List<String> register(String[] broker, ArtistName artistName){
            List<String> correctBroker = new ArrayList<>();
            System.out.println(broker);
            System.out.println(artistName);
            if(broker!=null && artistName!=null){
                if((broker[1].equals(this.getIp())) && (Integer.parseInt(broker[2]) == this.getPort())){
                    setConnectedBroker(broker);
                    correctBroker.add("this");
                    return correctBroker;
                }
                else{
                    setConnectedBroker(null);
                    correctBroker.add(broker[0]);
                    this.setPort(Integer.parseInt(broker[2]));
                    this.setIp(broker[1]);
                    correctBroker.add(broker[1]);
                    correctBroker.add(broker[2]);
                    return correctBroker;
                }
            }
            else {
                System.out.println("Null broker or artist name");
                return null;
            }

        }

        private void setConnectedBroker(String[] broker) {
            this.connectedBroker=broker;
        }

        private String[] getConnectedBroker(){
            return this.connectedBroker;
        }



        public int getPort() {
            return this.port;
        }

        public String getIp() {
            return this.ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public void setPort(int p) {
            this.port = p;
        }


        private boolean isNetworkAvailable() {
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null;
        }

    }


    /*
    private class AsyncTaskMediaPlayer extends AsyncTask<BlockingQueue, Object, String> {


        @Override
        protected String doInBackground(final BlockingQueue... params) {
            try {
                Value value = (Value) params[0].take();
                System.out.println(value);
                playMp3(value.getMusicFile().getMusicFileExtract(),0);
                //mp.start();
                //setTimer();
                play.setImageResource(android.R.drawable.ic_media_pause);
                value = (Value) params[0].take();
                playMp3(value.getMusicFile().getMusicFileExtract(),1);
                mp.setNextMediaPlayer(mpNext);
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        if(mp!=null){
                            Log.e("check","1");
                            playerNow = 1;
                            try {
                                Value value = (Value) params[0].take();
                                //!value.getFailure()
                                if(!value.getFailure()) {
                                    Log.e("check","2");
                                    playNextChunk(value,0);
                                    mpNext.setNextMediaPlayer(mp);
                                }
                                else{
                                    Log.e("check","3");
                                    play.setImageResource(android.R.drawable.ic_media_play);
                                    endOfSong(0);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                });
                mpNext.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        if(mpNext!=null){
                            Log.e("check","1 next");
                            playerNow = 0;
                            try {
                                Value value = (Value) params[0].take();
                                //!value.getFailure()
                                if(!value.getFailure()) {
                                    Log.e("check","2 next");
                                    playNextChunk(value,1);
                                    mp.setNextMediaPlayer(mpNext);
                                }
                                else{
                                    Log.e("check","3 next");
                                    play.setImageResource(android.R.drawable.ic_media_play);
                                    endOfSong(1);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }
        private void playNextChunk(Value value,int flag) {
            if(flag==0) {
                try {
                    mp.stop();
                    mp.reset();
                    mp.setDataSource(new StreamMediaDataSource(value.getMusicFile().getMusicFileExtract()));
                    mp.prepare();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else{
                try {
                    mpNext.stop();
                    mpNext.reset();
                    mpNext.setDataSource(new StreamMediaDataSource(value.getMusicFile().getMusicFileExtract()));
                    mpNext.prepare();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void playMp3(byte[] chunk,int flag) {
            if(flag==0) {
                try {
                    mp.reset();
                    mp.setDataSource(new StreamMediaDataSource(chunk));
                    mp.prepareAsync();
                    mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            mp.start();
                        }
                    });
                    //mp.start();
                } catch (Exception ex) {
                    String s = ex.toString();
                    ex.printStackTrace();
                }
            }
            else{
                try {
                    mpNext.reset();
                    mpNext.setDataSource(new StreamMediaDataSource(chunk));
                    mpNext.prepare();
                    //mpNext.start();
                } catch (IOException ex) {
                    String s = ex.toString();
                    ex.printStackTrace();
                }
            }
        }
    }*/
}
