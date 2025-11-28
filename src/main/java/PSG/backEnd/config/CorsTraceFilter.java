package PSG.backEnd.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class CorsTraceFilter {

    @Bean
    public FilterRegistrationBean<Filter> corsTrace() {
        Filter f = new Filter() {
            @Override public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletRequest r = (HttpServletRequest) req;
                String origin = r.getHeader("Origin");
                String acrm   = r.getHeader("Access-Control-Request-Method");
                if ("OPTIONS".equalsIgnoreCase(r.getMethod())) {
                    System.out.printf("[TRACE] OPTIONS %s | Origin=%s | ACRM=%s%n",
                            r.getRequestURI(), origin, acrm);
                }
                chain.doFilter(req, res);
            }
        };
        FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>(f);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1); // Right after the global CorsFilter
        return bean;
    }
}
