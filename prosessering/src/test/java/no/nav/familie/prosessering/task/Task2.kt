package no.nav.familie.prosessering.task

import no.nav.familie.prosessering.AsyncTask
import no.nav.familie.prosessering.TaskBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service @TaskBeskrivelse(tasktype = Task2.TASK_2, beskrivelse = "Dette er task 2")
class Task2 : AsyncTask {
    override fun doTask(task: Task) {
        try {
            TimeUnit.MICROSECONDS.sleep(1)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun preCondition(task: Task) {
        super.preCondition(task)
    }

    override fun onCompletion(task: Task) {}

    companion object {
        const val TASK_2 = "task2"
    }
}
