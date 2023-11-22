package no.nav.familie.http.client

import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

class MultipartBuilder {
    private val multipartRequest: MultiValueMap<String, Any> = LinkedMultiValueMap()

    fun withJson(
        name: String,
        any: Any,
    ): MultipartBuilder {
        val headers = HttpHeaders()
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        multipartRequest.add(name, HttpEntity(any, headers))
        return this
    }

    fun withByteArray(
        name: String,
        filename: String,
        bytes: ByteArray,
    ): MultipartBuilder {
        val request =
            object : ByteArrayResource(bytes) {
                override fun getFilename(): String? {
                    return filename
                }
            }
        multipartRequest.add(name, request)
        return this
    }

    fun build(): MultiValueMap<String, Any> {
        return multipartRequest
    }

    fun asEntity(): HttpEntity<MultiValueMap<String, Any>> {
        return HttpEntity(multipartRequest, MULTIPART_HEADERS)
    }

    companion object {
        val MULTIPART_HEADERS: HttpHeaders =
            HttpHeaders().apply {
                this.set(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
            }
    }
}
