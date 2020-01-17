package no.nav.familie.prosessering

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.rest.RestTask
import no.nav.familie.prosessering.rest.RestTaskMapper
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootConfiguration
@EnableJpaRepositories("no.nav.familie")
@EntityScan("no.nav.familie")
@ComponentScan("no.nav.familie")
class TestAppConfig {

    @Bean
    fun restTaskMapper(): RestTaskMapper {
        return object : RestTaskMapper {
            override fun toDto(task: Task): RestTask {
                return RestTask(task, linkedMapOf("" to ""))
            }
        }
    }

    @Bean
    fun tokenValidationContextHolder(): TokenValidationContextHolder {
        return object : TokenValidationContextHolder {
            override fun getTokenValidationContext(): TokenValidationContext {
                return TokenValidationContext(emptyMap())
            }

            override fun setTokenValidationContext(tokenValidationContext: TokenValidationContext) {}
        }
    }
}
