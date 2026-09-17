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
import net.ideahut.springboot.object.StringList;
import net.ideahut.springboot.object.StringMap;
import net.ideahut.springboot.object.StringSet;
import net.ideahut.springboot.template.service.MessageService;

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
	JsonNode mobile() {
		return messageService.getResource("mobile");
	}
	
	@GetMapping(value = "/portal")
	JsonNode portal() {
		return messageService.getResource("portal");
	}
	
	@PostMapping(value = "/translate/map")
	StringMap translateMap(@RequestBody StringSet codes) {
		return messageService.getMap(codes.toArray(new String[0]));
	}
	
	@PostMapping(value = "/translate/list")
	StringList translateList(@RequestBody StringList codes) {
		return messageService.getList(codes.toArray(new String[0]));
	}
	
}
