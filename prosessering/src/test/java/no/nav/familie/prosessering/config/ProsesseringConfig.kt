package no.nav.familie.prosessering.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration @EnableAsync @EnableScheduling
class ProsesseringConfig {

    @Bean(name = ["taskExecutor"])
    fun threadPoolTaskExecutor(): TaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 4
        executor.maxPoolSize = 4
        executor.threadNamePrefix = "TaskWorker-"
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setQueueCapacity(20)
        executor.initialize()
        return executor
    }
}
