package net.nolanwires.HomeControlAndroid.util;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.net.InetAddress;

import javax.jmdns.JmDNS;

/**
 * Created by nolan on 2/18/16.
 */
public class Singleton {
    private static RequestQueue queue = null;

    public static void addRequestToQueue(Context context, Request request) {
        if(queue == null) {
            queue = Volley.newRequestQueue(context.getApplicationContext());
        }
        queue.add(request);
    }

}
