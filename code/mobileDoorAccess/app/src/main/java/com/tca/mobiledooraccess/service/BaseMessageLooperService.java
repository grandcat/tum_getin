package com.tca.mobiledooraccess.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

/**
 * Created by Stefan on 26.06.2015.
 */
public class BaseMessageLooperService extends Service {
    private static final String TAG = "LooperServiceThread";
    /**
     * Custom message handler for processing incoming messages
     */
    private Class<BaseMsgHandler> msgHandlerClass;
    private boolean shutdownInvoked = false;

    protected LooperThread mLooperThread;
    protected Looper mServiceLooper;
    protected Handler mServiceHandler;
    // Target we publish for clients to send messages to IncomingMsgHandler.
    protected Messenger mMessenger;

    public BaseMessageLooperService(Class msgHandlerClass) {
        this.msgHandlerClass = msgHandlerClass;
    }

    @Override
    public void onCreate() {
        // Assign the application context of this service
        // Lifetime should be as long as the service is active, so passing to
        // the protocol handler should be ok.
        Log.i(TAG, "Service Application context is " + getApplicationContext().toString());

        // Create looping thread for asynchronously dispatching messages
        mLooperThread = new LooperThread(getApplicationContext());
        mLooperThread.start();
        Log.i(TAG, "Looper service thread started.");

        mServiceHandler = mLooperThread.getHandler();   // blocks until handler is created
        mServiceLooper = mLooperThread.getLooper();     // Attention: has to be executed after
                                                        // getHandler to be non-null
        // Initialize messenger interface for communication with CardEmulationService
        mMessenger = new Messenger(mLooperThread.getHandler());
    }

    @Override
    public void onDestroy() {
        if (!shutdownInvoked) {
            // Stop looper thread
            mServiceLooper.quit();
            shutdownInvoked = true;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (mMessenger != null)
            return mMessenger.getBinder();

        return null;
    }

    /**
     * Looper thread for asynchronously processing messages
     */
    final class LooperThread extends Thread {
        Context applicationContext;

        private Looper mLooper;
        private Handler mHandler;

        public LooperThread(Context context) {
            this.applicationContext = context;
        }

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
                Log.d(TAG, "Looper: returned handler " + mHandler.toString());
                return mHandler;
            }
        }

        public void run() {
            Looper.prepare();
            synchronized (this) {
                // The handler will bind to the looper of the current thread
                mLooper = Looper.myLooper();
                if (msgHandlerClass != null) {
                    try {
                        mHandler = msgHandlerClass.newInstance();
                        Log.d(TAG, "Using custom message handler " + msgHandlerClass.getName());
                        // Set application context for preferences and raw resource access
                        // within the protocol handler
                        ((BaseMsgHandler)mHandler).setAppContext(applicationContext);

                    } catch (InstantiationException e) {
                        Log.e(TAG, "Could not instantiate passed MsgHandler class.");
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                else {
                    mHandler = new BaseMsgHandler();
                }
                notifyAll();
            }
            Looper.loop();

            Log.d(TAG, "Shutting down.");
        }
    }
}

/**
 * State machine
 */
class BaseMsgHandler extends Handler {
    private static final String TAG = "LooperServiceThread";

    // Context for raw resource access and preferences
    protected Context appContext = null;

    public BaseMsgHandler() {
        super();
    }

    public void setAppContext(Context context) {
        this.appContext = context;
        Log.d(TAG, "Set app context: " + appContext.toString());
    }

    @Override
    public void handleMessage(Message msg) {
        // process incoming messages here
        Log.d(TAG, "LooperThread is: " + Thread.currentThread().getName());
        Log.d(TAG, "Got a message in handleMessage(). For more functionality, " +
                "this class should be derived.");
    }
}
