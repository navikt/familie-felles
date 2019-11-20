package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.AsyncTask
import no.nav.familie.prosessering.TaskBeskrivelse
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.aop.framework.AopProxyUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@DataJpaTest(excludeAutoConfiguration = [FlywayAutoConfiguration::class])
class AsyncTaskTest {

    @Autowired
    private lateinit var tasker: List<AsyncTask>

    @Test
    fun `skal ha annotasjon`() {
        Assertions.assertThat(tasker.any {
            harIkkePåkrevdAnnotasjon(it)
        }).isFalse()
    }

    private fun harIkkePåkrevdAnnotasjon(it: AsyncTask): Boolean {
        return !AnnotationUtils.isAnnotationDeclaredLocally(TaskBeskrivelse::class.java,
                                                            it.javaClass)
    }

    @Test
    fun `skal ha unike navn`() {
        val taskTyper = tasker.map { task: AsyncTask -> finnAnnotasjon(task).tasktype }

        Assertions.assertThat(taskTyper)
                .isEqualTo(taskTyper.distinct())
    }

    private fun finnAnnotasjon(task: AsyncTask): TaskBeskrivelse {
        val aClass = AopProxyUtils.ultimateTargetClass(task)
        return AnnotationUtils.findAnnotation(aClass, TaskBeskrivelse::class.java) ?: error("")
    }
}
