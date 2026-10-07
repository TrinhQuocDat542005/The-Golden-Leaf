package com.example.datban.service;

import java.util.Map;

public interface PushGateway {
    void send(String token,Map<String,String> data) throws Failure;
    class Failure extends RuntimeException {
        public final String code;
        public final boolean invalidToken;
        public final boolean retryable;
        public Failure(String code,boolean invalidToken,boolean retryable) {
            super(code);this.code=code;this.invalidToken=invalidToken;this.retryable=retryable;
        }
    }
}
