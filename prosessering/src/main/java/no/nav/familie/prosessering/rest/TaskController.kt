package no.nav.familie.prosessering.rest

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.sikkerhet.OIDCUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
class TaskController(private val restTaskService: RestTaskService, private val oidcUtil: OIDCUtil) {

    fun hentBrukernavn(): String {
        return oidcUtil.getClaim("preferred_username")
    }

    @GetMapping(path = ["/task"])
    fun task(@RequestHeader status: Status?,
             @RequestHeader(required = false) page: Int?): ResponseEntity<Ressurs<List<Task>>> {
        val statuser: List<Status> = status?.let { listOf(it) } ?: Status.values().toList()
        return ResponseEntity.ok(restTaskService.hentTasks(statuser, hentBrukernavn(), page ?: 0))
    }

    @GetMapping(path = ["/v2/task"])
    fun task2(@RequestParam status: Status?,
              @RequestParam(required = false) page: Int?): ResponseEntity<Ressurs<PaginableResponse<TaskDto>>> {
        val statuser: List<Status> = status?.let { listOf(it) } ?: Status.values().toList()
        return ResponseEntity.ok(restTaskService.hentTasks2(statuser, hentBrukernavn(), page ?: 0))
    }

    @GetMapping(path = ["/task/logg/{id}"])
    fun tasklogg(@PathVariable id: Long,
                 @RequestParam(required = false) page: Int?): ResponseEntity<Ressurs<List<TaskloggDto>>> {
        return ResponseEntity.ok(restTaskService.hentTaskLogg(id, hentBrukernavn()))
    }

    @PutMapping(path = ["/task/rekjor"])
    fun rekjørTask(@RequestParam taskId: Long): ResponseEntity<Ressurs<String>> {
        return ResponseEntity.ok(restTaskService.rekjørTask(taskId, hentBrukernavn()))
    }

    @PutMapping(path = ["task/rekjorAlle"])
    fun rekjørTasks(@RequestHeader status: Status): ResponseEntity<Ressurs<String>> {
        return ResponseEntity.ok(restTaskService.rekjørTasks(status, hentBrukernavn()))
    }

    @PutMapping(path = ["/task/avvikshaandter"])
    fun avvikshåndterTask(@RequestParam taskId: Long,
                          @RequestBody avvikshåndterDTO: AvvikshåndterDTO): ResponseEntity<Ressurs<String>> {
        return ResponseEntity.ok(restTaskService.avvikshåndterTask(taskId,
                                                                   avvikshåndterDTO.avvikstype,
                                                                   avvikshåndterDTO.årsak,
                                                                   hentBrukernavn()))
    }
}
