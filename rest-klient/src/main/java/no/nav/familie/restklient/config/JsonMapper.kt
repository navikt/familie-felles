package no.nav.familie.restklient.config

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

val jsonMapper: JsonMapper
    get() = jsonMapperBuilder.build()

val jsonMapperBuilder: JsonMapper.Builder =
    JsonMapper
        .builder()
        .addModule(KotlinModule.Builder().build())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
