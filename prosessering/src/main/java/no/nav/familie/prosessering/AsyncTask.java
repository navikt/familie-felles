package no.nav.familie.prosessering;

import no.nav.familie.prosessering.domene.Task;

public interface AsyncTask {

    /**
     * Caster exception hvis ikke oppfylt.
     *
     * @param task Henvendelsen som skal vurderes
     */
    default void preCondition(Task task) {
        // Do nothing by default
    }

    /**
     * Caster exception hvis ikke oppfylt.
     *
     * @param task Henvendelsen som skal vurderes
     */
    default void postCondition(Task task) {
        // Do nothing by default
    }

    /**
     * Utfør selve arbeidet.
     *
     * @param task Hendelsen
     * @throws RuntimeException exception vil markere saken som feilende
     */
    void doTask(Task task);

    /**
     * Eventuelle oppgaver som må utføres etter at tasken har kjørt OK.
     * Kan f.eks være å planlegge en ny task av en annen type.
     *
     * @param task Henvendelsen som skal vurderes
     */
    default void onCompletion(Task task) {
        // Do nothing by default
    }
}
