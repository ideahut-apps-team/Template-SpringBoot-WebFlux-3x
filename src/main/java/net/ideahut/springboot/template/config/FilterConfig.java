package net.ideahut.springboot.template.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;

import net.ideahut.springboot.definition.FilterDefinition;
import net.ideahut.springboot.filter.WebFluxRequestFilter;
import net.ideahut.springboot.helper.ObjectHelper;
import net.ideahut.springboot.template.app.AppProperties;
import net.ideahut.springboot.template.interceptor.AdminRequestInterceptor;
import net.ideahut.springboot.template.interceptor.ApiRequestInterceptor;

/*
 * Konfigurasi Filter
 */
@Configuration
class FilterConfig {
	
	@Bean
	protected WebFluxRequestFilter defaultRequestFilter(
		AppProperties appProperties,
		RequestMappingHandlerMapping requestMappingHandlerMapping,
		AdminRequestInterceptor adminRequestInterceptor,
		ApiRequestInterceptor apiRequestInterceptor
	) {
		FilterDefinition filter = ObjectHelper.useOrDefault(
			appProperties.getFilter(), 
			FilterDefinition::new
		);
		return new WebFluxRequestFilter()
				
		// ijinkan semua path untuk dicek di filter
		.setAllowPaths("/**")
		
		// handler mapping yang digunakan untuk mendapatkan HandlerMethod untuk mengecek permission
		.setHandlerMapping(requestMappingHandlerMapping)
		
		// daftar header yang selalu ditambahkan ke response, berguna untuk CORS
		.setHeaders(filter.getHeaders())
		
		// daftar interceptor untuk mengecek permission
		.setInterceptors(
			adminRequestInterceptor,
			apiRequestInterceptor
		)
		
		// untuk mengecek skipPaths & allowPaths baik di filter maupun di interceptor
		.setPathMatcher(null)
		
		// informasi waktu proses disertakan ke respon atau tidak
		.setResultTimeEnable(filter.getResultTimeEnable())
		
		// satuan waktu proses yang ditampilkan di respon
		.setResultTimeUnit(filter.getResultTimeUnit())
		
		// daftar path yang tidak akan dicek oleh filter
		//.setSkipPaths()
		
		// trace diaktifkan atau tidak
		.setTraceEnable(filter.getTraceEnable())
		
		// untuk membuat key unik log
		.setTraceGenerator(null)
		
		// nama trace key yang di daftar ke konfigurasi log
		//   pattern:
	    //       console: '%d %p [%X{traceId}] %F\(%M:%L\) : %msg%n'
	    //       file: '%d %p [%X{traceId}] %F\(%M:%L\) : %msg%n'
		// dari contoh di atas traceKey => traceId
		.setTraceKey(filter.getTraceKey());
	}
	
}
