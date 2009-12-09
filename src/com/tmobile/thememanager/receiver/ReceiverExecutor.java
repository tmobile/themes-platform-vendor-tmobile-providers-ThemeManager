package com.tmobile.thememanager.receiver;

import com.tmobile.thememanager.ThemeManager;

import android.os.Process;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Centralize all broadcast receiver events under a single ThreadPoolExecutor.
 * This executor is shared between components of both ThemeManager and
 * ProfileManager.
 * <p>
 * This thread pool is optimized for a single thread to operate on scheduled
 * work in series, though it can grow if heavily loaded. Jobs must never be
 * dependent on each other, or depend on a specific ordering.
 */
public class ReceiverExecutor {
    private static final String TAG = "ReceiverExecutor";

    private static final int CORE_POOL_SIZE = 1;
    private static final int MAXIMUM_POOL_SIZE = 3;
    private static final int KEEP_ALIVE_TIME = 10;

    private static final BlockingQueue<Runnable> sWorkQueue =
        new LinkedBlockingQueue<Runnable>(10);

    private static final ThreadPoolExecutor sExecutor =
        new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME,
                TimeUnit.SECONDS, sWorkQueue);

    /**
     * Call through to the underlying ThreadPoolExecutor to schedule a new
     * command. If {@link ThemeManager#DEBUG} is true, wall timings will be
     * inserted to measure job performance.
     */
    public static void execute(String commandName, Runnable command) {
        sExecutor.execute(new WrappedRunnable(commandName, command));
    }

    /**
     * @see #execute(String, Runnable)
     */
    public static void execute(Runnable command) {
        execute(null, command);
    }

    private static class WrappedRunnable implements Runnable {
        private final String mCommandName;
        private final Runnable mRunnable;

        public WrappedRunnable(String commandName, Runnable runnable) {
            mCommandName = commandName;
            mRunnable = runnable;
        }

        public void run() {
            /* Set background priority to keep the UI responsive. */
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            long startTime = 0;
            if (ThemeManager.DEBUG && mCommandName != null) {
                startTime = System.currentTimeMillis();
            }
            try {
                mRunnable.run();
            } finally {
                if (ThemeManager.DEBUG && mCommandName != null) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    Log.d(TAG, mCommandName + " took " + elapsed + " ms");
                }
            }
        }
    }
}
