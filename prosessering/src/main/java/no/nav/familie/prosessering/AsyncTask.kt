package no.nav.familie.prosessering

import no.nav.familie.prosessering.domene.Task

interface AsyncTask {

    /**
     * Caster exception hvis ikke oppfylt.
     *
     * @param task Henvendelsen som skal vurderes
     */
    fun preCondition(task: Task) {
        // Do nothing by default
    }

    /**
     * Caster exception hvis ikke oppfylt.
     *
     * @param task Henvendelsen som skal vurderes
     */
    fun postCondition(task: Task) {
        // Do nothing by default
    }

    /**
     * Utfør selve arbeidet.
     *
     * @param task Hendelsen
     * @throws RuntimeException exception vil markere saken som feilende
     */
    fun doTask(task: Task)

    /**
     * Eventuelle oppgaver som må utføres etter at tasken har kjørt OK.
     * Kan f.eks være å planlegge en ny task av en annen type.
     *
     * @param task Henvendelsen som skal vurderes
     */
    fun onCompletion(task: Task) {
        // Do nothing by default
    }
}
