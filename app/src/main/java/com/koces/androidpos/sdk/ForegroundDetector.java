package com.koces.androidpos.sdk;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ForegroundDetector implements Application.ActivityLifecycleCallbacks {

    enum State {
        None, Foreground, Background
    }
    private State state;
    private static ForegroundDetector Instance = null;
    private Application application = null;
    private Listener listener = null;
    private boolean isChangingConfigurations = false;
    private int running = 0;

    public interface Listener {
        void onBecameForeground();
        void onBecameBackground();
    }

    public static ForegroundDetector getInstance() {
        return Instance;
    }

    public ForegroundDetector(Application application) {
        Instance = this;
        this.application = application;
        application.registerActivityLifecycleCallbacks(this);
    }

    public void unregisterCallbacks() {
        this.application.unregisterActivityLifecycleCallbacks(this);
    }

    public void addListener(Listener listener) {
        state = State.None;
        this.listener = listener;
    }

    public void removeListener() {
        state = State.None;
    }

    public boolean isBackground() {
        return state == State.Background;
    }

    public boolean isForeground() {
        return state == State.Foreground;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (++running == 1 && !isChangingConfigurations) {
            state = State.Foreground;
            if (listener != null)
                listener.onBecameForeground();
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        isChangingConfigurations = activity.isChangingConfigurations();
        if (--running == 0 && !isChangingConfigurations) {
            state = State.Background;
            if (listener != null)
                listener.onBecameBackground();
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }
}