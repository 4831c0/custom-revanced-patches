package app.revanced.extension.mav;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

/**
 * Filters MÁVPlusz / EMMA promotional JE (in-app) messages.
 *
 * <p>Avoids {@code w6.b.i()} / openDBCount (stock {@code JeMessagesHelper.b()} leaks a
 * connection). Opens the SQLite file directly while holding the {@code w6.b} monitor.
 */
public final class JeMessagesFilter {
    private static final String TAG = "MavJeMsgFilter";
    private static final String DB_NAME = "mav_vonat_info";

    private JeMessagesFilter() {
    }

    /**
     * Replacement for {@code JeMessagesHelper.d()}: true if any non-promo JE message remains.
     */
    public static boolean hasNonPromoMessages() {
        try {
            ArrayList<Object> messages = loadMessages(
                    "egyszeri = ? OR megtekintve = ?",
                    new String[]{"0", "0"}
            );
            removeFromList(messages);
            return !messages.isEmpty();
        } catch (Throwable t) {
            Log.w(TAG, "hasNonPromoMessages failed", t);
            return false;
        }
    }

    /**
     * Replacement for {@code JeMessagesHelper.b()}: icon for the toolbar message action.
     * Stock {@code b()} opens via {@code getDatabase} and never closes it, which breaks
     * later opens ({@code openDBCount:1}) such as {@code BaseActivity.onResume}.
     */
    public static int messageIconResId() {
        try {
            Context context = appContext();
            if (context == null) {
                return 0;
            }
            ArrayList<Object> messages = loadMessages("egyszeri = ?", new String[]{"0"});
            removeFromList(messages);
            String name = messages.isEmpty() ? "ic_message" : "ic_message_r";
            return id(context, "drawable", name);
        } catch (Throwable t) {
            Log.w(TAG, "messageIconResId failed", t);
            return 0;
        }
    }

    /**
     * Replacement for {@code JeMessagesHelper.e(Activity)}: show JE sheet without MÁVPlusz promos.
     */
    public static void showFiltered(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        try {
            ArrayList<Object> messages = loadMessages(
                    "egyszeri = ? OR megtekintve = ?",
                    new String[]{"0", "0"}
            );
            removeFromList(messages);
            if (messages.isEmpty()) {
                return;
            }
            showDialog(activity, messages);
        } catch (Throwable t) {
            Log.w(TAG, "showFiltered failed", t);
        }
    }

    /**
     * Drops MÁVPlusz promo entries from a list about to be inserted into {@code jemessages}.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void removeFromList(ArrayList list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        try {
            Iterator it = list.iterator();
            int removed = 0;
            while (it.hasNext()) {
                Object item = it.next();
                if (isMavPluszPromo(item)) {
                    it.remove();
                    removed++;
                }
            }
            if (removed > 0) {
                Log.i(TAG, "removed " + removed + " mavplusz je message(s)");
            }
        } catch (Throwable t) {
            Log.w(TAG, "list filter failed", t);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ArrayList<Object> loadMessages(String selection, String[] selectionArgs)
            throws Exception {
        ArrayList<Object> messages = new ArrayList<>();
        Class<?> helperClass = Class.forName("w6.b");
        synchronized (helperClass) {
            Context context = appContext();
            if (context == null) {
                return messages;
            }
            File dbFile = context.getDatabasePath(DB_NAME);
            if (!dbFile.exists()) {
                return messages;
            }
            SQLiteDatabase db = SQLiteDatabase.openDatabase(
                    dbFile.getAbsolutePath(),
                    null,
                    SQLiteDatabase.OPEN_READONLY
            );
            Cursor cursor = null;
            try {
                cursor = db.query(
                        "jemessages",
                        new String[]{"title", "message", "egyszeri", "megtekintve", "link_nev", "link_url"},
                        selection,
                        selectionArgs,
                        null,
                        null,
                        "message"
                );
                if (cursor == null) {
                    return messages;
                }
                Class<?> voClass = Class.forName("hu.mavszk.vonatinfo2.model.a1");
                while (cursor.moveToNext()) {
                    Object vo = voClass.getDeclaredConstructor().newInstance();
                    invokeVoid(vo, "l", String.class, cursor.getString(0));
                    invokeVoid(vo, "m", String.class, cursor.getString(1));
                    invokeVoid(vo, "h", boolean.class, cursor.getInt(2) == 1);
                    invokeVoid(vo, "k", boolean.class, cursor.getInt(3) == 1);
                    invokeVoid(vo, "i", String.class, cursor.getString(4));
                    invokeVoid(vo, "j", String.class, cursor.getString(5));
                    messages.add(vo);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                db.close();
            }
        }
        return messages;
    }

    private static void showDialog(Activity activity, ArrayList<Object> messages) throws Exception {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(1);
        int layoutId = id(activity, "layout", "je_messages_bottom_sheet_layout");
        dialog.setContentView(layoutId);
        Button close = dialog.findViewById(id(activity, "id", "closeDialog"));
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        ListView listView = dialog.findViewById(id(activity, "id", "jeMessages"));
        Constructor<?> adapterCtor = Class.forName("i7.f")
                .getConstructor(Activity.class, ArrayList.class);
        listView.setAdapter((ListAdapter) adapterCtor.newInstance(activity, messages));
        dialog.setCancelable(false);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(-1, -2);
            window.setBackgroundDrawable(new ColorDrawable(0));
            int anim = id(activity, "style", "DialogAnimation");
            if (anim != 0) {
                window.getAttributes().windowAnimations = anim;
            }
            window.setGravity(17);
        }
    }

    private static Context appContext() throws Exception {
        Object ctx = Class.forName("hu.mavszk.vonatinfo2.VonatInfo").getMethod("e").invoke(null);
        return ctx instanceof Context ? (Context) ctx : null;
    }

    private static int id(Context context, String type, String name) {
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }

    private static void invokeVoid(Object target, String method, Class<?> argType, Object arg)
            throws Exception {
        Method m = target.getClass().getMethod(method, argType);
        m.invoke(target, arg);
    }

    private static boolean isMavPluszPromo(Object message) {
        if (message == null) {
            return false;
        }
        try {
            String title = invokeString(message, "f");
            String body = invokeString(message, "g");
            String linkName = invokeString(message, "c");
            String linkUrl = invokeString(message, "d");
            String haystack = normalize(title) + " " + normalize(body) + " "
                    + normalize(linkName) + " " + normalize(linkUrl);
            return haystack.contains("mavplus")
                    || haystack.contains("mav+")
                    || haystack.contains("emmapp")
                    || haystack.contains("hu.mav.emm");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String invokeString(Object target, String method) throws Exception {
        Method m = target.getClass().getMethod(method);
        Object value = m.invoke(target);
        return value instanceof String ? (String) value : "";
    }

    private static String normalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}
