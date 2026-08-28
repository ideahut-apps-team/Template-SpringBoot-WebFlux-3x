package net.ideahut.springboot.template.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.resource.VersionResourceResolver;

import net.ideahut.springboot.admin.AdminHandler;
import net.ideahut.springboot.config.WebFluxConfig;
import net.ideahut.springboot.object.TimeValue;

@Configuration
@EnableWebFlux
class WebConfig extends WebFluxConfig {
	
	private final AdminHandler adminHandler;
	
	@Autowired
	WebConfig(
		AdminHandler adminHandler
	) {
		this.adminHandler = adminHandler;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		if (adminHandler.isWebEnabled() && !registry.hasMappingForPattern(adminHandler.getWebPath() + "/**")) {
			TimeValue maxAge = adminHandler.getWebCacheMaxAge();
			registry
			.addResourceHandler(adminHandler.getWebPath() + "/**")
			.addResourceLocations(adminHandler.getWebLocation())
			.setCacheControl(CacheControl.maxAge(maxAge.getValue(), maxAge.getUnit()))
	        .resourceChain(adminHandler.isWebResourceChain())
	        .addResolver(new VersionResourceResolver().addContentVersionStrategy(adminHandler.getWebPath() + "/**"));
		}
		super.addResourceHandlers(registry);
	}
	
}
