package net.ideahut.springboot.template.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.ideahut.springboot.annotation.Public;
import net.ideahut.springboot.api.ApiService;
import net.ideahut.springboot.api.ApiTokenSysParam;
import net.ideahut.springboot.helper.ObjectHelper;
import net.ideahut.springboot.helper.StringHelper;
import net.ideahut.springboot.helper.WebFluxHelper;
import net.ideahut.springboot.rest.RestMethod;
import net.ideahut.springboot.rest.RestRequest;
import net.ideahut.springboot.rest.RestResponse;
import reactor.core.publisher.Mono;

/*
 * API untuk request ke ApiService yang lain
 */
@ComponentScan
@RestController
@RequestMapping("/proxy")
class ProxyController {
	
	private final ApiService apiService;
	
	@Autowired
	ProxyController(
		ApiService apiService
	) {
		this.apiService = apiService;
	}
	
	/*
	 * Meminta token API consumer untuk melakukan request
	 * Token akan disimpan di SysParams, sysCode = "API_TOKEN", paramCode = <API_NAME>
	 */
	@Public
	@PostMapping("/token")
	void token(
		@RequestParam("apiName") String apiName
	) {
		String token = apiService.getApiTokenService().retrieveApiToken(apiService, apiName);
		if (!StringHelper.isEmpty(token) && ObjectHelper.isInstance(ApiTokenSysParam.class, apiService.getApiTokenService())) {
			((ApiTokenSysParam) apiService.getApiTokenService()).updateSysParamApiToken(apiName, token);
		}
	}

	/*
	 * Proxy request ke ApiService lain
	 * - menggunakan ApiToken yang tersimpan di SysParam
	 * - semua http method diijinkan
	 * 
	 */
	@Public
	@RequestMapping(
		path = "/request/{apiName}/**", 
		method = { 
			RequestMethod.GET, 
			RequestMethod.POST, 
			RequestMethod.PUT, 
			RequestMethod.DELETE,
			RequestMethod.HEAD,
			RequestMethod.PATCH,
			RequestMethod.TRACE
		}
	)
	Mono<byte[]> request(
		@PathVariable("apiName") String apiName,
		ServerHttpRequest httpRequest,
		ServerHttpResponse httpResponse
	) {
		return WebFluxHelper
		.onRequestBody(httpRequest)
		.flatMap(requestBody -> {
			// replace prefix path, dan path sisanya akan diappend ke base url service yang dituju
			String path = httpRequest
			.getPath()
			.pathWithinApplication()
			.value()
			.replace("/proxy/request/" + apiName, "");
			String apiToken = apiService.getApiTokenService().getSysParamApiToken(apiName);
			RestRequest restRequest = new RestRequest()
			.setPath(path)
			.setMethod(RestMethod.valueOf(httpRequest.getMethod().name().toUpperCase()))
			.setQueries(httpRequest.getQueryParams())
			.setHeaders(WebFluxHelper.getHeaders(httpResponse));
			ObjectHelper.callOrElse(
				!RestMethod.GET.equals(restRequest.getMethod()), 
				() -> restRequest.setBody(requestBody), 
				() -> restRequest.getHeaders().remove(HttpHeaders.CONTENT_LENGTH)
			);
			RestResponse restResponse = apiService.callApiEndpoint(apiName, restRequest, apiToken);
			for (String restHeaderName : restResponse.getHeaderNames()) {
				httpResponse.getHeaders().put(restHeaderName, restResponse.getHeaderValues(restHeaderName));
			}
			byte[] body = restResponse.getBodyAsByteArray();
			return Mono.just(body);
		});
	}
	
}
