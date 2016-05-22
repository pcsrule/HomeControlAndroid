package net.nolanwires.HomeControlAndroid.deviceadapters;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.security.GeneralSecurityException;

import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.ChromeCastSpontaneousEvent;
import su.litvak.chromecast.api.v2.ChromeCastSpontaneousEventListener;
import su.litvak.chromecast.api.v2.ChromeCasts;
import su.litvak.chromecast.api.v2.ChromeCastsListener;
import su.litvak.chromecast.api.v2.MediaStatus;
import su.litvak.chromecast.api.v2.Status;
import su.litvak.chromecast.api.v2.Volume;

/**
 * Threaded wrapper to allow easy use of su.litvak.chromecast.api.v2 from an Android UI thread.
 * Created by nolan on 2/21/16.
 */
public class ChromecastClient extends Thread implements Handler.Callback, ChromeCastsListener, ChromeCastSpontaneousEventListener {
    public static final int MSG_NEWSTATUS = 0;
    public static final int MSG_NEWNAME = 1;
    public static final int MSG_NEWMEDIASTATUS = 2;

    public static final int ACTION_STOPAPP = 0;
    public static final int ACTION_VOLUMEUP = 3;
    public static final int ACTION_VOLUMEDOWN = 4;

    private static final String TAG = "CHROMECAST";

    private String mDeviceName = "Chromecast";
    private ChromeCast mChromecast;
    private Handler mResponseHandler;
    private Looper mLooper;
    private Handler mThreadHandler;

    public ChromecastClient(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
        start();
    }

    public String getDeviceName() {
        return mDeviceName;
    }

    @Override
    public void run() {
        Looper.prepare();
        synchronized (this) {
            mLooper = Looper.myLooper();
            notifyAll();
        }

        mThreadHandler = new Handler(mLooper, this);

        try {
            synchronized (this) {
                ChromeCasts.registerListener(this);
                ChromeCasts.startDiscovery();

                wait();

                mChromecast = ChromeCasts.get().get(0);

                mDeviceName = mChromecast.getName();
                mResponseHandler.sendMessage(mResponseHandler.obtainMessage(MSG_NEWNAME, mDeviceName));

                mChromecast.registerListener(this);
                if (!mChromecast.isConnected())
                    mChromecast.connect();

                Status status = mChromecast.getStatus();
                if (status != null)
                    mResponseHandler.sendMessage(mResponseHandler.obtainMessage(MSG_NEWSTATUS, status.applications.get(0).name));

                Looper.loop();

                mChromecast.disconnect();
                ChromeCasts.stopDiscovery();
                mLooper.quit();
            }

        } catch (IOException | InterruptedException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        Volume curVolume;
        try {
            switch (msg.what) {
                case ACTION_STOPAPP:
                    mChromecast.stopApp();
                    return true;
                case ACTION_VOLUMEUP:
                    curVolume = mChromecast.getMediaStatus().volume;
                    mChromecast.setVolume(curVolume.level + curVolume.increment);
                    return true;
                case ACTION_VOLUMEDOWN:
                    curVolume = mChromecast.getMediaStatus().volume;
                    mChromecast.setVolume(curVolume.level - curVolume.increment);
                    return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void sendAction(int what) {
        if (mThreadHandler != null)
            mThreadHandler.sendMessage(mThreadHandler.obtainMessage(what));
    }

    public void quit() {
        if (mLooper != null) {
            mLooper.quit();
        }
    }

    @Override
    public void newChromeCastDiscovered(ChromeCast chromeCast) {
        synchronized (this) {
            notify();
        }
    }

    @Override
    public void chromeCastRemoved(ChromeCast chromecast) {
        quit();
    }

    @Override
    public void spontaneousEventReceived(ChromeCastSpontaneousEvent event) {
        Log.d(TAG, event.getType().toString());
        switch (event.getType()) {
            case STATUS:
                Status status = (Status) event.getData();
                if (status != null && !status.applications.isEmpty()) {
                    mResponseHandler.sendMessage(mResponseHandler.obtainMessage(MSG_NEWSTATUS, status.applications.get(0).name));
                }
                break;
            case MEDIA_STATUS:
                MediaStatus mediaStatus = (MediaStatus) event.getData();
                if (mediaStatus != null) {
                    mResponseHandler.sendMessage(mResponseHandler.obtainMessage(MSG_NEWMEDIASTATUS, mediaStatus.playerState.toString()));
                }
                break;
        }
    }
}
