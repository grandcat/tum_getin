package com.tca.mobiledooraccess.service;

public final class MessageExchangeService extends BaseMessageLooperService {
    public static final String TAG = "MessageExchangeService";

    public MessageExchangeService() {
        // Define our custom message handler
        super(StatefulProtocolHandler.class);
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
