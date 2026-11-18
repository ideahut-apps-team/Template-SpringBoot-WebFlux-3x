package net.ideahut.springboot.template.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.ideahut.springboot.annotation.Public;
import net.ideahut.springboot.api.ApiAccess;
import net.ideahut.springboot.api.ApiAuth;
import net.ideahut.springboot.api.ApiRequest;
import net.ideahut.springboot.api.WebFluxApiService;
import net.ideahut.springboot.helper.WebFluxHelper;
import net.ideahut.springboot.template.service.AuthService;
import reactor.core.publisher.Mono;

/*
 * API untuk login & logout
 */
@Public
@ComponentScan
@RestController
@RequestMapping("/auth")
class AuthController {
	
	private final AuthService authService;
	private final WebFluxApiService apiService;
	
	@Autowired
	AuthController(
		AuthService authService,
		WebFluxApiService apiService
	) {
		this.authService = authService;
		this.apiService = apiService;
	}
	
	@PostMapping("/login")
	public Mono<ApiAuth> login(
		@RequestParam("username") String username,
		@RequestParam("password") String password,
		ServerHttpRequest httpRequest
	) {
		return WebFluxHelper.fromCallable(() -> {
			ApiRequest apiRequest = apiService.getApiRequest(httpRequest, true);
			ApiAuth apiAuth = authService.login(apiRequest, username, password);
			return apiAuth != null ? apiAuth.setApiAccess(null).setApiKey(null) : null;
		});
	}
	
	@RequestMapping(value = "/logout", method = {RequestMethod.GET, RequestMethod.POST})
	public Mono<ApiAccess> logout(
		ServerHttpRequest httpRequest
	) {
		return WebFluxHelper.fromCallable(() -> {
			ApiRequest apiRequest = apiService.getApiRequest(httpRequest, true);
			return authService.logout(apiRequest);
		});
	}
	
	@RequestMapping(value = "/info", method = {RequestMethod.GET, RequestMethod.POST})
	public Mono<ApiAccess> info(
		ServerHttpRequest httpRequest	
	) {
		return WebFluxHelper.fromCallable(() -> {
			ApiRequest apiRequest = apiService.getApiRequest(httpRequest, true);
			return authService.info(apiRequest);
		});
	}
	
}
