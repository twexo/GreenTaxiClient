package com.twexdo.client;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
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

import java.util.Objects;
import java.util.Random;

public class MyService extends Service {
    private static final int NOTIF_ID = 2021;
    private final String TAG="MyService:TAG";
    public String myPhoneNumber = "";
    String msg, id,numeSofer;
    double x, y;
    int timp;
    String receiverid;
    PendingIntent pendingIntent;
    Notification notification;
    NotificationCompat.Builder b;
    public static final String CHANNEL_ID = "ForegroundServiceChannelServiceNotif";
    public static final String SMS_CHANNEL_ID = "ForegroundServiceChannelSms";
    int notificationId;

    PowerManager.WakeLock wakeLock;
    PowerManager powerManager;

    private NotificationManager mNotificationManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand");
        try {
            wakeLock.acquire(60 * 60 * 1000L /*1 hour*/);
            Log.d(TAG,"WakeLock aquired");
        }catch (Exception e){
            Log.e(TAG,e.toString());
        }
        getMyPhoneNumber();
        retrivemsg();
        startForeground(NOTIF_ID,getNotification());
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = Objects.requireNonNull(powerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.adrian.sofergt::ServiceWakeLock");
        Log.d(TAG,"onCreate");
        mNotificationManager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();
        Uri sound = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/raw/silent");
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            Log.d(TAG,"Build version is >= API 26");
            NotificationChannel mNotificationChannel=new NotificationChannel(CHANNEL_ID,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationChannel.setSound(sound, audioAttributes);
            mNotificationManager.createNotificationChannel(mNotificationChannel);
        }

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

                    if (receiverid != null ? receiverid.equals(myPhoneNumber) : false) {
                        Toast.makeText(MyService.this, "Mesaj pentru mine!", Toast.LENGTH_SHORT).show();
                        id = dataSnapshot.child("from").getValue(String.class);
                        numeSofer = dataSnapshot.child("nume").getValue(String.class);
                        int type = dataSnapshot.child("type").getValue(Integer.class);
                        if (type == 1) {
                            int time = dataSnapshot.child("time").getValue(Integer.class);
                            if (time > 0) {
                                msg = numeSofer+" a confirmat ca va ajunge in " + time + " minute. \n"+id;
                                Intent notificationIntent = new Intent(MyService.this, ClientConfirmare.class);
                                notificationIntent.putExtra("id", id);
                                notificationIntent.putExtra("numeSofer", numeSofer);
                                notificationIntent.putExtra("myphNr",receiverid);
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
                        }else if (type == 3) {
                            msg = "Soferul a refuzat comanda.";
                            Intent notificationIntent = new Intent(MyService.this, MainActivity.class);
                            notificationIntent.putExtra("id", id);
                            notificationIntent.putExtra("smsid",dataSnapshot.getKey());
                            notificationIntent.putExtra("notificationId",notificationId);
                            pendingIntent = PendingIntent.getActivity(MyService.this,
                                    0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                            FirebaseDatabase.getInstance().getReference("mesaj").child(dataSnapshot.getKey()).setValue(null);
                        }
                        createNotificationChannel();
                        b = new NotificationCompat.Builder(getApplicationContext(), SMS_CHANNEL_ID)
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

                        PowerManager pm = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);

                        PowerManager.WakeLock wl = pm.newWakeLock(805306394 ,"com.adrian:MyLock");
                        wl.acquire(10000);
                        PowerManager.WakeLock wl_cpu = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"com.adrian:MyCpuLock");

                        wl_cpu.acquire(10000);
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

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
        wakeLock.release();
        super.onDestroy();
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    SMS_CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
    @SuppressLint({"MissingPermission"})
    private void getMyPhoneNumber(){

        TelephonyManager tmgr= (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //try{ myPhoneNumber= Objects.requireNonNull(tmgr).getLine1Number();}catch (Exception e){  myPhoneNumber="";Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();}

            myPhoneNumber = Objects.requireNonNull(tmgr).getSubscriberId();

        Toast.makeText(getApplicationContext(), "my phone is " +myPhoneNumber, Toast.LENGTH_SHORT).show();
    }
    private Notification getNotification() {


        NotificationCompat.Builder builder=new NotificationCompat.Builder(this)
                .setContentText("Service in background")
                .setContentTitle("GreenTaxi")
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .setSmallIcon(R.drawable.sms)
                //.setWhen(System.currentTimeMillis())
                ;

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            builder.setChannelId(CHANNEL_ID);
        }
        Log.d(TAG,"returned builder.build()");
        return builder.build();


    }

}