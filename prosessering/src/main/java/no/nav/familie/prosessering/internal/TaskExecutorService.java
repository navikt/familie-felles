package no.nav.familie.prosessering.internal;

import no.nav.familie.prosessering.domene.Task;
import no.nav.familie.prosessering.domene.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskExecutorService {

    public static final int POLLING_DELAY = 30000;
    private static final Logger log = LoggerFactory.getLogger(TaskExecutorService.class);
    private TaskWorker worker;
    private TaskExecutor taskExecutor;
    private TaskRepository taskProsesseringRepository;

    @Autowired
    public TaskExecutorService(TaskWorker worker,
                               @Qualifier("taskExecutor") TaskExecutor taskExecutor,
                               TaskRepository taskRepository) {
        this.worker = worker;
        this.taskExecutor = taskExecutor;
        this.taskProsesseringRepository = taskRepository;
    }

    @Scheduled(fixedDelay = POLLING_DELAY)
    @Transactional
    public void pollAndExecute() {
        log.debug("Poller etter nye tasks");
        final var maxAntall = 10;
        final var pollingSize = calculatePollingSize(maxAntall);

        final var minCapacity = 2;
        if (pollingSize > minCapacity) {
            final var tasks = taskProsesseringRepository.finnAlleTasksKlareForProsessering(PageRequest.of(0, pollingSize));
            log.trace("Pollet {} tasks med max {}", tasks.size(), maxAntall);

            tasks.forEach(this::executeWork);
        } else {
            log.trace("Pollet ingen tasks siden kapasiteten var {} < {}", pollingSize, minCapacity);
        }
        log.trace("Ferdig med polling, venter {} ms til neste kjøring.", POLLING_DELAY);
    }

    private int calculatePollingSize(int maxAntall) {
        final var remainingCapacity = ((ThreadPoolTaskExecutor) taskExecutor).getThreadPoolExecutor().getQueue().remainingCapacity();
        final var pollingSize = Math.min(remainingCapacity, maxAntall);
        log.trace("Ledig kapasitet i kø {}, poller etter {}", remainingCapacity, pollingSize);
        return pollingSize;
    }

    private void executeWork(Task task) {
        task.plukker();
        taskProsesseringRepository.saveAndFlush(task);
        worker.doTask(task.getId());
    }
}
