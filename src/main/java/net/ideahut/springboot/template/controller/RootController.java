package net.ideahut.springboot.template.controller;

import java.util.UUID;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import net.ideahut.springboot.annotation.Public;
import net.ideahut.springboot.init.InitRequest;

@Public
@ComponentScan
@RestController
@RequestMapping("/")
class RootController {

	@GetMapping("")
	void get() {
		/**/
	}
	
	@GetMapping("/favicon.ico")
	void favicon() {
		/**/
	}
	
	@PostMapping("/init")
    ResponseEntity<String> init(@RequestBody @Valid InitRequest initRequest) {
        return ResponseEntity.ok(UUID.randomUUID().toString() + initRequest);
    }
	
}
