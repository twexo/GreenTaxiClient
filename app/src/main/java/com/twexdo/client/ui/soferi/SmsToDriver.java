package com.twexdo.client.ui.soferi;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.twexdo.client.R;
import com.twexdo.client.sms;

public class SmsToDriver extends AppCompatActivity {
    EditText adresa,reper;
    TextView intampinare;
    String s_adresa,s_reper,nrTelSoferCerut,numeSoferCerut;
    Button b_getLocation,b_sendComanda;
    DatabaseReference databaseReference;
    boolean allpermissionGaranted=false;
    String myPhoneNr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_to_driver);

        adresa=findViewById(R.id.sms_adresa);
        reper=findViewById(R.id.sms_reper);
        b_sendComanda=findViewById(R.id.sms_send);
        intampinare=findViewById(R.id.intampinare);
        getExtra();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!(checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) ||
                !(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) ||
                !(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                requestPermissions(new String[]{
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, 10);
            } else {
                allpermissionGaranted = true;
                getMyPhoneNumber();
            }
        }

        b_sendComanda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              sendMessage(myPhoneNr,adresa.getText().toString(),reper.getText().toString());
              finish();
            }
        });


    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 10) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getMyPhoneNumber();
            }
        }
    }
    public void getExtra(){
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            nrTelSoferCerut=extras.getString("nr_tel_soferCerut");
            numeSoferCerut=extras.getString("nume_soferCerut");
            if(numeSoferCerut.length()>1){
            setTitle("Comanda catre "+numeSoferCerut);
            intampinare.setText("Unde doriti ca " + numeSoferCerut+" sa vina?");
            }
            else{
                Toast.makeText(this, "Nu am putut identifica soferul dorit de dvs.", Toast.LENGTH_SHORT).show();
                finish();
            }

        }
    }




    @SuppressLint("MissingPermission")
    private void getMyPhoneNumber(){
        TelephonyManager tmgr= (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        try{ myPhoneNr=tmgr.getLine1Number();}catch (Exception e){Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();}
        if(myPhoneNr.equals("")){
            myPhoneNr = tmgr.getSubscriberId();
        }

       // Toast.makeText(getApplicationContext(), "my phone is " +myPhoneNr, Toast.LENGTH_SHORT).show();
    }

    public void sendMessage(String _myPhoneNr,String addresa,String reper){
        try {
            if(_myPhoneNr.length() < 9) Toast.makeText(getApplicationContext(), "Nu am putut identifica nr. dvs. de telefon...", Toast.LENGTH_SHORT).show();
            else if(adresa.length()<5 )Toast.makeText(getApplicationContext(), "Avem nevoie de o adresa valida ...", Toast.LENGTH_SHORT).show();
            else {
                databaseReference = FirebaseDatabase.getInstance().getReference();
                String content = "Comanda ceruta pe " + addresa + " . \n Repere : " + reper + " ";
                //aici voi edita x si y cu  cand voi pune mapa
                databaseReference.child("mesaj").push().setValue(new sms(nrTelSoferCerut, _myPhoneNr, content, 1, 1)) .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getApplicationContext(), "Comanda a fost trimisa cu succes!", Toast.LENGTH_SHORT).show();
                    }
                })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(), "Comanda esuata!", Toast.LENGTH_SHORT).show();
                            }
                        });
            }

        }catch (Exception error){
            Toast.makeText(getApplicationContext(), "Am intampinat o problema...", Toast.LENGTH_SHORT).show();
            Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
        }
    }


}
