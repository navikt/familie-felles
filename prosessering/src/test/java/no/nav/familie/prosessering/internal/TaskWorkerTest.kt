package no.nav.familie.prosessering.internal

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.familie.prosessering.TestAppConfig
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task.Companion.nyTask
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.task.TaskStep1
import no.nav.familie.prosessering.task.TaskStep2
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.context.transaction.TestTransaction

@RunWith(SpringRunner::class)
@ContextConfiguration(classes = [TestAppConfig::class])
@DataJpaTest(excludeAutoConfiguration = [FlywayAutoConfiguration::class])
class TaskWorkerTest {



    @Autowired
    private lateinit var repository: TaskRepository

    @Autowired
    private lateinit var worker: TaskWorker

    @Test
    fun `skal behandle task`() {
        val task1 = nyTask(TaskStep1.TASK_1, "{'a'='b'}").plukker()
        repository.save(task1)
        assertThat(task1.status).isEqualTo(Status.PLUKKET)
        TestTransaction.flagForCommit()
        TestTransaction.end()
        worker.doActualWork(task1.id!!)
//        TestTransaction.start()
        val findByIdOrNull = repository.findByIdOrNull(task1.id)
        assertThat(findByIdOrNull?.status).isEqualTo(Status.FERDIG)
        assertThat(findByIdOrNull?.logg).hasSize(4)
    }



}
