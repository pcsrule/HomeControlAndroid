package net.nolanwires.HomeControlAndroid.deviceadapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import net.nolanwires.HomeControlAndroid.util.Singleton;
import net.nolanwires.HomeControlAndroid.util.XMLHelpers;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

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
    private final String TCP_HUB_PATH = "https://" + TCP_HUB_ADDRESS + "/gwr/gop.php";
    private final String TCP_HUB_ROOMGETCAROUSEL_CMD_PREFIX = "cmd=GWRBatch&data=%3Cgwrcmds%3E%3Cgwrcmd%3E%3Cgcmd%3ERoomGetCarousel%3C%2Fgcmd%3E%3Cgdata%3E%3Cgip%3E%3Cversion%3E1%3C%2Fversion%3E%3Ctoken%3E";
    private final String TCP_HUB_ROOMGETCAROUSEL_CMD_PREFIX_SUFFIX = "%3C%2Ftoken%3E%3Cfields%3Ename%2Ccontrol%2Cproduct%2Cclass%2Crealtype%2Cstatus%3C%2Ffields%3E%3C%2Fgip%3E%3C%2Fgdata%3E%3C%2Fgwrcmd%3E%3C%2Fgwrcmds%3E";

    private ArrayList<Light> lights;
    private HashMap<String, Light> lightsMap;
    private OnLightStatusUpdateListener mListener;
    private Context mContext;
    private String mTCPHubToken;
    private boolean mSyncInProgress = false;

    /**
     * Essentially a struct to hold data needed to represent a Light
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
    public TcpConnectedLightingClient(Context context, OnLightStatusUpdateListener listener, String token) {
        lights = new ArrayList<>();
        lightsMap = new HashMap<>();
        mContext = context;
        mListener = listener;
        mTCPHubToken = token;
    }

    public interface OnLightStatusUpdateListener {
        void OnLightStatusUpdate(String token);
    }

    /**
     * Get a light by its friendly name.
     *
     * @param name the name to lookup.
     * @return the first light found to contain the given String, or null if none are found.
     */
    public Light getLightForName(String name) {
        for (Light l : lights) {
            if (name.contains(l.name.toLowerCase().split(" ", 2)[0]))
                return l;
        }
        return null;
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

        if (mTCPHubToken == null && !mSyncInProgress) {
            getToken();
            return;
        }

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


                        if ((tmp = XMLHelpers.getContentForTagName(lightAttributes, "level")) != null)
                            newLight.brightness = Integer.valueOf(tmp);
                        else
                            newLight.brightness = 0;

                        newLight.isOn = (tmp = XMLHelpers.getContentForTagName(lightAttributes, "state")) != null && tmp.equals("1");

                        newLight.offline = (tmp = XMLHelpers.getContentForTagName(lightAttributes, "offline")) != null && tmp.equals("1");
                    }

                    // callback
                    mListener.OnLightStatusUpdate(mTCPHubToken);
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
                return (TCP_HUB_ROOMGETCAROUSEL_CMD_PREFIX + mTCPHubToken + TCP_HUB_ROOMGETCAROUSEL_CMD_PREFIX_SUFFIX).getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
        };

        Singleton.addRequestToQueue(mContext, GWRBatchRequest);
    }

    private void getToken() {
        if (mSyncInProgress) {
            return;
        }
        mSyncInProgress = true;

        String guid = UUID.randomUUID().toString();
        final String GWRLoginCmd = "cmd=GWRLogin&data=<gip><version>1</version><email>" + guid + "</email><password>" + guid + "</password></gip>&fmt=xml";

        Response.Listener<String> responseListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Document d = XMLHelpers.getDocumentFromString(response);

                if (d != null) {
                    String token = null;
                    Node tokenNode = d.getElementsByTagName("token").item(0);

                    if (tokenNode != null) {
                        token = tokenNode.getTextContent();
                    }

                    mSyncInProgress = false;

                    if (token != null) {
                        mTCPHubToken = token;
                        mListener.OnLightStatusUpdate(mTCPHubToken);
                        getLights();
                    }
                }
            }
        };

        StringRequest setRequest = new StringRequest(Request.Method.POST, TCP_HUB_PATH, responseListener, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, error.toString());
                mSyncInProgress = false;
            }
        }) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    Log.d("shit", URLEncoder.encode(GWRLoginCmd, "UTF-8"));
                    return URLEncoder.encode(GWRLoginCmd, "UTF-8").getBytes();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
        };
        Singleton.addRequestToQueue(mContext, setRequest);
    }

    /**
     * Turn a TCP light on or off by id.
     *
     * @param id   The id of the light for which brightness will be set
     * @param isOn Brightness level to set (will be capped 0-100)
     */
    public void setIsLightOn(final String id, final boolean isOn) {
        String setOnCmd = "cmd=GWRBatch&data=%3Cgwrcmds%3E%3Cgwrcmd%3E%3Cgcmd%3EDeviceSendCommand%3C%2Fgcmd%3E%3Cgdata%3E%3Cgip%3E%3Cversion%3E1%3C%2Fversion%3E%3Ctoken%3E"
                + mTCPHubToken
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
                mListener.OnLightStatusUpdate(mTCPHubToken);
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
                + mTCPHubToken
                + "%3C%2Ftoken%3E%3Cdid%3E"
                + id
                + "%3C%2Fdid%3E%3Cvalue%3E"
                + brightness
                + "%3C%2Fvalue%3E%3Ctype%3Elevel%3C%2Ftype%3E%3C%2Fgip%3E%3C%2Fgdata%3E%3C%2Fgwrcmd%3E%3Cgwrcmd%3E%3Cgcmd%3EDeviceSendCommand%3C%2Fgcmd%3E%3Cgdata%3E%3Cgip%3E%3Cversion%3E1%3C%2Fversion%3E%3Ctoken%3E"
                + mTCPHubToken
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
                mListener.OnLightStatusUpdate(mTCPHubToken);
            }
        };

        sendLightUpdate(setBrightnessCmd, responseListener);
    }

    private void sendLightUpdate(final String cmd, Response.Listener<String> responseListener) {
        StringRequest setRequest = new StringRequest(Request.Method.POST, TCP_HUB_PATH, responseListener, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, error.toString());
                //mListener.OnLightStatusUpdate();
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
