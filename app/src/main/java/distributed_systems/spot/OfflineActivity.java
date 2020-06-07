package distributed_systems.spot;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import distributed_systems.spot.Code.ArtistName;
import distributed_systems.spot.Code.Info;
import distributed_systems.spot.Code.MusicFile;
import distributed_systems.spot.Code.Value;

public class OfflineActivity extends AppCompatActivity {
    private Button btn1;
    private ImageButton btn2;
    private Button btn3;
    private ListView viewArtists;
    private ListView viewSongs;
    private String selectedArtist;
    private String selectedSong;
    private OfflineActivity.AsyncTaskRunner runner;
    private BlockingQueue<String> inputQueue;
    private boolean isExit;
    private String stage;
    private List<Value> allTracks;
    private String track;
    private SearchView searchArtist;
    private SearchView searchSong;
    //private ArrayAdapter adapterArtist;
    //private ArrayAdapter adapterSong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline);
        btn1 = (Button) findViewById(R.id.button3);
        btn2 = (ImageButton) findViewById(R.id.imageButton5);
        btn3 = (Button) findViewById(R.id.button4);
        viewArtists = (ListView) findViewById(R.id.listViewOffline1);
        viewSongs = (ListView) findViewById(R.id.listViewOffline2);
        searchArtist = (SearchView) findViewById(R.id.searchArtist);
        searchSong = (SearchView) findViewById(R.id.searchSong);

    }
    //TODO SET VISIBLE AND CLICKABLE PLAYER BUTTONS AND UI AFTER USER CHOOSE A SONG
    public void onStart() {
        super.onStart();
        String ip = (String) getIntent().getStringExtra("ip");
        int port = (int) getIntent().getIntExtra("port",5000);
        allTracks = new ArrayList<>();
        isExit = false;
        viewArtists.setVisibility(View.INVISIBLE);
        viewSongs.setVisibility(View.INVISIBLE);
        btn1.setVisibility(View.INVISIBLE);
        btn2.setVisibility(View.INVISIBLE);
        btn3.setVisibility(View.INVISIBLE);
        btn1.setClickable(false);
        btn2.setClickable(false);
        btn3.setClickable(false);
        searchArtist.setVisibility(View.INVISIBLE);
        searchSong.setVisibility(View.INVISIBLE);
        stage = "init";
        //final BlockingQueue<String> inputQueue = new LinkedBlockingDeque<>();
        inputQueue = new LinkedBlockingDeque<>();
        inputQueue.add(ip);
        inputQueue.add(Integer.toString(port));
        runner = new AsyncTaskRunner();
        runner.execute(inputQueue);
        //state = false;
        selectedArtist = null;
        selectedSong = null;
        viewArtists.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long arg3) {
                view.setSelected(true);
                view.setActivated(true);
                //viewArtists.setItemChecked(position, true);
                selectedArtist = (String) (viewArtists.getItemAtPosition(position));

            }
        });

        searchArtist.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                ArrayAdapter adapterArtist = (ArrayAdapter) viewArtists.getAdapter();
                adapterArtist.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                ArrayAdapter adapterArtist = (ArrayAdapter) viewArtists.getAdapter();
                if (TextUtils.isEmpty(newText)) {
                    adapterArtist.getFilter().filter(null);
                } else {
                    adapterArtist.getFilter().filter(newText);
                }
                return true;
                //return false;
            }
        });




        //TODO CHECK THAT USER HAD SELECTED BEFORE PUSH BUTTON
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //hasChoose = true;
                inputQueue.clear();
                allTracks.clear();
                if (selectedArtist != null) {
                    try {
                        inputQueue.put(selectedArtist);
                        stage = "artist";
                        selectedArtist = null;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(OfflineActivity.this, "Not selected artist", Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewSongs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long arg3) {
                view.setSelected(true);
                view.setActivated(true);
                selectedSong = (String) (viewSongs.getItemAtPosition(position));

            }
        });

        searchSong.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            ArrayAdapter adapterSong = (ArrayAdapter) viewSongs.getAdapter();
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapterSong.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    adapterSong.getFilter().filter(null);
                } else {
                    adapterSong.getFilter().filter(newText);
                }
                return true;
                //return false;
            }
        });


        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedSong != null) {
                    allTracks.clear();
                    inputQueue.clear();
                    //state=false;
                    try {
                        inputQueue.put(selectedSong);
                        stage = "song";
                        selectedSong = null;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(OfflineActivity.this, "Not selected song", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!allTracks.isEmpty()) {
                    Intent i = new Intent(getApplicationContext(), MediaPlayerOfflineActivity.class);
                    i.putExtra("Music file name", track);
                    startActivityForResult(i, 0);
                }
                else{
                    Toast.makeText(OfflineActivity.this, "No chunks...", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onStop(){
        isExit = true;
        Log.e("check","onDestroy");
        try {
            inputQueue.put("exit");
            stage="exit";
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        runner.cancel(true);
        super.onStop();
    }

    public String mergeChunks(List<Value> listOfValues){
        Value mergedValue = null;
        String name = null;
        byte[] reader = null;
        int count = 0;
        for(Value v : listOfValues) {
            if(count==0){
                reader = v.getMusicFile().getMusicFileExtract();
            }
            else{
                int valueSize = v.getMusicFile().getMusicFileExtract().length;
                int combined = reader.length+ valueSize;
                byte[] temp = new byte[combined];
                for (int i = 0; i < combined; ++i) {
                    temp[i] = i < reader.length ? reader[i] : v.getMusicFile().getMusicFileExtract()[i - reader.length];
                }
                reader = temp;
            }
            count++;
        }
        OutputStream outstream = null;
        MusicFile m = listOfValues.get(0).getMusicFile();
        MusicFile mergedM = new MusicFile();
        mergedM.setMusicFileExtract(reader);
        mergedM.setAlbumInfo(m.getAlbumInfo());
        mergedM.setArtistName(m.getArtistName());
        mergedM.setGenre(m.getGenre());
        mergedM.setTrackName(m.getTrackName());
        mergedValue = new Value();
        mergedValue.setMusicFile(mergedM);
        String fileName = m.getArtistName() + "-" + m.getTrackName() + ".mp3";
        //String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/saved songs/";
        //String path = getApplicationContext().getFilesDir().getAbsolutePath()+"/saved songs/";
        //File file = new File(path);
        //Log.e("path",path);
        //if(!file.exists()){
        //    boolean jet = file.mkdir();
        //}
        try {
            String filepath = "/saved songs/";
            File path = getExternalFilesDir(filepath);
            Log.e("path",path.getAbsolutePath());
            File of = new File(getExternalFilesDir(filepath), fileName);
            //File of = new File(file, fileName);
            outstream = new FileOutputStream(of);
            outstream.write(reader);
            name = fileName;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(outstream!=null)
                    outstream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return name;
    }

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



    private class AsyncTaskRunner extends AsyncTask<BlockingQueue, Object, String> {

        private String ip ;
        private int port ;
        private boolean notified;
        private String[] connectedBroker;
        private int inState;
        private Socket requestSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private ProgressDialog progressDialog;
        private ProgressDialog progressDialog2;


        @Override
        protected String doInBackground(BlockingQueue... params) {
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
                    this.setIp((String) params[0].take());
                    this.setPort(Integer.parseInt((String) params[0].take()));
                    requestSocket = new Socket();
                    requestSocket.connect(new InetSocketAddress(ip,port),60000);
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
                            while (info.getListOfBrokersInfo().keySet().isEmpty()) {
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

                            artist = (String) params[0].take();
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
                                song = (String) params[0].take();
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
                                Toast.makeText(OfflineActivity.this, m, Toast.LENGTH_SHORT).show();
                            }
                        /*TODO ISWS NA SKAEI PRIN STEILEI OLA TA CHUNKS AMA PX EXW EPILEKSEI ENA NEO SONG NA PARW
                            H ENA NEO ARTIST AYTO THA GINETAI ME TO NA STELNEI MYNHMA KATALHLO STON BROKER px STOP kai ekeinos tha kleinei
                            sundesh me publisher
                         */
                            else {
                                publishProgress(null, 3);
                                allTracks.add(v);
                                while (true) {
                                    v = (Value) in.readObject();
                                    System.out.println(v);
                                    //System.out.println(v.getFailure());
                                    if (v == null) {
                                        out.writeObject("ok");
                                        break;
                                    }

                                    if (!stage.equalsIgnoreCase("chunks")) {
                                        out.writeObject("stop");
                                        fstop = true;
                                        break;
                                    }
                                    out.writeObject("ok");
                                    allTracks.add(v);
                                }
                            }
                            publishProgress(null, 4);
                            if (!fstop) {
                                if (isCancelled() || isExit) {
                                    Log.e("check", "cancel 3");
                                    inState = 4;
                                    out.writeObject("exit");
                                    out.flush();
                                    break;
                                }
                                out.writeObject("exit");
                                out.flush();
                                break;
                            } else {
                                Log.e("check", stage);
                                Thread.sleep(3000);
                                if (isCancelled() || isExit) {
                                    Log.e("check", "cancel 3.1");
                                    inState = 4;
                                    out.writeObject("exit");
                                    out.flush();
                                    break;
                                }
                                if (stage.equalsIgnoreCase("chunks")) {
                                    stage = "init";
                                    flag = 0;
                                } else if (stage.equalsIgnoreCase("song")) {
                                    stage = "artist";
                                    flag = 2;
                                    //song = (String) params[0].get(0).poll();
                                } else if (stage.equalsIgnoreCase("artist")) {
                                    flag = 0;
                                    artist = (String) params[0].poll();
                                }
                                out.writeObject("keep");
                                out.flush();
                                Log.e("check", stage);
                            }
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
                return null;
            }
            if(!allTracks.isEmpty()){
                publishProgress(null,5);
                track = mergeChunks(allTracks);
                publishProgress(null,6);
            }
            return null;
        }



        @Override
        protected void onPostExecute(String result) {
            btn3.setVisibility(View.VISIBLE);
            btn3.setClickable(true);
        }


        @Override
        protected void onProgressUpdate(Object... text) {
            //synchronized(this) {

            int flag = (int) text[1];
            if (flag == 0) {
                searchSong.setVisibility(View.INVISIBLE);
                viewSongs.setVisibility(View.INVISIBLE);
                btn2.setClickable(false);
                btn2.setVisibility(View.INVISIBLE);
                Info info = (Info) text[0];
                List<String> artists = new ArrayList<>();
                for (ArtistName a : info.getListOfBrokersInfo().keySet()) {
                    artists.add(a.getArtistName());
                }
                Log.e("check","onProgressUpdate0");
                ArrayAdapter adapterArtist = new ArrayAdapter<String>(OfflineActivity.this, R.layout.activity_listview, artists);
                viewArtists.setAdapter(adapterArtist);
                viewArtists.setVisibility(View.VISIBLE);
                searchArtist.setVisibility(View.VISIBLE);
                btn1.setClickable(true);
                btn1.setVisibility(View.VISIBLE);
            }
            if (flag == 1) {
                Log.e("check","onProgressUpdate1");
                List<String> songs = (ArrayList<String>) text[0];
                ArrayAdapter adapterSong = new ArrayAdapter<String>(OfflineActivity.this, R.layout.activity_listview, songs);
                viewSongs.setAdapter(adapterSong);
                viewSongs.setVisibility(View.VISIBLE);
                searchSong.setVisibility(View.VISIBLE);
                btn2.setClickable(true);
                btn2.setVisibility(View.VISIBLE);
            }
            if(flag == 3){
                progressDialog = ProgressDialog.show(OfflineActivity.this,
                        "Progress Dialog",
                        "Receiving chunks...");
            }
            else if(flag==4){
                if(progressDialog!=null) {
                    progressDialog.dismiss();
                }
            }
            else if(flag==5){
                progressDialog = ProgressDialog.show(OfflineActivity.this,
                        "Progress Dialog",
                        "Merging chunks...");
            }
            else if(flag==6){
                btn1.setClickable(false);
                btn1.setVisibility(View.INVISIBLE);
                btn2.setClickable(false);
                btn2.setVisibility(View.INVISIBLE);
                if(progressDialog!=null) {
                    progressDialog.dismiss();
                }
            }
            if(flag==-1){
                Toast.makeText(OfflineActivity.this,"Not internet connection", Toast.LENGTH_LONG).show();
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

}

