package no.nav.familie.prosessering.internal;

import no.nav.familie.prosessering.AsyncTask;
import no.nav.familie.prosessering.TaskBeskrivelse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest(excludeAutoConfiguration = {FlywayAutoConfiguration.class})
public class AsyncTaskTest {

    @Autowired
    private List<AsyncTask> tasker;

    @Test
    public void skal_ha_annotasjon() {
        assertThat(tasker.stream().anyMatch(this::harIkkePåkrevdAnnotasjon)).isFalse();
    }

    private boolean harIkkePåkrevdAnnotasjon(AsyncTask it) {
        return !AnnotationUtils.isAnnotationDeclaredLocally(TaskBeskrivelse.class, it.getClass());
    }

    @Test
    public void skal_ha_unike_nav() {
        final var taskTyper = tasker.stream().map(this::finnAnnotasjon).map(TaskBeskrivelse::taskType).collect(Collectors.toList());

        assertThat(taskTyper).isEqualTo(taskTyper.stream().distinct().collect(Collectors.toList()));
    }

    private TaskBeskrivelse finnAnnotasjon(AsyncTask task) {
        final Class<?> aClass = AopProxyUtils.ultimateTargetClass(task);
        return AnnotationUtils.findAnnotation(aClass, TaskBeskrivelse.class);
    }
}
