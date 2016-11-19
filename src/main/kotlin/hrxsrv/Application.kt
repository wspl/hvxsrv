package hrxsrv

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@SpringBootApplication
open class Application

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}


// for supports RequestBody with kotlin data class
@Configuration
open class ObjectMapperConfiguration {
    @Bean
    @Primary
    open fun objectMapper() = ObjectMapper().apply {
        registerModule(KotlinModule())
    }
}