package com.valinet.homepi;

import android.content.Context;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
//android:icon="@mipmap/ic_launcher"
    String ips;
    private NsdManager mNsdManager;
    private String SERVICE_NAME = "Client Device";
    private String SERVICE_TYPE = "_workstation._tcp.";
    private static final String TAG = "BonjourManager";
    WifiManager.MulticastLock multicastLock = null;
    SwipeRefreshLayout mySwipeRefreshLayout;
    WebView view;
    HashMap<String, String> hosts = new HashMap<String, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        findViewById(android.R.id.content).getRootView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        super.onCreate(savedInstanceState);

        try
        {
            this.getSupportActionBar().hide();
        }
        catch (NullPointerException e){}

        setContentView(R.layout.activity_main);

        mySwipeRefreshLayout = (SwipeRefreshLayout)this.findViewById(R.id.swipeContainer);
        mySwipeRefreshLayout.setRefreshing(true);
        view=(WebView) this.findViewById(R.id.webview);
        view.getSettings().setJavaScriptEnabled(true);
        view.getSettings().setBuiltInZoomControls(true);
        view.getSettings().setDisplayZoomControls(false);
        view.setLongClickable(true);
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
        view.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading (WebView view,
                                                     WebResourceRequest request)
            {
                Uri url = request.getUrl();
                Log.e(TAG, "url: " + url.getHost().toString());
                if (hosts.containsKey(url.getHost().toString()))
                {
                    URL originalURL = null;
                    try {
                        originalURL = new URL(url.toString());
                        try {
                            URL newURL = new URL(
                                    originalURL.getProtocol(),
                                    hosts.get(url.getHost().toString()),
                                    originalURL.getPort(),
                                    originalURL.getFile()
                            );
                            view.loadUrl(newURL.toString());
                            return true;
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mySwipeRefreshLayout.setRefreshing(false);
                view.loadUrl("javascript:(function() {" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        // Tell the browser to BASE64-decode the string into your script !!!
                        "style.innerHTML = '.container { background-color: #ffffff }';" +
                        "parent.appendChild(style)" +
                        "})()");
            }
        });
        //view.loadUrl(url);

        mySwipeRefreshLayout.setOnRefreshListener(
            new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    view.loadUrl("http://" + ips);
                }
            }
        );

        WifiManager wifi = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifi.createMulticastLock("multicastLock");
        multicastLock.setReferenceCounted(true);
        multicastLock.acquire();

        mNsdManager = (NsdManager)getSystemService(Context.NSD_SERVICE);
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    NsdManager.DiscoveryListener mDiscoveryListener = new NsdManager.DiscoveryListener() {

        // Called as soon as service discovery begins.
        @Override
        public void onDiscoveryStarted(String regType) {
            Log.d(TAG, "Service discovery started");
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            // A service was found! Do something with it.
            //Log.d(TAG, "Service discovery success : " + service);
            //Log.d(TAG, "Host = "+ service.getHost());
            //Log.d(TAG, "port = " + String.valueOf(service.getPort()));

            if (!service.getServiceType().equals(SERVICE_TYPE)) {
                // Service type is the string containing the protocol and
                // transport layer for this service.
                Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
            } else if (service.getServiceName().equals(SERVICE_NAME)) {
                // The name of the service tells the user what they'd be
                // connecting to. It could be "Bob's Chat App".
                Log.d(TAG, "Same machine: " + SERVICE_NAME);
            } else {
                Log.d(TAG, "Found service");

                // connect to the service and obtain serviceInfo
                mNsdManager.resolveService(service, new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        // Called when the resolve fails.  Use the error code to debug.
                        Log.e(TAG, "Resolve failed" + errorCode);
                    }

                    @Override
                    public void onServiceResolved(NsdServiceInfo serviceInfo) {
                        Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

                        if (serviceInfo.getServiceName().equals(SERVICE_NAME)) {
                            Log.d(TAG, "Same IP.");
                            return;
                        }

                        int port = serviceInfo.getPort();
                        InetAddress host = serviceInfo.getHost(); // getHost() will work now

                        final String addr = serviceInfo.getHost().toString().replace("/", "");
                        Log.e(TAG, "Added: " + serviceInfo.getServiceName().toString().split(" ")[0] + ".local");
                        hosts.put(serviceInfo.getServiceName().toString().split(" ")[0] + ".local", addr);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //WebView myWebView = (WebView) findViewById(R.id.webview);
                                //myWebView.setWebViewClient(new WebViewClient());
                                view.loadUrl("http://" + addr);
                                ips = addr;
                            }
                        });
                    }
                });
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo service) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Log.e(TAG, "service lost" + service);
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.i(TAG, "Discovery stopped: " + serviceType);
            if (multicastLock != null) {
                multicastLock.release();
                multicastLock = null;
            }
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            mNsdManager.stopServiceDiscovery(this);
            if (multicastLock != null) {
                multicastLock.release();
                multicastLock = null;
            }
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            mNsdManager.stopServiceDiscovery(this);
            if (multicastLock != null) {
                multicastLock.release();
                multicastLock = null;
            }
        }
    };
}
