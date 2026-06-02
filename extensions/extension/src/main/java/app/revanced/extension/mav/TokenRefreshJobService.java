package app.revanced.extension.mav;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;

/**
 * Periodic job that extends the MÁV session when a valid token is stored.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public final class TokenRefreshJobService extends JobService {

    @Override
    public boolean onStartJob(JobParameters params) {
        new Thread(() -> {
            try {
                TokenRefreshHelper.refreshIfNeeded();
            } finally {
                jobFinished(params, false);
            }
        }, "MavTokenRefresh").start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
