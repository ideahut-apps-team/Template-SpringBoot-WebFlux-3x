package net.ideahut.springboot.template.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import net.ideahut.springboot.annotation.Public;
import net.ideahut.springboot.helper.WebFluxHelper;
import net.ideahut.springboot.object.ListString;
import net.ideahut.springboot.object.MapString;
import net.ideahut.springboot.object.SetString;
import net.ideahut.springboot.template.service.MessageService;
import reactor.core.publisher.Mono;

/*
 * API untuk mendapatkan message yang tersimpan di resource & database
 */
@Public
@ComponentScan
@RestController
@RequestMapping("/message")
class MessageController {

	private final MessageService messageService;
	
	@Autowired
	MessageController(
		MessageService messageService		
	) {
		this.messageService = messageService;
	}

	@GetMapping(value = "/mobile")
	public Mono<JsonNode> mobile() {
		return WebFluxHelper.fromCallable(() -> messageService.getResource("mobile"));
	}
	
	@GetMapping(value = "/portal")
	public Mono<JsonNode> portal() {
		return WebFluxHelper.fromCallable(() -> messageService.getResource("portal"));
	}
	
	@PostMapping(value = "/translate/map")
	public Mono<MapString> translateMap(@RequestBody SetString codes) {
		return WebFluxHelper.fromCallable(() -> messageService.getMap(codes.toArray(new String[0])));
	}
	
	@PostMapping(value = "/translate/list")
	public Mono<ListString> translateList(@RequestBody ListString codes) {
		return WebFluxHelper.fromCallable(() -> messageService.getList(codes.toArray(new String[0])));
	}
	
}
