package com.example.datban.service;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name="app.firebase.enabled",havingValue="true")
public class DeliveryJob {
    private final DeliveryWorker worker;
    public DeliveryJob(DeliveryWorker worker) { this.worker=worker; }
    @Scheduled(fixedDelayString="${app.notification.delivery-delay-ms:15000}")
    public void run() { worker.runBatch(); }
}
