package app.revanced.extension.mav;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reloads the active user from SQLite and calls BejelentkezesHosszabbitas when the
 * stored session token is still valid (no stock 24h-before-expiry gate).
 *
 * <p>Targets MÁV Vonatinfo 4.12 obfuscated types (k8.g0, xb.d0, etc.).
 */
public final class TokenRefreshHelper {
    private static final String TAG = "MavTokenRefresh";
    private static final AtomicBoolean pendingRefresh = new AtomicBoolean(false);

    private TokenRefreshHelper() {
    }

    /**
     * Hooked from {@code communication.request.c.k(Object)} after the server response is parsed.
     */
    public static void onLoginExtensionResponse(Object response) {
        if (!pendingRefresh.compareAndSet(true, false)) {
            return;
        }
        try {
            if (isSuccessfulLoginResponse(response)) {
                Log.i(TAG, "run:success");
            } else {
                Log.i(TAG, "run:refresh_failed");
            }
        } catch (Throwable ignored) {
            Log.i(TAG, "run:refresh_failed");
        }
    }

    public static void refreshIfNeeded() {
        try {
            Log.i(TAG, "run:start");
            if (!hasNetwork()) {
                Log.i(TAG, "run:skip_no_network");
                return;
            }

            Object user = loadActiveUser();
            if (user == null) {
                Log.i(TAG, "run:skip_no_active_user");
                return;
            }

            if (!hydrateSession(user)) {
                Log.i(TAG, "run:skip_hydrate_failed");
                return;
            }

            if (!isSessionValid()) {
                Log.i(TAG, "run:skip_session_invalid");
                return;
            }

            fireLoginExtension();
            Log.i(TAG, "run:refresh_request_sent");
        } catch (Throwable ignored) {
        }
    }

    private static boolean hasNetwork() throws Exception {
        Class<?> httpManager = Class.forName("u6.c");
        Method h = httpManager.getDeclaredMethod("h");
        Object result = h.invoke(null);
        return result instanceof Boolean && (Boolean) result;
    }

    private static Object loadActiveUser() throws Exception {
        Class<?> dao = Class.forName("xb.d0");
        Method y = dao.getDeclaredMethod("Y");
        return y.invoke(null);
    }

    private static boolean hydrateSession(Object user) throws Exception {
        Method a = user.getClass().getDeclaredMethod("a");
        Object result = a.invoke(user);
        return result instanceof Boolean && (Boolean) result;
    }

    private static boolean isSessionValid() throws Exception {
        Class<?> loginManager = Class.forName("k8.g0");
        Method e = loginManager.getDeclaredMethod("e");
        Object result = e.invoke(null);
        return result instanceof Boolean && (Boolean) result;
    }

    private static void fireLoginExtension() throws Exception {
        Class<?> vonatInfo = Class.forName("hu.mavszk.vonatinfo2.VonatInfo");
        Method t = vonatInfo.getDeclaredMethod("t");
        Method k = vonatInfo.getDeclaredMethod("k");
        String username = (String) t.invoke(null);
        String langCode = (String) k.invoke(null);
        if (isNullOrEmpty(langCode)) {
            langCode = readLanguagePref();
        }

        Class<?> loginManager = Class.forName("k8.g0");
        Method getToken = loginManager.getDeclaredMethod("a");
        String token = (String) getToken.invoke(null);

        Class<?> uaidHelper = Class.forName("k8.m1");
        Method uaid = uaidHelper.getDeclaredMethod("b");
        String deviceId = (String) uaid.invoke(null);

        Class<?> requestVo = Class.forName("hu.mavszk.vonatinfo2.model.d");
        Object dVar = requestVo.getDeclaredConstructor().newInstance();
        requestVo.getDeclaredMethod("e", String.class).invoke(dVar, username);
        requestVo.getDeclaredMethod("f", String.class).invoke(dVar, langCode);
        requestVo.getDeclaredMethod("g", String.class).invoke(dVar, token);
        requestVo.getDeclaredMethod("h", String.class).invoke(dVar, deviceId);

        Class<?> requestClass = Class.forName("hu.mavszk.vonatinfo2.communication.request.c");
        Object request = requestClass.getDeclaredConstructor(requestVo).newInstance(dVar);

        pendingRefresh.set(true);

        Class<?> httpManager = Class.forName("u6.c");
        Method g = httpManager.getDeclaredMethod("g");
        Object manager = g.invoke(null);
        Method e = httpManager.getDeclaredMethod("e", Class.forName("u6.a"), String.class);
        e.invoke(manager, request, null);
    }

    private static boolean isSuccessfulLoginResponse(Object response) throws Exception {
        if (response == null) {
            return false;
        }
        Class<?> loginResponseClass = Class.forName("hu.mavszk.vonatinfo2.model.v1");
        if (!loginResponseClass.isInstance(response)) {
            return false;
        }
        Method isValid = loginResponseClass.getDeclaredMethod("g");
        Object result = isValid.invoke(response);
        return result instanceof Boolean && (Boolean) result;
    }

    private static String readLanguagePref() throws Exception {
        Class<?> vonatInfo = Class.forName("hu.mavszk.vonatinfo2.VonatInfo");
        Method getContext = vonatInfo.getDeclaredMethod("e");
        Context context = (Context) getContext.invoke(null);
        if (context == null) {
            return "HU";
        }

        Class<?> prefsKeys = Class.forName("k8.o1");
        java.lang.reflect.Field prefsName = prefsKeys.getDeclaredField("a");
        java.lang.reflect.Field langKey = prefsKeys.getDeclaredField("f");
        String prefsFile = (String) prefsName.get(null);
        String langField = (String) langKey.get(null);

        SharedPreferences prefs = context.getSharedPreferences(prefsFile, Context.MODE_PRIVATE);
        String lang = prefs.getString(langField, null);
        return !isNullOrEmpty(lang) ? lang : "HU";
    }

    private static boolean isNullOrEmpty(String value) {
        return value == null || value.length() == 0;
    }
}
