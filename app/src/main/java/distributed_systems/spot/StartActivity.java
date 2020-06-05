package distributed_systems.spot;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class StartActivity extends AppCompatActivity {

    private Button offline;
    private Button online;
    private ImageButton settings;
    private String ip;
    private String port;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        online = (Button) findViewById(R.id.button6);
        offline = (Button) findViewById(R.id.button5);
        settings = (ImageButton) findViewById(R.id.imageButton11);
        ip=null;
        port=null;
    }

    public void onStart() {
        super.onStart();
        online.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if((ip==null || port==null) || (ip.equals("") || port.equals("") )){
                    Toast.makeText(StartActivity.this,"You must first set ip and port pushing the settings button",Toast.LENGTH_LONG).show();
                }
                else {
                    Intent i = new Intent(getApplicationContext(), MainActivity.class);
                    i.putExtra("ip",ip);
                    i.putExtra("port",Integer.parseInt(port));
                    startActivityForResult(i, 0);
                }
            }
        });
        offline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if((ip==null || port==null) || (ip.equals("") || port.equals("") )){
                    Toast.makeText(StartActivity.this,"You must first set ip and port pushing the settings button",Toast.LENGTH_LONG).show();
                }
                else {
                    Intent i = new Intent(getApplicationContext(), OfflineActivity.class);
                    i.putExtra("ip", ip);
                    i.putExtra("port", Integer.parseInt(port));
                    startActivityForResult(i, 0);
                }
            }
        });
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogOpen();

            }
        });

    }

    private void dialogOpen() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(StartActivity.this);
        LayoutInflater inflater = StartActivity.this.getLayoutInflater();
        View layout = inflater.inflate(R.layout.layout_dialog,null);
        final EditText ipInput = (EditText) layout.findViewById(R.id.input_ip);
        final EditText portInput = (EditText) layout.findViewById(R.id.input_port);
        alert.setView(layout);
        alert.setTitle("Settings");
        alert.setMessage("Enter ip and port of broker");

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                ip = ipInput.getText().toString();
                port = portInput.getText().toString();
                if(ip.equals("") || port.equals("")){
                    Toast.makeText(StartActivity.this,"You don't have set ip and port",Toast.LENGTH_SHORT).show();
                }

            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });

        alert.show();
    }
}
