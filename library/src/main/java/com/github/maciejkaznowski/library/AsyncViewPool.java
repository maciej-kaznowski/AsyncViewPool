package com.github.maciejkaznowski.library;

import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

@SuppressWarnings({"WeakerAccess", "unused"})
public class AsyncViewPool implements AsyncLayoutInflater.OnInflateFinishedListener, ComponentCallbacks2 {

    private static final String TAG = "AsyncViewPool";
    @NonNull
    private final SparseArray<Queue<View>> asyncInflatedViews = new SparseArray<>();
    @NonNull
    private final Context context;
    @NonNull
    private final AsyncLayoutInflater asyncLayoutInflater;
    @Nullable
    private final ViewGroup parent;

    private boolean debug;
    private boolean destroyed = false;

    @SuppressWarnings("unused")
    public AsyncViewPool(@NonNull Context context) {
        this(context, null, false);
    }

    public AsyncViewPool(@NonNull ViewGroup parent) {
        this(parent.getContext(), parent, false);
    }

    public AsyncViewPool(@NonNull Context context,
                         @Nullable ViewGroup parent,
                         boolean debug) {
        //noinspection ConstantConditions
        if (context == null) throw new NullPointerException("Context cannot be null");

        this.context = context;
        this.parent = parent;
        this.debug = debug;
        this.asyncLayoutInflater = new AsyncLayoutInflater(context);

        registerComponent();
    }

    @NonNull
    private static Application getApp(@NonNull Context context) {
        return context instanceof Application ? ((Application) context) : getApp(context.getApplicationContext());
    }

