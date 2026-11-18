package net.ideahut.springboot.template.controller;

import java.util.UUID;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import net.ideahut.springboot.annotation.Public;
import net.ideahut.springboot.helper.WebFluxHelper;
import net.ideahut.springboot.init.InitRequest;
import reactor.core.publisher.Mono;

@Public
@ComponentScan
@RestController
@RequestMapping("/")
class RootController {

	@GetMapping("")
	public void get() {
		/**/
	}
	
	@GetMapping("/favicon.ico")
	public void favicon() {
		/**/
	}
	
	@PostMapping("/init")
	public Mono<String> init(@RequestBody @Valid InitRequest initRequest) {
		return WebFluxHelper.fromCallable(() -> UUID.randomUUID().toString() + initRequest);
    }
	
}
