package net.ideahut.springboot.template.controller.test;

import java.util.function.Supplier;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import net.ideahut.springboot.annotation.Body;
import net.ideahut.springboot.annotation.Public;
import net.ideahut.springboot.helper.ErrorHelper;
import net.ideahut.springboot.helper.ObjectHelper;
import net.ideahut.springboot.helper.StringHelper;
import net.ideahut.springboot.helper.ThreadHelper;
import net.ideahut.springboot.helper.WebFluxHelper;
import net.ideahut.springboot.object.Message;
import net.ideahut.springboot.object.Result;
import reactor.core.publisher.Flux;

/*
 * Contoh API untuk fungsi dasar http
 */
@Slf4j
@Public
@ComponentScan
@RestController
@RequestMapping("/test/basic")
class BasicController {

	@GetMapping("/exception")
	void exception() {
		throw ErrorHelper.exception(() -> StringHelper.format("ERROR-{}", System.nanoTime()));
	}
	
	@GetMapping("/virtualThread")
	Result virtualThread() {
		Thread thread = Thread.currentThread();
		boolean isVt = ThreadHelper.isThreadVirtual(thread);
		return Result.success(isVt).setInfo("thread", thread.getName());
	}
	
	@GetMapping("/bytes")
	byte[] bytes() {
		return ("BYTES-" + System.nanoTime()).getBytes();
	}
	
	@GetMapping("/string")
	String string() {
		return "STRING-" + System.nanoTime();
	}
	
	@GetMapping("/responseEntity")
	ResponseEntity<String> responseEntity() {
		return ResponseEntity.ok()
		.header("Test-Strre", "string")
		.body("STRRE-" + System.nanoTime());
	}

	@GetMapping("/send")
	void send(
		ServerWebExchange exchange
	) {
		//WebFluxHelper.sendResponse(exchange, null, false, "SEND-" + System.nanoTime()); //-
		WebFluxHelper.sendResponse(exchange, "SEND-" + System.nanoTime()); //-
		//WebFluxHelper.sendResponse(exchange, null, false, System.nanoTime()); //-
		//WebFluxHelper.sendResponse(exchange, System.nanoTime()); //-
		//WebFluxHelper.sendResponse(exchange, new Exception("ERROR-SEND-" + System.nanoTime())); //-
		
		/**
		String hval = System.nanoTime() + "";
		response.setHeader("xxx1", hval);
		response.setHeader("xxx2", hval);
		ResponseEntity<Message> re = ResponseEntity.ok()
		.header("xxx2", "KEREN", "LAGI")
		.header("yyyy", "NONE")
		.body(Message.of("YYY", "VALUE"));
		WebFluxHelper.sendResponse(exchange, re);
		*/
	}
	
	@GetMapping("/result")
	Result result() {
		return Result.success("RESULT-" + System.nanoTime());
	}
	
	@GetMapping("/message")
	Message message() {
		return Message.of("MSG", "MESSAGE-{}", System.nanoTime());
	}
	
	// tambahkan annotation @Body(request = true), agar request tidak dibaca di level filter
	@Body
	@PostMapping(value = "/multipart")
	Flux<Result> multipart(
		@RequestPart("name") String name,
		@RequestPart(name = "file", required = false) FilePart file
	) {
		return ObjectHelper.callOrElse(
			file != null, 
			() -> file.content().flatMap(dataBuffer -> {
		        byte[] bytes = WebFluxHelper.getDataBufferAsBytes(dataBuffer);
		        Result result = Result.success()
		        .setInfo("name", name)
		        .setInfo("length", bytes.length)
		        .setInfo("filename", file.filename());
		        return Flux.just(result);
		    }), 
			() -> {
				Result result = Result.success()
				.setInfo("name", name);
				return Flux.just(result);
			}
		);
	}
	
	@GetMapping("/logger")
	void logger() {
		Throwable throwable = new Exception(StringHelper.format("EXCEPTION: {}", System.nanoTime() + ""));
		log.debug("{}", message(() -> "DEBUG"), throwable);
		log.trace("{}", message(() -> "TRACE"), throwable);
		log.info("{}", message(() -> "INFO-" + System.nanoTime()), throwable);
		log.warn("{}", message(() -> "WARN"), throwable);
		log.error("{}", message(() -> "ERROR"), throwable);
	}
	
	private Object message(Supplier<CharSequence> message) {
		return new Object() {
			@Override
			public String toString() {
				CharSequence charSequence = message != null ? message.get() : null;
				return charSequence != null ? charSequence.toString() : "";
			}
		};
	}
	
}
