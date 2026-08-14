package com.mtn.yolo.ussdgatewayservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter
import org.springframework.web.client.RestTemplate

@SpringBootApplication
class UssdGatewayServiceApplication {

    @Bean
    fun restTemplate(): RestTemplate {
        val restTemplate = RestTemplate(clientHttpRequestFactory())
        
        // Reorder message converters: JSON first, then XML
        val converters = mutableListOf<HttpMessageConverter<*>>()
        
        // Add JSON converter first (highest priority)
        val jsonConverter = MappingJackson2HttpMessageConverter()
        jsonConverter.supportedMediaTypes = listOf(MediaType.APPLICATION_JSON)
        converters.add(jsonConverter)
        
        // Add XML converter second (lower priority)
        val xmlConverter = MappingJackson2XmlHttpMessageConverter()
        xmlConverter.supportedMediaTypes = listOf(MediaType.APPLICATION_XML)
        converters.add(xmlConverter)
        
        restTemplate.messageConverters = converters
        return restTemplate
    }
    
    private fun clientHttpRequestFactory(): ClientHttpRequestFactory {
        val factory = SimpleClientHttpRequestFactory()
        factory.setConnectTimeout(5000)
        factory.setReadTimeout(5000)
        return BufferingClientHttpRequestFactory(factory)
    }
}

fun main(args: Array<String>) {
    runApplication<UssdGatewayServiceApplication>(*args)
}