    @NonNull
    private static String describeTrimLevel(int level) {
        switch (level) {
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
                return "TRIM_MEMORY_RUNNING_MODERATE";
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
                return "TRIM_MEMORY_RUNNING_LOW";
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
                return "TRIM_MEMORY_RUNNING_CRITICAL";
            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
                return "TRIM_MEMORY_UI_HIDDEN";
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
                return "TRIM_MEMORY_BACKGROUND";
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
                return "TRIM_MEMORY_MODERATE";
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
                return "TRIM_MEMORY_COMPLETE";

            default:
                Log.w(TAG, String.format("Unknown trim level %s", level));
                return "UNKNOWN: " + level;
        }

    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void destroy() {
        if (destroyed) return;
        this.destroyed = true;

        if (debug) {
            Log.v(TAG, "Destroying");
        }

        unregisterComponent();
        clearInflatedViews();
    }

    @Nullable
    public View getView(@LayoutRes int layout) {
        Queue<View> asyncPool = asyncInflatedViews.get(layout);
        if (asyncPool != null && !asyncPool.isEmpty()) {
            View view = asyncPool.poll();

            if (debug) {
                String msg = String.format("Got inflated layout %s from async pool, %s remaining", ViewUtils.getLayoutHexString(layout), asyncPool.size());
                Log.v(TAG, msg);
            }

            return view;
        }

        if (debug) {
            String msg = String.format("Haven't requested to inflate any views yet for layout=%s, so cannot provide View from the AsyncPool", ViewUtils.getLayoutHexString(layout));
            Log.v(TAG, msg);
        }

        return null;
    }

    @NonNull
    public View getViewOrInflate(@LayoutRes int layout) {
        View fromViewPool = getView(layout);
        if (fromViewPool != null) return fromViewPool;

        if (debug) {
            String msg = String.format("Preinflated View for layout=%s doesn't exist, inflating on MainThread", ViewUtils.getLayoutHexString(layout));
        }

        return LayoutInflater.from(context).inflate(layout, parent, false);
    }


    public void inflateAll(@NonNull Collection<Integer> layouts) {
        for (Integer layout : layouts) inflate(layout);
    }

    public void inflateAll(@NonNull Map<Integer, Integer> layoutsToCount) {
        for (Integer layout : layoutsToCount.keySet()) {
            int count = Objects.requireNonNull(layoutsToCount.get(layout));
            inflate(layout, count);
        }
    }

    public void inflate(@LayoutRes int layout, int count) {
        inflate(layout, parent, count);
    }

    public void inflate(@LayoutRes int layout) {
        inflate(layout, parent);
    }

    public void inflate(@LayoutRes int layout, @Nullable ViewGroup parent) {
        inflate(layout, parent, 1);
    }

    public void inflate(@LayoutRes int layout, @Nullable ViewGroup viewGroup, int count) {
        if (destroyed) {
            if (debug) {
                String msg = String.format("Cannot inflate %s: AsyncViewPool has been destroyed", ViewUtils.getLayoutHexString(layout));
                Log.v(TAG, msg);
            }

            return;
        }


        if (debug) {
            String msg = String.format("Requested to inflate %s of layout %s", count, ViewUtils.getLayoutHexString(layout));
            Log.v(TAG, msg);
        }

        for (int i = 0; i < count; i++) asyncLayoutInflater.inflate(layout, viewGroup, this);
    }

    private void registerComponent() {
        getApp(context).registerComponentCallbacks(this);
    }

    private void unregisterComponent() {
        getApp(context).unregisterComponentCallbacks(this);
    }

    public void clearInflatedViews() {
        if (debug) {
            String msg = String.format("Clearing %s inflated views", asyncInflatedViews.size());
            Log.v(TAG, msg);
        }
        asyncInflatedViews.clear();
    }


    @Override
    public void onInflateFinished(@NonNull View view, @LayoutRes int layout, @Nullable ViewGroup viewGroup) {
        if (destroyed) {
            if (debug) {
                String msg = String.format("Inflated %s, but the pool has been destroyed. Not storing", ViewUtils.getLayoutHexString(layout));
                Log.v(TAG, msg);
            }

            return;
        }

        Queue<View> views = asyncInflatedViews.get(layout);
        if (views == null) views = new LinkedList<>();

        views.add(view);

        asyncInflatedViews.put(layout, views);

        if (debug) {
            String msg = String.format("Inflated %s, current async available count is %sd", ViewUtils.getLayoutHexString(layout), views.size());
            Log.v(TAG, msg);
        }
    }

    @Override
    public void onTrimMemory(int level) {
        if (debug) {
            String msg = String.format("onTrimMemory: level=%s", describeTrimLevel(level));
            Log.v(TAG, msg);
        }

        switch (level) {
            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
                /*
                   Release any UI objects that currently hold memory.

                   The user interface has moved to the background.
                */
                if (debug) {
                    Log.v(TAG, "Received TRIM_MEMORY_UI_HIDDEN - app is in background, clearing cached views");
                }
                clearInflatedViews();
                break;

            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:

                /*
                   Release any memory that your app doesn't need to run.

                   The device is running low on memory while the app is running.
                   The event raised indicates the severity of the memory-related event.
                   If the event is TRIM_MEMORY_RUNNING_CRITICAL, then the system will
                   begin killing background processes.
                */
                if (debug) {
                    Log.v(TAG, "Device is low on memory, and app is in foreground, clearing cached views");
                }
                clearInflatedViews();
                break;

            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
                /*
                   Release as much memory as the process can.

                   The app is on the LRU list and the system is running low on memory.
                   The event raised indicates where the app sits within the LRU list.
                   If the event is TRIM_MEMORY_COMPLETE, the process will be one of
                   the first to be terminated.
                */
                if (debug) {
                    Log.v(TAG, "Device is low on memory, clearing cached views");
                }
                clearInflatedViews();
                break;

            default:
                /*
                  Release any non-critical data structures.

                  The app received an unrecognized memory level value
                  from the system. Treat this as a generic low-memory message.
                */
                if (debug) {
                    String msg = String.format("Received unrecognized memory level=%s. Not releasing resources", level);
                    Log.v(TAG, msg);
                }
                break;
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        if (debug) {
            Log.v(TAG, "Received configuration change, clearing views");
        }
        /*
        Does not stop any views which are currently being inflated from being added to the pool.

        Create a separate listener which will hold the current Configuration.
        Then, once a view is inflated, compare the configurations to determine if we should keep the newly inflated view
        */
        clearInflatedViews();
    }

    @Override
    public void onLowMemory() {
        if (debug) {
            Log.v(TAG, "onLowMemory() called, clearing views");
        }
        clearInflatedViews();
    }
}