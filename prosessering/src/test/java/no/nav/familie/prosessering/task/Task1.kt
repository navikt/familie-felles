package no.nav.familie.prosessering.task

import no.nav.familie.prosessering.AsyncTask
import no.nav.familie.prosessering.TaskBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.Task.Companion.nyTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service @TaskBeskrivelse(tasktype = Task1.TASK_1, beskrivelse = "Dette er task 1")
class Task1 @Autowired constructor(private val taskRepository: TaskRepository) : AsyncTask {


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

    override fun onCompletion(task: Task) {
        val nesteTask =
                nyTask(Task2.TASK_2, task.payloadId)
        taskRepository.save(nesteTask)
    }

    companion object {
        const val TASK_1 = "task1"
    }

}
