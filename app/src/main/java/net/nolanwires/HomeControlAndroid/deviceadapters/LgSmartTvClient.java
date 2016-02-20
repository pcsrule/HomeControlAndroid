package net.nolanwires.HomeControlAndroid.deviceadapters;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import net.nolanwires.HomeControlAndroid.util.Singleton;
import net.nolanwires.HomeControlAndroid.util.XMLHelpers;

import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Simple API to send key commands to some LG Smart TVs and projectors.
 * Created by nolan on 2/19/16.
 */
public class LgSmartTvClient {
    private static final String XML_VERSION_STRING = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
    private static final String PAIRING_KEY = "856887";
    private static final String TV_HOST = "projector";
    private static final String TV_PORT = "8080";
    private static final String TV_URL = "http://" + TV_HOST + ':' + TV_PORT + "/roap/api/";

    private static final String TAG = "LGTVCLIENT";

    public enum LG_KEYCODES {
        POWER(1),
        NUM_0(2),
        NUM_1(3),
        NUM_2(4),
        NUM_3(5),
        NUM_4(6),
        NUM_5(7),
        NUM_6(8),
        NUM_7(9),
        NUM_8(10),
        NUM_9(11),
        UP(12),
        DOWN(13),
        LEFT(14),
        RIGHT(15),
        OK(20),
        INPUT(47),
        ENERGY_SAVING(409);

        private final int keycode;
        LG_KEYCODES(int keycode) {
            this.keycode = keycode;
        }
        public int getKeycode() { return keycode; }
    }

    private String mSessionId;
    private Context mContext;
    private LinkedList<LG_KEYCODES> keyQueue;

    /**
     * Construct API.
     * @param context Used to get Volley thread pool.
     */
    public LgSmartTvClient(Context context) {
        mContext = context;
        keyQueue = new LinkedList<>();
    }

    /**
     * Send a key code to the TV.
     * @param keycode keycode to send.
     */
    public void sendKeyCode(LG_KEYCODES keycode) {
        keyQueue.add(keycode);
        pollKeyQueue();
    }

    /**
     * Send a key sequence to toggle power save on the LG PA75U (and probably others).
     */
    public void sendChangePowerSave() {
        keyQueue.add(LG_KEYCODES.ENERGY_SAVING);
        keyQueue.add(LG_KEYCODES.UP);
        keyQueue.add(LG_KEYCODES.UP);
        keyQueue.add(LG_KEYCODES.OK);
        pollKeyQueue();
    }

    private void pollKeyQueue() {
        final LG_KEYCODES keyCodeToSend = keyQueue.poll();
        if(keyCodeToSend == null)
            return;

        if(mSessionId != null) {
            final String keyCommandString = XML_VERSION_STRING +
                    "<command><session>" +
                    mSessionId +
                    "</session><type>HandleKeyInput</type><value>" +
                    keyCodeToSend.getKeycode() +
                    "</value></command>";

            LgXmlRequest keyCommandRequest = new LgXmlRequest(TV_URL + "command", keyCommandString, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    pollKeyQueue();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if(error.networkResponse.statusCode == 401) {
                        keyQueue.addFirst(keyCodeToSend);
                        getSessionId();
                    }
                }
            });

            Singleton.addRequestToQueue(mContext, keyCommandRequest);
        } else
            keyQueue.addFirst(keyCodeToSend);
            getSessionId();
    }

    private void getSessionId() {
        if (mSessionId == null) {
            final String pairCmdString = XML_VERSION_STRING
                    + "<auth><type>AuthReq</type><value>"
                    + PAIRING_KEY
                    + "</value></auth>";

            LgXmlRequest sessionIdRequest = new LgXmlRequest(TV_URL + "auth", pairCmdString, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Document d = XMLHelpers.getDocumentFromString(response);

                    try {
                        assert d != null : response;
                        mSessionId = d.getElementsByTagName("session").item(0).getTextContent();
                        pollKeyQueue();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, error.toString());
                }
            });

            Singleton.addRequestToQueue(mContext, sessionIdRequest);
        }
    }

    private class LgXmlRequest extends StringRequest {
        private String mBody;

        public LgXmlRequest(String url, String body, Response.Listener<String> listener, Response.ErrorListener errorListener) {
            super(Method.POST, url, listener, errorListener);
            mBody = body;
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            Map<String, String> headers = new HashMap<>();
            headers.put("Host", TV_HOST + ":" + TV_PORT);
            return headers;
        }

        @Override
        public byte[] getBody() throws AuthFailureError {
            return mBody.getBytes();
        }

        @Override
        public String getBodyContentType() {
            return "application/atom+xml";
        }
    }

}