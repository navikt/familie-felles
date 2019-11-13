package no.nav.familie.prosessering.internal;

import no.nav.familie.prosessering.TestAppConfig;
import no.nav.familie.prosessering.domene.Status;
import no.nav.familie.prosessering.domene.Task;
import no.nav.familie.prosessering.domene.TaskRepository;
import no.nav.familie.prosessering.task.Task1;
import no.nav.familie.prosessering.task.Task2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@RunWith(SpringRunner.class)
@ContextConfiguration(
    classes = {TestAppConfig.class})
@DataJpaTest(excludeAutoConfiguration = {FlywayAutoConfiguration.class})
public class TaskWorkerTest {


    @MockBean
    private Task2 task;
    @Autowired
    private TaskRepository repository;

    @Autowired
    private TaskWorker worker;

    @Test
    public void skal_behandle_task() {
        var task1 = Task.nyTask(Task1.TASK_1, "{'a'='b'}");
        repository.saveAndFlush(task1);
        assertThat(task1.getStatus()).isEqualTo(Status.UBEHANDLET);

        worker.doActualWork(task1.getId());

        task1 = repository.findById(task1.getId()).orElseThrow();
        assertThat(task1.getStatus()).isEqualTo(Status.FERDIG);
        assertThat(task1.getLogg()).hasSize(3);
    }

    @Test
    public void skal_håndtere_feil() {
        var task2 = Task.nyTask(Task2.TASK_2, "{'a'='b'}");
        repository.saveAndFlush(task2);
        assertThat(task2.getStatus()).isEqualTo(Status.UBEHANDLET);
        doThrow(new IllegalStateException()).when(task).doTask(any());

        worker.doActualWork(task2.getId());

        task2 = repository.findById(task2.getId()).orElseThrow();
        assertThat(task2.getStatus()).isEqualTo(Status.KLAR_TIL_PLUKK);
        assertThat(task2.getLogg()).hasSize(3);

        worker.doActualWork(task2.getId());

        task2 = repository.findById(task2.getId()).orElseThrow();
        assertThat(task2.getStatus()).isEqualTo(Status.KLAR_TIL_PLUKK);

        worker.doActualWork(task2.getId());

        task2 = repository.findById(task2.getId()).orElseThrow();
        assertThat(task2.getStatus()).isEqualTo(Status.FEILET);
    }

}
