package com.hs.firstopengl;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import java.util.concurrent.atomic.AtomicBoolean;

public class LifeGLSurfaceView extends GLSurfaceView implements LifecycleObserver {
    private AtomicBoolean isRenderSet = new AtomicBoolean(false);

    public LifeGLSurfaceView(Context context) {
        this(context, null);
    }

    public LifeGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void bindLifecycle(@NonNull LifecycleOwner lifecycleOwner) {
        lifecycleOwner.getLifecycle().addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onViewPause() {
        if (isRenderSet.get()) {
            super.onPause();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onViewResume() {
        if (isRenderSet.get()) {
            super.onResume();
        }
    }

    @Override
    public void setRenderer(Renderer renderer) {
        setEGLContextClientVersion(2);
        super.setRenderer(renderer);
        isRenderSet.set(true);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }
}
