package com.twexdo.client;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Random;

public class MyService extends Service {
    public String myPhoneNumber = "";
    String msg, id,numeSofer;
    double x, y;
    int timp;
    String receiverid;
    PendingIntent pendingIntent;
    Notification notification;
    NotificationCompat.Builder b;
    public static final String CHANNEL_ID = "ForegroundServiceChannelTEST";
    int notificationId;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getMyPhoneNumber();
        retrivemsg();
       // Toast.makeText(this, "SERVICE CREATED", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    public void retrivemsg() {
        DatabaseReference mdb = FirebaseDatabase.getInstance().getReference("mesaj");
        //builder=new NotificationCompat.Builder(this,"onchildadded");

        mdb.addChildEventListener(new ChildEventListener() {

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                notificationId = new Random().nextInt();

                receiverid = dataSnapshot.child("to").getValue(String.class);
                try{

                    if (receiverid.equals(myPhoneNumber)) {
                        Toast.makeText(MyService.this, "Mesaj pentru mine!", Toast.LENGTH_SHORT).show();
                        id = dataSnapshot.child("from").getValue(String.class);
                        numeSofer = dataSnapshot.child("nume").getValue(String.class);
                        int type = dataSnapshot.child("type").getValue(Integer.class);
                        if (type == 1) {
                            int time = dataSnapshot.child("time").getValue(Integer.class);
                            if (time > 0) {
                                msg = "Soferul a confirmat ca va ajunge in " + time + " minute.";
                                Intent notificationIntent = new Intent(MyService.this, ClientConfirmare.class);
                                notificationIntent.putExtra("id", id);
                                notificationIntent.putExtra("numeSofer", numeSofer);
                                notificationIntent.putExtra("time", time);
                                notificationIntent.putExtra("smsid",dataSnapshot.getKey());
                                notificationIntent.putExtra("notificationId",notificationId);
                                pendingIntent = PendingIntent.getActivity(MyService.this,
                                        0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                            } else {

                                msg = "Soferul a refuzat comanda.";
                                Intent notificationIntent = new Intent(MyService.this, MainActivity.class);
                                notificationIntent.putExtra("id", id);
                                notificationIntent.putExtra("smsid",dataSnapshot.getKey());
                                notificationIntent.putExtra("notificationId",notificationId);
                                pendingIntent = PendingIntent.getActivity(MyService.this,
                                        0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                            }
                        }



                        createNotificationChannel();
                        b = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                                .setContentTitle(numeSofer)
                                .setContentText(msg)
                                .setContentIntent(pendingIntent)
                                .setSmallIcon(R.drawable.sms);
                        notification = b.build();


                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){


                            NotificationManagerCompat notificationManager= NotificationManagerCompat.from(getApplicationContext());
                            notificationManager.notify(notificationId ,b.build());

                        }else{
                            startForeground(notificationId, notification);
                        }
                    }

                }catch (Exception e){

                    Toast.makeText(MyService.this, "EROARE!!", Toast.LENGTH_SHORT).show();
                    Log.e("onChildAdded",myPhoneNumber+" == "+receiverid);
                }


            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
    @SuppressLint("MissingPermission")
    private void getMyPhoneNumber(){
        TelephonyManager tmgr= (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        try{ myPhoneNumber=tmgr.getLine1Number();}catch (Exception e){Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();}
        if(myPhoneNumber.equals("")){
            myPhoneNumber = tmgr.getSubscriberId();
        }
        Toast.makeText(getApplicationContext(), "my phone is " +myPhoneNumber, Toast.LENGTH_SHORT).show();
    }

}