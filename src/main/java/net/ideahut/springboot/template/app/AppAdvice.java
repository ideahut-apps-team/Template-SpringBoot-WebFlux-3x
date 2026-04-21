package net.ideahut.springboot.template.app;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;

import net.ideahut.springboot.admin.AdminHandler;
import net.ideahut.springboot.advice.WebFluxAdvice;
import net.ideahut.springboot.object.StringSet;

/*
 * - Untuk menghandle semua error yang terjadi di aplikasi
 * - Intercept object response sebelum dikirim ke client
 * 
 */

@ControllerAdvice
class AppAdvice extends WebFluxAdvice {

	private final AppProperties appProperties;
	private final AdminHandler adminHandler;
	
	@Autowired
	AppAdvice(
		AppProperties appProperties,
		AdminHandler adminHandler
	) {
		this.appProperties = appProperties;
		this.adminHandler = adminHandler;
	}

	@Override
	protected boolean logAllError() {
		return !Boolean.FALSE.equals(appProperties.getLogAllError());
	}

	@Override
	protected Collection<String> exceptionSkipPaths() {
		StringSet skipPaths = new StringSet();
		skipPaths.add(adminHandler.getWebPath() + "/**");
		return skipPaths;
	}
	
}
