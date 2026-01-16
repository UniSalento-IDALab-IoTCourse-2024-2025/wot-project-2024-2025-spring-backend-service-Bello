package bello.antonio.carrier_management_service.configuration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Consenti solo le rotte che iniziano con /api
                .allowedOrigins("http://13.60.154.6:80")  // Il tuo frontend React
                .allowedMethods("GET", "POST", "PUT", "DELETE") // Metodi HTTP permessi
                .allowedHeaders("*") // Permetti tutte le intestazioni
                .allowCredentials(true); // Permetti l'invio di credenziali (come i cookie)
    }
}
*/