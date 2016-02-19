package net.nolanwires.HomeControlAndroid.deviceadapters;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import net.nolanwires.HomeControlAndroid.util.Singleton;
import net.nolanwires.HomeControlAndroid.util.XMLHelpers;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Controls TCP Connected lights using a simple interface
 * Created by nolan on 2/18/16.
 */
public class TcpConnectedLightingClient {
    private static final String TAG = TcpConnectedLightingClient.class.getName();
    private final String TCP_HUB_ADDRESS = "lighting";
    private final String TCP_HUB_TOKEN = "72pplj0qkktntqewyu8e71bzxnjraeam6piwfxd6";
    private final String TCP_HUB_PATH = "https://" + TCP_HUB_ADDRESS + "/gwr/gop.php";
    private final String TCP_HUB_ROOMGETCAROUSEL_CMD = "cmd=GWRBatch&data=%3Cgwrcmds%3E%3Cgwrcmd%3E%3Cgcmd%3ERoomGetCarousel%3C%2Fgcmd%3E%3Cgdata%3E%3Cgip%3E%3Cversion%3E1%3C%2Fversion%3E%3Ctoken%3E" + TCP_HUB_TOKEN + "%3C%2Ftoken%3E%3Cfields%3Ename%2Ccontrol%2Cproduct%2Cclass%2Crealtype%2Cstatus%3C%2Ffields%3E%3C%2Fgip%3E%3C%2Fgdata%3E%3C%2Fgwrcmd%3E%3C%2Fgwrcmds%3E";

    private ArrayList<Light> lights;
    private HashMap<String, Light> lightsMap;
    private OnLightStatusUpdateListener mListener;
    private Context mContext;

    /**
     * Essentially a struct to hold data needed to represent
     */
    public class Light {
        private String id, name;
        private int brightness;
        private boolean offline, isOn;

        public boolean isOn() {
            return isOn;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getBrightness() {
            return brightness;
        }

        public boolean isOffline() {
            return offline;
        }
    }

    /**
     * Construct a new TCP lighting client. Application context is needed to allow Volley to reuse
     * thread pools if available.
     *
     * @param context  Application context to find a Volley RequestQueue instance.
     * @param listener Callback interface to notify of light status updates.
     */
    public TcpConnectedLightingClient(Context context, OnLightStatusUpdateListener listener) {
        lights = new ArrayList<>();
        lightsMap = new HashMap<>();
        mContext = context;
        mListener = listener;
    }

    public interface OnLightStatusUpdateListener {
        void OnLightStatusUpdate();
    }

    /**
     * Gets a reference to the Light object with the specified ID.
     *
     * @param id The id of the Light to lookup.
     * @return The light with the provided ID, or null if the ID wasn't valid.
     */
    public Light getLightForId(String id) {
        return lightsMap.get(id);
    }

    /**
     * Gets a reference to the Light object with the specified index in the backing list.
     *
     * @param index The index of the Light to lookup.
     * @return The light with the provided index, or null if the index was invalid.
     */
    public Light getLightForIndex(int index) {
        return lights.get(index);
    }

    /**
     * Gets the number of lights in the backing list.
     *
     * @return number of known lights.
     */
    public int getNumLights() {
        return lights.size();
    }

