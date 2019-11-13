package no.nav.familie.prosessering;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableJpaRepositories({"no.nav.familie"})
@EntityScan({"no.nav.familie"})
@ComponentScan({"no.nav.familie"})
public class TestAppConfig {
}
