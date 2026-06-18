package com.mentis.hrms.config;

import com.mentis.hrms.interceptor.RoleInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private RoleInterceptor roleInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/dashboard/**", "/candidate/dashboard/**")
                .excludePathPatterns(
                        "/candidate/auth/**",
                        "/candidate/login",
                        "/candidate/create-password",
                        "/candidate/forgot-password",
                        "/candidate/reset-password/**",

                        // Static resources
                        "/static/**",
                        "/webjars/**",
                        "/uploads/**",

                        // Resume endpoints
                        "/dashboard/hr/download-resume/**",
                        "/dashboard/hr/preview-resume/**",
                        "/dashboard/hr/preview-image/**",
                        "/dashboard/download-resume/**",
                        "/dashboard/preview-resume/**",
                        "/dashboard/preview-image/**",

                        // ✅ ADD THIS - Allow admin attendance APIs
                        "/dashboard/admin/attendance/api/**"
                );
    }




    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Static resources (keep explicit handlers for predictable resolution)
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(3600);

        // ✅ CRITICAL: Add handler for data JSON files
        registry.addResourceHandler("/data/**")
                .addResourceLocations("classpath:/static/data/")
                .setCachePeriod(3600);

        // Uploaded files
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///C:/hrms/uploads/")
                .setCachePeriod(3600);

        // Webjars
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .setCachePeriod(3600);
    }

}