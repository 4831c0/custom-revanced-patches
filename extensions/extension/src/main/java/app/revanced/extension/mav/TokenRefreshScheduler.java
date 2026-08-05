package app.revanced.extension.mav;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * Schedules a persisted daily job to refresh the session token when still valid.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public final class TokenRefreshScheduler {

    /** Must be stable across app versions for this patch. */
    public static final int JOB_ID = 0x4D415654; // "MAVT"

    private static final long PERIOD_MS = 24L * 60L * 60L * 1000L;

    private TokenRefreshScheduler() {
    }

    public static void schedule(Context context) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        ComponentName component = new ComponentName(appContext, TokenRefreshJobService.class);
        if (!isComponentDeclared(appContext, component)) {
            // Some patching setups may not merge extension manifest components.
            // Never crash app startup in that case.
            return;
        }

        JobScheduler scheduler = (JobScheduler) appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler == null) {
            return;
        }
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, component)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setPeriodic(PERIOD_MS, PERIOD_MS);
        } else {
            builder.setPeriodic(PERIOD_MS);
        }

        try {
            scheduler.schedule(builder.build());
        } catch (IllegalArgumentException ignored) {
            // Defensive: do not crash process if framework rejects unknown component.
        }
    }

    private static boolean isComponentDeclared(Context context, ComponentName component) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.getPackageManager().getServiceInfo(
                        component,
                        PackageManager.ComponentInfoFlags.of(0)
                );
            } else {
                context.getPackageManager().getServiceInfo(component, 0);
            }
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }
}
