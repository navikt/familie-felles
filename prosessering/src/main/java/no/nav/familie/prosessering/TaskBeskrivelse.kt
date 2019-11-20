package no.nav.familie.prosessering


import java.lang.annotation.*

@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
annotation class TaskBeskrivelse(
        /**
         * Antall retries på tasken før den settes til feilet
         *
         * @return antall forsøk
         */
        val maxAntallFeil: Int = 3,
        /**
         * Task typen
         *
         * @return typen
         */
        val tasktype: String,
        /**
         * Beskrivelse for task typen
         *
         * @return beskrivelsen
         */
        val beskrivelse: String)
