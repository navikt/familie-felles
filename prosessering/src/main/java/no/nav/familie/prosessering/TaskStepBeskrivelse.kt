package no.nav.familie.prosessering


import java.lang.annotation.*

@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
annotation class TaskStepBeskrivelse(
        /**
         * Antall retries på tasken før den settes til feilet
         *
         * @return antall forsøk
         */
        val maxAntallFeil: Int = 3,
        /**
         * TaskStep-typen
         *
         * @return typen
         */
        val taskStepType: String,
        /**
         * Beskrivelse for taskStep
         *
         * @return beskrivelsen
         */
        val beskrivelse: String)
