package net.ideahut.springboot.template.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.ideahut.springboot.admin.AdminHandler;
import net.ideahut.springboot.helper.ObjectHelper;
import net.ideahut.springboot.interceptor.AdminWebFluxInterceptor;
import net.ideahut.springboot.object.SetString;
import net.ideahut.springboot.security.WebFluxSecurity;
import net.ideahut.springboot.template.Application;
import net.ideahut.springboot.template.app.AppProperties;

@Component
public class AdminRequestInterceptor extends AdminWebFluxInterceptor {

	private final AdminHandler adminHandler;
	private final WebFluxSecurity adminSecurity;
	private final String publicBaseUrl;
	private final SetString allowPaths;
	private final SetString skipPaths;
	
	@Autowired
	AdminRequestInterceptor(
		AppProperties appProperties,
		AdminHandler adminHandler,
		WebFluxSecurity adminSecurity
	) {
		SetString allowPathsEi = new SetString();
		allowPathsEi.add(adminHandler.getApiPath() + "/**");
		if (adminHandler.isWebEnabled()) {
			allowPathsEi.add(adminHandler.getWebPath() + "/**");
		}
		SetString skipPathsEi = new SetString();
		this.adminHandler = adminHandler;
		this.adminSecurity = adminSecurity;
		this.publicBaseUrl = ObjectHelper.useOrDefault(appProperties.getPublicBaseUrl(), "");
		this.allowPaths = SetString.unmodifiable(allowPathsEi);
		this.skipPaths = SetString.unmodifiable(skipPathsEi);
	}
	
	@Override
	protected boolean isServiceAvailable() {
		return Application.isReady();
	}
	
	@Override
	protected String publicBaseUrl() {
		return publicBaseUrl;
	}

	@Override
	protected AdminHandler adminHandler() {
		return adminHandler;
	}

	@Override
	protected WebFluxSecurity adminSecurity() {
		return adminSecurity;
	}

	@Override
	public SetString allowPaths() {
		return allowPaths;
	}

	@Override
	public SetString skipPaths() {
		return skipPaths;
	}

}
