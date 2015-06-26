package com.tca.mobiledooraccess.service;

import android.os.Message;
import android.util.Log;

public class MessageExchangeService extends MessageLooperService {
    public static final String TAG = "MessageExchangeService";

    public MessageExchangeService() {
        // Define custom message handler
        super(IncomingMsgHandler.class);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}

/**
 * State machine
 */
class IncomingMsgHandler extends MsgHandler {

    public IncomingMsgHandler() {
        super();
    }

    @Override
    public IncomingMsgHandler newInstance() {
        return new IncomingMsgHandler();
    }

    @Override
    public void handleMessage(Message msg) {
        // process incoming messages here
        Log.d(MessageExchangeService.TAG, "B LooperThread is: " + Thread.currentThread().getName());
        Log.d(MessageExchangeService.TAG, "Got message with arg1 " + msg.arg1);
    }
}
