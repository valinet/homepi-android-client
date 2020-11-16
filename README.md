# homepi Android client
This is a simple Android application that presents the user with a web view that opens the home page for homepi. The reason we have to provide an app for that is because Android does not have mDNS support in the default resolver, hence most (if not all) web browsers on the platform cannot access .local domains.

This app uses Android's DNS-SD API (NsdManager) to discover the homepi service in the local network, gathering its IP address from there and accessing its web page directly via the obtained web address.

Of course, this is pretty redundant if you set homepi to use a static address, but this serves as a good working example and helps convery a complete implementation for the project.

Code is based on example from Google: https://developer.android.com/reference/android/net/nsd/NsdManager.

Compile using Android Studio. SDK is set to 30 but I don't think I use functionality specific to that, so it may compile find with older versions as well.

Check out the main homepi project at https://github.com/valinet/homepi-plus.