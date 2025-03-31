package com.cordova.plugins.cookiemaster;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.os.Build;
import android.util.Log;

import java.net.HttpCookie;

import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebSettings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class CookieMaster extends CordovaPlugin {

    private final String TAG = "CookieMasterPlugin";
    public static final String ACTION_GET_COOKIE_VALUE = "getCookieValue";
    public static final String ACTION_SET_COOKIE_VALUE = "setCookieValue";
    public static final String ACTION_CLEAR_COOKIES = "clearCookies";

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

        if (ACTION_GET_COOKIE_VALUE.equals(action)) {
            final String url = args.getString(0);
            final String cookieName = args.getString(1);

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        CookieManager cookieManager = CookieManager.getInstance();
                        String[] cookies = cookieManager.getCookie(url).split("; ");
                        String cookieValue = "";

                        for (int i = 0; i < cookies.length; i++) {
                            if (cookies[i].contains(cookieName + "=")) {
                                cookieValue = cookies[i].split("=")[1].trim();
                                break;
                            }
                        }

                        JSONObject json = null;
                        if (cookieValue != "") {
                            json = new JSONObject("{cookieValue:\"" + cookieValue + "\"}");
                        }
                        if (json != null) {
                            PluginResult res = new PluginResult(PluginResult.Status.OK, json);
                            callbackContext.sendPluginResult(res);
                        } else {
                            callbackContext.error("Cookie not found!");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Exception: " + e.getMessage());
                        callbackContext.error(e.getMessage());
                    }
                }
            });
            return true;

        } else if (ACTION_SET_COOKIE_VALUE.equals(action)) {
            final String url = args.getString(0);
            final String cookieName = args.getString(1);
            final String cookieValue = args.getString(2);
	    final String cookieDomain = args.getString(3);
	    final String cookiePath = args.getString(4);

	    long oneHourFromNow = System.currentTimeMillis() + (60 * 60 * 1000); // 1 hour in milliseconds
	    SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
	    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	    String expiresDate = sdf.format(new Date(oneHourFromNow));
		
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        HttpCookie cookie = new HttpCookie(cookieName, cookieValue);
			cookie.setPath(cookiePath);
			cookie.setDomain(cookieDomain);  // Ensure this is the correct domain
			cookie.setSecure(true);  // If using HTTPS
			cookie.setHttpOnly(true);
			
			String cookieString = cookie.getName() + "=" + cookie.getValue() + "; path=" + cookie.getPath() + "; domain=" + cookie.getDomain() + "; Secure; HttpOnly; Expires=" + expiresDate;
			
			CookieManager cookieManager = CookieManager.getInstance();

			// Enable cookies for WebView
		        WebView webView = new WebView(this);
		        WebSettings webSettings = webView.getSettings();
		        webSettings.setJavaScriptEnabled(true);
		    
		        CookieManager cookieManager = CookieManager.getInstance();
		        cookieManager.setAcceptCookie(true);
		        cookieManager.setAcceptThirdPartyCookies(webView, true); // For Android 5.0+
			    
			cookieManager.setCookie(url, cookieString);
			// Ensure cookies are written to storage
		        cookieManager.flush();

                        PluginResult res = new PluginResult(PluginResult.Status.OK, "Successfully added cookie");
                        callbackContext.sendPluginResult(res);
                    } catch (Exception e) {
                        Log.e(TAG, "Exception: " + e.getMessage());
                        callbackContext.error(e.getMessage());
                    }
                }
            });
            return true;
        }

        else if (ACTION_CLEAR_COOKIES.equals(action)) {

            CookieManager cookieManager = CookieManager.getInstance();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
			    cookieManager.removeAllCookie();
			    cookieManager.flush();
			} else
			{
			    cookieManager.removeAllCookie();
			    cookieManager.removeSessionCookie();
			}

			callbackContext.success();
            return true;
        }

        callbackContext.error("Invalid action");
        return false;

    }
}
