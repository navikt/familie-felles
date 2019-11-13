package no.nav.familie.prosessering.task;

import no.nav.familie.prosessering.AsyncTask;
import no.nav.familie.prosessering.TaskBeskrivelse;
import no.nav.familie.prosessering.domene.Task;
import no.nav.familie.prosessering.domene.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@TaskBeskrivelse(taskType = Task1.TASK_1, beskrivelse = "Dette er task 1")
public class Task1 implements AsyncTask {

    public static final String TASK_1= "task1";

    private TaskRepository taskRepository;


    @Autowired
    public Task1(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }


    @Override
    public void doTask(Task task) {

        try {
            TimeUnit.MICROSECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCompletion(Task task){
        Task nesteTask = Task.nyTask(Task2.TASK_2, task.getPayload());
        taskRepository.save(nesteTask);
    }
}
