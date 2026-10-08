package com.example.datban;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:h2:mem:management_port;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "management.server.port=0","management.endpoints.web.exposure.include=health,prometheus",
    "app.security.require-auth=true","app.monitoring.token=isolated-monitoring-token-32-characters"})
@ActiveProfiles("test")
@Import(OperationsTests.Fakes.class)
@org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
class ManagementPortTests {
    @LocalServerPort int businessPort;
    @Value("${local.management.port}") int managementPort;
    @Autowired TestRestTemplate http;
    @Test void readinessServedOnSeparateManagementPort() {
        assertThat(managementPort).isNotEqualTo(businessPort);
        var response=http.getForEntity("http://127.0.0.1:"+managementPort+"/actuator/health/readiness",String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP").doesNotContain("components");
    }
    @Test void metricsCredentialIsEnforcedOnActualManagementHttpListener() {
        String url="http://127.0.0.1:"+managementPort+"/actuator/prometheus";
        assertThat(http.getForEntity(url,String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        var headers=new HttpHeaders();headers.setBearerAuth("isolated-monitoring-token-32-characters");
        assertThat(http.exchange(url,HttpMethod.GET,new HttpEntity<>(headers),String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
