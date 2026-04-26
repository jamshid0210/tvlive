package com.tvapp.webview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * TV yonganida avtomatik ishga tushadigan Receiver
 * AndroidManifest da RECEIVE_BOOT_COMPLETED ruxsati kerak
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            // Kichik delay - tizim to'liq yuklanishini kutish (3 soniya)
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // MainActivity ni ishga tushirish
            Intent launchIntent = new Intent(context, MainActivity.class);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(launchIntent);
        }
    }
}
