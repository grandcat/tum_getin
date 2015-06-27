package com.tca.mobiledooraccess.service;

import android.os.Message;
import android.util.Log;

import java.io.UnsupportedEncodingException;

public final class MessageExchangeService extends BaseMessageLooperService {
    public static final String TAG = "MessageExchangeService";

    public MessageExchangeService() {
        // Define our custom message handler
        super(StmProtocolHandler.class);
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
