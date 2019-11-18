package no.nav.familie.prosessering;

import no.nav.familie.ks.kontrakter.sak.Ressurs;
import no.nav.familie.prosessering.domene.Status;
import no.nav.familie.prosessering.domene.TaskRepository;
import no.nav.familie.prosessering.rest.RestTaskService;
import no.nav.security.token.support.core.context.TokenValidationContext;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableJpaRepositories({"no.nav.familie"})
@EntityScan({"no.nav.familie"})
@ComponentScan({"no.nav.familie"})
public class TestAppConfig {

    @Bean
    RestTaskService restTaskService(TaskRepository taskRepository){
        return new RestTaskService(taskRepository) {
            @NotNull
            @Override
            public Ressurs hentTasks(
                @NotNull Status status,
                @NotNull String saksbehandlerId) {
                return Ressurs.Companion.ikkeTilgang("dfg");
            }
        };
    }

    @Bean
    TokenValidationContextHolder tokenValidationContextHolder() {
        return new TokenValidationContextHolder() {
            @Override
            public TokenValidationContext getTokenValidationContext() {
                return null;
            }

            @Override
            public void setTokenValidationContext(TokenValidationContext tokenValidationContext) {

            }
        };
    }
}
