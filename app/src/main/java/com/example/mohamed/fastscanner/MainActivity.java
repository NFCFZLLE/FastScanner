package com.example.mohamed.fastscanner;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_CODE_WRITE_EXTERNAL_STORAGE = 100;
    TextView text;
    String id;
    Button save;
    FileWriter writer;
    File gpxfile;
    private NfcAdapter mNfcAdapter;
    private final String[][] techList = new String[][]{
            new String[]{
                    NfcA.class.getName(),
                    NfcB.class.getName(),
                    NfcF.class.getName(),
                    NfcV.class.getName(),
                    IsoDep.class.getName(),
                    MifareClassic.class.getName(),
                    MifareUltralight.class.getName(), Ndef.class.getName()
            }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
//        themeColor = R.color.intro1_bg;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_CODE_WRITE_EXTERNAL_STORAGE);
            } else {
                Toast.makeText(this, "Please enable storage permission", Toast.LENGTH_LONG).show();
                openSettings();
            }
        }
        text = (TextView) findViewById(R.id.text);
        save = (Button) findViewById(R.id.save);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (!mNfcAdapter.isEnabled()) {
            text.setText("NFC is disabled.");
        } else {
            text.setText("Scan NFC Card");
        }
        gpxfile = new File(getFilesDir(), "UIDs.csv");

        try {

            writer = new FileWriter(gpxfile);

            writeCsvHeader("First Param", "Second Param", "Third Param");
//            writeCsvData(0.31f,5.2f,7.0f);
//            writeCsvData(0.31f,5.2f,7.1f);
//            writeCsvData(0.31f,5.2f,7.2f);
//            Toast.makeText(this, "Created File", Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "CAN'T create CSV file", Toast.LENGTH_LONG).show();

        }

        if (!save.isEnabled())
            save.setBackgroundColor(0x00C089);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                   try {
//                    writer.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                Toast.makeText(getApplicationContext(), "Saved successfully!", Toast.LENGTH_LONG).show();

                Intent intentShareFile = new Intent(Intent.ACTION_SEND);

                if (gpxfile.exists()) {
                    Uri uri = FileProvider.getUriForFile(getApplicationContext(), "com.example.mohamed.fastscanner", gpxfile);
                    intentShareFile.setType("text/comma-separated-values");
                    intentShareFile.putExtra(Intent.EXTRA_STREAM, uri);
                    intentShareFile.putExtra(Intent.EXTRA_SUBJECT, "Sharing File...");
                    intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing UIDs...");
//                    intentShareFile.setData(uri);
//                    intentShareFile.setData(uri);
//                    intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intentShareFile, "Share File"));
                }
            }
        });
//        handleIntent(getIntent());

    }

    //    Use this routine to write the csv headers:
    private void writeCsvHeader(String h1, String h2, String h3) throws IOException {
        String line = String.format("%s,%s,%s\n", h1, h2, h3);
        writer.write(line);
    }

    //    Use this routine to write CSV values to the file:
    private void writeCsvData(String d) throws IOException {
        String line = String.format("%s\n", d);
        writer.write(line);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // creating pending intent:
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        // creating intent receiver for NFC events:
        IntentFilter filter = new IntentFilter();
        filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
        // enabling foreground dispatch for getting intent from NFC event:
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[]{filter}, this.techList);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // disabling foreground dispatch:
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            id = ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
            text.setText(
                    "NFC Tag\n" + id);
            try {
                writeCsvData(id);
            } catch (IOException e) {
                e.printStackTrace();
            }
            save.setEnabled(true);
            save.setBackgroundColor(0xFF30BF6C);


        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS_CODE_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Thank you!", Toast.LENGTH_LONG).show();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_CODE_WRITE_EXTERNAL_STORAGE);
                    } else {
                        Toast.makeText(this, "Please enable storage permission", Toast.LENGTH_LONG).show();
                        openSettings();
                    }
                }
            }

        }
    }

    private String ByteArrayToHexString(byte[] inarray) {
        int i, j, in;
        String[] hex = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
        String out = "";

        for (j = 0; j < inarray.length; ++j) {
            in = (int) inarray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }
        return out;
    }

    private void openSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("com.example.mohamed.fastscanner", this.getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
}
