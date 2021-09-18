package com.example.nextstop;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;


public class NotificationReceiver extends BroadcastReceiver {

    public boolean cancel_alarm = false;

    @Override
    public void onReceive(Context context, Intent intent) {

        context.sendBroadcast(new Intent("STOP_ALERTS"));

        String action = intent.getAction();

        if (App.CANCEL_ALARM.equals(action)) {
            Toast.makeText(context, "Cancel Alarm  Called: " + action, Toast.LENGTH_SHORT).show();
            cancel_alarm = true;
            context.stopService(new Intent(context, GPS_Service.class));

        } else if (App.STOP_ALARM.equals(action)) {
            Toast.makeText(context, "Stop Alarm Called: " + action, Toast.LENGTH_SHORT).show();
            context.stopService(new Intent(context, GPS_Service.class));
            context.sendBroadcast(new Intent("STOP_ALERTS"));

        }


    }//end of onRecieve


}
