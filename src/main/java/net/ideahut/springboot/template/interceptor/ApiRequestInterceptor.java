package net.ideahut.springboot.template.interceptor;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import net.ideahut.springboot.admin.AdminHandler;
import net.ideahut.springboot.api.ApiAccess;
import net.ideahut.springboot.api.ApiService;
import net.ideahut.springboot.api.WebFluxApiService;
import net.ideahut.springboot.function.TriFunction;
import net.ideahut.springboot.interceptor.ApiWebFluxInterceptor;
import net.ideahut.springboot.mapper.DataMapper;
import net.ideahut.springboot.object.SetString;
import net.ideahut.springboot.template.Application;

@Component
public class ApiRequestInterceptor extends ApiWebFluxInterceptor {

	// set false, agar ApiService tidak melakukan pengecekan (development)
	private static final boolean CHECK_BY_API_SERVICE = true;
	
	// set true, jika tidak ingin mengecek ApiRole / ApiSource request (development)
	private static final boolean ALLOW_ALL_REQUEST = false;
	
	// allow all request function
	private static final TriFunction<ApiService, ApiAccess, HandlerMethod, Boolean> ALLOW_ALL_FUNCTION = 
	(service, apiAccess, handlerMethod) -> true;
	
	private final DataMapper dataMapper;
	private final WebFluxApiService apiService;
	private final ApiAccess publicApiAccess;
	private final SetString allowPaths;
	private final SetString skipPaths;
	
	@Autowired
	ApiRequestInterceptor(
		DataMapper dataMapper,
		AdminHandler adminHandler,
		WebFluxApiService apiService
	) {
		SetString allowPathsEi = new SetString(Arrays.asList("/**"));
		SetString skipPathsEi = new SetString();
		skipPathsEi.add(adminHandler.getApiPath() + "/**");
		if (adminHandler.isWebEnabled()) {
			skipPathsEi.add(adminHandler.getWebPath() + "/**");
		}
		this.dataMapper = dataMapper;
		this.apiService = apiService;
		this.publicApiAccess = ApiAccess.unmodifiable(new ApiAccess().setApiRole("PUBLIC"));
		this.allowPaths = SetString.unmodifiable(allowPathsEi);
		this.skipPaths = SetString.unmodifiable(skipPathsEi);
	}
	
	@Override
	protected boolean isServiceAvailable() {
		return Application.isReady();
	}
	
	@Override
	protected ApiAccess publicApiAccess() {
		return publicApiAccess;
	}
	
	@Override
	protected WebFluxApiService apiService() {
		return CHECK_BY_API_SERVICE ? apiService : null;
	}
	
	@Override
	protected DataMapper dataMapper() {
		return dataMapper;
	}

	@Override
	public SetString allowPaths() {
		return allowPaths;
	}

	@Override
	public SetString skipPaths() {
		return skipPaths;
	}

	@Override
	protected TriFunction<ApiService, ApiAccess, HandlerMethod, Boolean> allowFunction() {
		return ALLOW_ALL_REQUEST ? ALLOW_ALL_FUNCTION : super.allowFunction();
	}
}
