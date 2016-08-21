package net.nolanwires.HomeControlAndroid.deviceadapters;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.concurrent.Semaphore;

import static net.nolanwires.HomeControlAndroid.fragments.WakeOnLanAdapterFragment.MSG_MAC_FOUND;

/**
 * Created by nolan on 8/16/16.
 */

public class WakeOnLanClient extends Thread implements Serializable {

    private static final String TAG = "WOL";
    private static final int WOL_PORT = 9;
    private String mDNSName;
    private String mHostMACAddress;
    private Semaphore mSemaphore;
    private InetAddress mBroadcastAddress;
    private Handler mHandler;
    private InetAddress mInetAddress;

    public WakeOnLanClient(String dnsName, InetAddress ipAddress, String mac, InetAddress broadcastAddress, Handler handler) {
        mDNSName = dnsName;
        mHostMACAddress = mac;
        mSemaphore = new Semaphore(0);
        mBroadcastAddress = broadcastAddress;
        mInetAddress = ipAddress;
        mHandler = handler;

        // Start worker thread
        start();
    }

    public void doRequest() {
        mSemaphore.release();
    }

    @Override
    public synchronized void run() {

        if (mHostMACAddress == null) {
            lookupMACAddressForHostname();
        }

        while (!interrupted()) {
            try {
                mSemaphore.acquire();
                sendWakeOnLanRequest();
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void sendWakeOnLanRequest() {
        Log.d(TAG, "send request to:" + mHostMACAddress);

        try {
            byte[] macBytes = getMacBytes();
            byte[] bytes = new byte[6 + 16 * macBytes.length];
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) 0xff;
            }
            for (int i = 6; i < bytes.length; i += macBytes.length) {
                System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
            }

            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, mBroadcastAddress, WOL_PORT);
            DatagramSocket socket = new DatagramSocket();
            socket.send(packet);
            socket.close();

            Log.d(TAG, "Sent WOL packet");
        } catch (Exception e) {
            Log.d(TAG, "Failed to send WOL packet: " + e.toString());
        }
    }

    private byte[] getMacBytes() throws IllegalArgumentException {
        byte[] bytes = new byte[6];
        String[] hex = mHostMACAddress.split("(\\:|\\-)");
        if (hex.length != 6) {
            throw new IllegalArgumentException("Invalid MAC address.");
        }
        try {
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) Integer.parseInt(hex[i], 16);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex digit in MAC address.");
        }
        return bytes;
    }

    private void lookupMACAddressForHostname() {
        String ip = null;

        // Try to poke the host to generate an ARP request
        try {
            while (!interrupted()) {
                try {
                    if(mInetAddress == null)
                        mInetAddress = InetAddress.getByName(mDNSName);

                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(mInetAddress, 90), 10);
                    if (socket.isConnected()) {
                        socket.close();
                    }
                } catch (UnknownHostException e) {
                    Log.d(TAG, "DNS lookup failed");
                } catch (IOException ignored) {
                }

                mHostMACAddress = getMacFromArpCache(mInetAddress.getHostAddress());

                if (mHostMACAddress != null) {
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_MAC_FOUND, this));

                    return;
                }

                Thread.sleep(5000);
            }
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Try to extract a hardware MAC address from a given IP address using the
     * ARP cache (/proc/net/arp).<br>
     * <br>
     * We assume that the file has this structure:<br>
     * <br>
     * IP address       HW type     Flags       HW address            Mask     Device
     * 192.168.18.11    0x1         0x2         00:04:20:06:55:1a     *        eth0
     * 192.168.18.36    0x1         0x2         00:22:43:ab:2a:5b     *        eth0
     *
     * @param ip
     * @return the MAC from the ARP cache
     */

    private String getMacFromArpCache(String ip) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split(" +");
                if (splitted.length >= 4 && ip.equals(splitted[0])) {
                    // Basic sanity check
                    String mac = splitted[3];
                    if (!mac.equals("00:00:00:00:00:00") && mac.matches("..:..:..:..:..:..")) {
                        return mac;
                    } else {
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        } finally {
            try {
                assert br != null;
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return mDNSName + " (" + mHostMACAddress + ")";
    }

    public String getMACString() {
        return mHostMACAddress;
    }

    public String getDNSName() {
        return mDNSName;
    }

    public InetAddress getIPAddress() {
        return mInetAddress;
    }
}
