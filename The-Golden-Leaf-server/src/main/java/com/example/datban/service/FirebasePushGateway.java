package com.example.datban.service;

import com.google.firebase.messaging.*;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class FirebasePushGateway implements PushGateway {
    public void send(String token,Map<String,String> data) {
        try {
            FirebaseMessaging.getInstance().send(Message.builder().setToken(token).putAllData(data)
                    .setAndroidConfig(AndroidConfig.builder().setTtl(3600000).build()).build());
        } catch (FirebaseMessagingException ex) {
            var code=ex.getMessagingErrorCode();
            boolean invalid=code==MessagingErrorCode.UNREGISTERED;
            boolean retry=code==MessagingErrorCode.UNAVAILABLE || code==MessagingErrorCode.INTERNAL || code==MessagingErrorCode.QUOTA_EXCEEDED;
            throw new Failure(code==null ? "FCM_ERROR" : code.name(),invalid,retry);
        } catch (IllegalStateException ex) { throw new Failure("FCM_NOT_CONFIGURED",false,true); }
    }
}
