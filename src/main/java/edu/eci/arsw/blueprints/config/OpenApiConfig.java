package edu.eci.arsw.blueprints.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI api() {
        return new OpenAPI()
                .info(new Info()
                        .title("ARSW Blueprints API")
                        .version("v1.0.0")
                        .description("""
                                REST API para gestión de Blueprints.
                                Laboratorio #4 – Arquitecturas de Software (ARSW).
                                Escuela Colombiana de Ingeniería Julio Garavito.
                                """)
                        .contact(new Contact()
                                .name("Escuela Colombiana de Ingeniería")
                                .url("https://www.escuelaing.edu.co"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Servidor local")
                ));
    }
}
