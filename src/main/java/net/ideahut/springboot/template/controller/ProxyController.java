package net.ideahut.springboot.template.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.ideahut.springboot.annotation.Public;
import net.ideahut.springboot.api.ApiService;
import net.ideahut.springboot.api.ApiSource;
import net.ideahut.springboot.helper.WebFluxHelper;
import net.ideahut.springboot.rest.RestMethod;
import net.ideahut.springboot.rest.RestRequest;
import reactor.core.publisher.Mono;

/*
 * API untuk request ke ApiService yang lain
 */
@ComponentScan
@RestController
@RequestMapping("/proxy")
class ProxyController {
	
	private static final AntPathMatcher pathMatcher = new AntPathMatcher();
	
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
	public Mono<String> token(
		@RequestParam("apiName") String apiName
	) {
		return WebFluxHelper.fromCallable(() -> {
			ApiSource apiSource = apiService.getApiSource(apiName);
			return apiService.getApiSourceGate(apiSource).createApiToken(apiSource);
		});
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
	public Mono<ResponseEntity<byte[]>> request(
	    @PathVariable("apiName") String apiName,
	    ServerHttpRequest httpRequest
	) {
	    String fullPath = httpRequest.getPath().pathWithinApplication().value();
	    String matchPattern = "/request/" + apiName + "/**";
	    String apiPath = pathMatcher.extractPathWithinPattern(matchPattern, fullPath);
	    RestMethod method = RestMethod.valueOf(httpRequest.getMethod().name());
	    String queryString = httpRequest.getURI().getRawQuery(); 
	    RestRequest restRequest = new RestRequest()
        .setPath("/" + apiPath)
        .setMethod(method)
        .setQueryString(queryString != null ? queryString : "")
        .setHeaders(WebFluxHelper.getHeaders(httpRequest)); 
	    Mono<RestRequest> requestMono;
	    if (!RestMethod.GET.equals(method)) {
	    	requestMono = WebFluxHelper
	    	.fromDataBuffer(httpRequest.getBody())
	    	.map(restRequest::setBody);
	    } else {
	        restRequest.getHeaders().remove(HttpHeaders.CONTENT_LENGTH);
	        requestMono = Mono.just(restRequest);
	    }
	    return requestMono.flatMap(req -> WebFluxHelper.fromCallable(() -> apiService.callEndpoint(apiName, req).throwError()) )
	    .map(restResponse -> {
	        HttpHeaders responseHeaders = new HttpHeaders();
	        for (String restHeaderName : restResponse.getHeaderNames()) {
	            List<String> restHeaderValues = restResponse.getHeaderValues(restHeaderName);
	            responseHeaders.addAll(restHeaderName, restHeaderValues);
	        }
	        return ResponseEntity
	            .status(restResponse.getStatus())
	            .headers(responseHeaders)
	            .body(restResponse.getBodyAsByteArray());
	    });
	}
	
}
