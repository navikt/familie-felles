package no.nav.familie.prosessering.rest

import no.nav.familie.ks.kontrakter.sak.Ressurs
import no.nav.familie.ks.mottak.app.rest.AvvikshåndterDTO
import no.nav.familie.sikkerhet.OIDCUtil
import no.nav.familie.prosessering.domene.Status
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
class TaskController(
        private val restTaskService: RestTaskService, private val oidcUtil: OIDCUtil) {

    fun hentBrukernavn(): String {
        return oidcUtil.getClaim("preferred_username")
    }

    @GetMapping(path = ["/task"])
    fun task(@RequestHeader status: Status): ResponseEntity<Ressurs> {
        return ResponseEntity.ok(restTaskService.hentTasks(status, hentBrukernavn()))
    }

    @PutMapping(path = ["/task/rekjor"])
    fun rekjørTask(@RequestParam taskId: Long): ResponseEntity<Ressurs> {
        return ResponseEntity.ok(restTaskService.rekjørTask(taskId, hentBrukernavn()))
    }

    @PutMapping(path = ["task/rekjorAlle"])
    fun rekjørTasks(@RequestHeader status: Status): ResponseEntity<Ressurs> {
        return ResponseEntity.ok(restTaskService.rekjørTasks(status, hentBrukernavn()))
    }

    @PutMapping(path = ["/task/avvikshaandter"])
    fun avvikshåndterTask(@RequestParam taskId: Long, @RequestBody avvikshåndterDTO: AvvikshåndterDTO): ResponseEntity<Ressurs> {
        return ResponseEntity.ok(restTaskService.avvikshåndterTask(taskId, avvikshåndterDTO.avvikstype, avvikshåndterDTO.årsak, hentBrukernavn()))
    }
}
