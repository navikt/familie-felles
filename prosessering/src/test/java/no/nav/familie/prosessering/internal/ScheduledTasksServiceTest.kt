package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.TestAppConfig
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@ContextConfiguration(classes = [TestAppConfig::class])
@DataJpaTest(excludeAutoConfiguration = [FlywayAutoConfiguration::class])
class ScheduledTasksServiceTest {


    @Autowired
    private lateinit var scheduledTasksService: ScheduledTaskService

    @Autowired
    private lateinit var tasksRepository: TaskRepository

    @Test
    @Sql("classpath:sql-testdata/gamle_tasker_med_logg.sql")
    @DirtiesContext
    fun `skal slette gamle tasker med status FERDIG`() {
        scheduledTasksService.slettTasksKlarForSletting()
        tasksRepository.flush()

        assertThat(tasksRepository.findAll())
                .hasSize(1)
                .extracting("status").containsOnly(Status.KLAR_TIL_PLUKK)
    }

    @Test
    @DirtiesContext
    fun `skal ikke slette nye tasker`() {
        val nyTask = Task.nyTask("type", "payload")
        nyTask.ferdigstill()
        tasksRepository.saveAndFlush(nyTask)


        scheduledTasksService.slettTasksKlarForSletting()

        val alleTasker = tasksRepository.findAll()
        assertThat(tasksRepository.findAll())
                .filteredOn("id", nyTask.id)
                .isNotEmpty
    }
}
