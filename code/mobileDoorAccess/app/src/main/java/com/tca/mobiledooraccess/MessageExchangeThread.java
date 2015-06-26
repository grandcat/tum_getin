package com.tca.mobiledooraccess;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

/**
 * Created by Stefan on 25.06.2015.
 */
public class MessageExchangeThread extends Thread {
    private static final String TAG = "LooperThread";

    private Looper mLooper;
    public Handler mHandler;

    private Messenger mReturnChannel = null;

    /**
     * Returns looper reference. Used to shutdown endless loop.
     * @return looper reference
     */
    public Looper getLooper() {
        return mLooper;
    }

    public Handler getHandler() {
        synchronized (this) {
            while (mHandler == null){
                try {
                    wait();
                } catch(InterruptedException e) {}
            }
            return mHandler;
        }
    }

    public void run() {
        Looper.prepare();
        synchronized (this) {
            // The handler will bind to the looper of the current thread
            mHandler = new MsgHandler();
            mLooper = Looper.myLooper();
            notifyAll();
        }
        Looper.loop();

        Log.d(TAG, "Shutting down.");
    }

    final class MsgHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            // process incoming messages here
            Log.d(TAG, "LooperThread is: " + Thread.currentThread().getName());
            Log.d(TAG, "Got message with arg1 " + msg.arg1);
            if (mReturnChannel == null) mReturnChannel = msg.replyTo;
        }
    }
}
