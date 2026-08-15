package dev.playground.springmaven;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

public class HelloSpringMavenApplication {

    public static void main(String[] args) throws Exception {
        AnnotationConfigWebApplicationContext springContext = new AnnotationConfigWebApplicationContext();
        springContext.register(WebConfig.class);

        ServletContextHandler servletContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContext.setContextPath("/");
        servletContext.addServlet(new ServletHolder(new DispatcherServlet(springContext)), "/");

        Server server = new Server(8080);
        server.setHandler(servletContext);
        server.start();
        server.join();
    }
}

@Configuration
@EnableWebMvc
@ComponentScan(basePackageClasses = HelloSpringMavenApplication.class)
class WebConfig {
}

@RestController
class HelloController {

    @GetMapping("/hello")
    String hello() {
        return "Hello from Spring MVC with Maven";
    }
}
