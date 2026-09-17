package net.ideahut.springboot.template.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.ideahut.springboot.audit.AuditHandler;
import net.ideahut.springboot.audit.AuditRequest;
import net.ideahut.springboot.helper.ErrorHelper;
import net.ideahut.springboot.helper.ObjectHelper;
import net.ideahut.springboot.helper.StringHelper;
import net.ideahut.springboot.helper.WebFluxHelper;
import net.ideahut.springboot.object.Page;
import net.ideahut.springboot.object.Result;
import net.ideahut.springboot.template.Application;
import reactor.core.publisher.Mono;

/*
 * API untuk melihat data audit
 */
@ComponentScan
@RestController
@RequestMapping("/audit")
class AuditController {
	
	private final AuditHandler auditHandler;
	
	@Autowired
	AuditController(
		AuditHandler auditHandler
	) {
		this.auditHandler = auditHandler;
	}
	
	
	@PostMapping(value = "/list")
	Mono<Result> list(ServerHttpRequest httpRequest) {
		return WebFluxHelper
		.onRequestBody(httpRequest)
		.flatMap(bytes -> {
			AuditRequest auditRequest = auditHandler.getRequest(bytes);
			String entity = ObjectHelper.useOrDefault(auditRequest.getEntity(), "").trim();
			ObjectHelper.callIf(
				!StringHelper.isEmpty(entity) && auditRequest.getClassOfEntity() == null, 
				() -> {
					Class<?> classOfEntity = ObjectHelper.useOrDefault(
						ObjectHelper.safeClassOf(entity), 
						() -> ObjectHelper.safeClassOf(Application.Package.APPLICATION + ".entity." + entity)
					);
					ErrorHelper.throwNull(classOfEntity, () -> "Entity not found: " + entity);
					return auditRequest.setClassOfEntity(classOfEntity);
				}
			);			
			Page page = auditHandler.getList(auditRequest);
			return Mono.just(Result.success(page));
		});
	}
	
	
	@GetMapping(value = "/bytes")
	Result bytes(
		@RequestParam(name = "manager", required = false) String manager,
		@RequestParam(name = "id") String id
	) {
		byte[] bytes = auditHandler.getBytes(manager, id);
		return Result.success(bytes);
	}
	
	
}
