package com.example.datban;

import com.example.datban.service.DeliveryJob;
import com.example.datban.service.DeliveryWorker;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeliveryJobConditionTests {
    private final ApplicationContextRunner context=new ApplicationContextRunner()
        .withBean(DeliveryWorker.class,()->mock(DeliveryWorker.class)).withUserConfiguration(DeliveryJob.class);
    @Test void firebaseOffNeverSchedulesDelivery() {
        context.withPropertyValues("app.firebase.enabled=false","app.notification.delivery-enabled=true")
            .run(c->assertThat(c).doesNotHaveBean(DeliveryJob.class));
    }
    @Test void restoreRehearsalCanDisableDeliveryWithoutDisablingFirebaseAuth() {
        context.withPropertyValues("app.firebase.enabled=true","app.notification.delivery-enabled=false")
            .run(c->assertThat(c).doesNotHaveBean(DeliveryJob.class));
    }
    @Test void normalFirebaseDeploymentRetainsDeliveryByDefault() {
        context.withPropertyValues("app.firebase.enabled=true").run(c->assertThat(c).hasSingleBean(DeliveryJob.class));
    }
}
