package no.nav.familie.prosessering.task;

import no.nav.familie.prosessering.AsyncTask;
import no.nav.familie.prosessering.TaskBeskrivelse;
import no.nav.familie.prosessering.domene.Task;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@TaskBeskrivelse(taskType = Task2.TASK_2, beskrivelse = "Dette er task 2")
public class Task2 implements AsyncTask {

    public static final String TASK_2= "task2";


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

    }
}
