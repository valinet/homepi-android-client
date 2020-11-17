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
    private String SERVICE_TYPE = "_valinet._tcp.";
    WifiManager.MulticastLock multicastLock = null;
    SwipeRefreshLayout mySwipeRefreshLayout;
    WebView view;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        findViewById(android.R.id.content).getRootView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

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
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mySwipeRefreshLayout.setRefreshing(false);
                view.loadUrl("javascript:(function() {" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        // Tell the browser to BASE64-decode the string into your script !!!
                        "style.innerHTML = '.container { background-color: #ffffff }';" +
                        "parent.appendChild(style);" +
                        "document.getElementsByTagName('h2').item(0).innerHTML = '<br>homepi+';" +
                        "})()");
            }
        });

        mySwipeRefreshLayout.setOnRefreshListener(
            new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    view.loadUrl("http://" + ips);
                }
            }
        );

        WifiManager wifi = (WifiManager)getApplicationContext().getSystemService(
                Context.WIFI_SERVICE
        );
        multicastLock = wifi.createMulticastLock("multicastLock");
        multicastLock.setReferenceCounted(true);
        multicastLock.acquire();

        mNsdManager = (NsdManager)getSystemService(Context.NSD_SERVICE);
        mNsdManager.discoverServices(
                SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD,
                mDiscoveryListener
        );
    }

    NsdManager.DiscoveryListener mDiscoveryListener = new NsdManager.DiscoveryListener()
    {
        // Called as soon as service discovery begins.
        @Override
        public void onDiscoveryStarted(String regType) {
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            if (service.getServiceType().equals(SERVICE_TYPE)) {
                mNsdManager.resolveService(service, new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                    }

                    @Override
                    public void onServiceResolved(NsdServiceInfo serviceInfo) {
                        if (serviceInfo.getServiceName().equals(SERVICE_NAME)) {
                            return;
                        }

                        final int port = serviceInfo.getPort();
                        final InetAddress host = serviceInfo.getHost(); // getHost() will work now
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (port == 80) {
                                    ips = host.toString();
                                    view.loadUrl("http://" + host.toString());
                                }
                            }
                        });
                    }
                });
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo service)
        {
        }

        @Override
        public void onDiscoveryStopped(String serviceType)
        {
            if (multicastLock != null) {
                multicastLock.release();
                multicastLock = null;
            }
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode)
        {
            mNsdManager.stopServiceDiscovery(this);
            if (multicastLock != null) {
                multicastLock.release();
                multicastLock = null;
            }
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode)
        {
            mNsdManager.stopServiceDiscovery(this);
            if (multicastLock != null) {
                multicastLock.release();
                multicastLock = null;
            }
        }
    };
}