    /**
     * Poll the TCP hub for the current state of all lights, expect a callback to the listener
     * provided at construction.
     */
    public void getLights() {

        StringRequest GWRBatchRequest = new StringRequest(Request.Method.POST, TCP_HUB_PATH, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // parse XML
                Document d = XMLHelpers.getDocumentFromString(response);
                if (d != null) {
                    NodeList lightNodeList = d.getElementsByTagName("did");

                    for (int i = 0; i < lightNodeList.getLength(); ++i) {
                        String tmp;
                        Light newLight;

                        tmp = lightNodeList.item(i).getTextContent();
                        newLight = lightsMap.get(tmp);

                        if (newLight == null) {
                            newLight = new Light();
                            newLight.id = tmp;
                            lights.add(newLight);
                            lightsMap.put(tmp, newLight);
                        }

                        NodeList lightAttributes = lightNodeList.item(i).getParentNode().getChildNodes();

                        newLight.name = XMLHelpers.getContentForTagName(lightAttributes, "name");


                        if ((tmp = XMLHelpers.getContentForTagName(lightAttributes, "level")) != null) {
                            newLight.brightness = Integer.valueOf(tmp);
                        }
                        if ((tmp = XMLHelpers.getContentForTagName(lightAttributes, "state")) != null) {
                            newLight.isOn = tmp.equals("1");
                        }

                        if ((tmp = XMLHelpers.getContentForTagName(lightAttributes, "offline")) != null) {
                            newLight.offline = tmp.equals("1");
                        }
                    }

                    // callback
                    mListener.OnLightStatusUpdate();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, error.toString());
            }
        }) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                return TCP_HUB_ROOMGETCAROUSEL_CMD.getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
        };

        Singleton.addRequestToQueue(mContext, GWRBatchRequest);
    }

    /**
     * Turn a TCP light on or off by id.
     *
     * @param id   The id of the light for which brightness will be set
     * @param isOn Brightness level to set (will be capped 0-100)
     */
    public void setIsLightOn(final String id, final boolean isOn) {
        String setOnCmd = "cmd=GWRBatch&data=%3Cgwrcmds%3E%3Cgwrcmd%3E%3Cgcmd%3EDeviceSendCommand%3C%2Fgcmd%3E%3Cgdata%3E%3Cgip%3E%3Cversion%3E1%3C%2Fversion%3E%3Ctoken%3E"
                + TCP_HUB_TOKEN
                + "%3C%2Ftoken%3E%3Cdid%3E"
                + id
                + "%3C%2Fdid%3E%3Cvalue%3E"
                + (isOn ? "1" : "0")
                + "%3C%2Fvalue%3E%3C%2Fgip%3E%3C%2Fgdata%3E%3C%2Fgwrcmd%3E%3C%2Fgwrcmds%3E";

        Response.Listener<String> responseListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // HTTP 200, don't really need to check the response
                Light updatedLight = getLightForId(id);
                updatedLight.isOn = isOn;
                mListener.OnLightStatusUpdate();
            }
        };

        sendLightUpdate(setOnCmd, responseListener);
    }

    /**
     * Set the brightness of a TCP light by id. If the brightness requested is non-zero, the light will be
     * turned on. If the brightness is zero, the light will be turned off.
     *
     * @param id         The id of the light for which brightness will be set
     * @param brightness Brightness level to set (will be capped 0-100)
     */
    public void setLightBrightness(final String id, final int brightness) {
        String setBrightnessCmd = "cmd=GWRBatch&data=%3Cgwrcmds%3E%3Cgwrcmd%3E%3Cgcmd%3EDeviceSendCommand%3C%2Fgcmd%3E%3Cgdata%3E%3Cgip%3E%3Cversion%3E1%3C%2Fversion%3E%3Ctoken%3E"
                + TCP_HUB_TOKEN
                + "%3C%2Ftoken%3E%3Cdid%3E"
                + id
                + "%3C%2Fdid%3E%3Cvalue%3E"
                + brightness
                + "%3C%2Fvalue%3E%3Ctype%3Elevel%3C%2Ftype%3E%3C%2Fgip%3E%3C%2Fgdata%3E%3C%2Fgwrcmd%3E%3Cgwrcmd%3E%3Cgcmd%3EDeviceSendCommand%3C%2Fgcmd%3E%3Cgdata%3E%3Cgip%3E%3Cversion%3E1%3C%2Fversion%3E%3Ctoken%3E"
                + TCP_HUB_TOKEN
                + "%3C%2Ftoken%3E%3Cdid%3E"
                + id
                + "%3C%2Fdid%3E%3Cvalue%3E"
                + (brightness == 0 ? "0" : "1")
                + "%3C%2Fvalue%3E%3C%2Fgip%3E%3C%2Fgdata%3E%3C%2Fgwrcmd%3E%3C%2Fgwrcmds%3E";

        Response.Listener<String> responseListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Light updatedLight = getLightForId(id);
                updatedLight.brightness = brightness;
                updatedLight.isOn = brightness != 0;
                mListener.OnLightStatusUpdate();
            }
        };

        sendLightUpdate(setBrightnessCmd, responseListener);
    }

    private void sendLightUpdate(final String cmd, Response.Listener<String> responseListener) {
        StringRequest setRequest = new StringRequest(Request.Method.POST, TCP_HUB_PATH, responseListener, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, error.toString());
                mListener.OnLightStatusUpdate();
            }
        }) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                return cmd.getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
        };
        Singleton.addRequestToQueue(mContext, setRequest);
    }

    // Accept all SSL certificates for now
    static {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            X509Certificate[] myTrustedAnchors = new X509Certificate[0];
                            return myTrustedAnchors;
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
