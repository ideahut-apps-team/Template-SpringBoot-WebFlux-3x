package net.ideahut.springboot.template.controller.test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import net.ideahut.springboot.annotation.Public;
import net.ideahut.springboot.helper.ErrorHelper;
import net.ideahut.springboot.helper.StringHelper;
import net.ideahut.springboot.helper.ThreadHelper;
import net.ideahut.springboot.helper.WebFluxHelper;
import net.ideahut.springboot.object.Message;
import net.ideahut.springboot.object.Result;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
	public void exception() {
		throw ErrorHelper.exception(() -> StringHelper.format("ERROR-{}", System.nanoTime()));
	}
	
	@GetMapping("/virtualThread")
	public Mono<Result> virtualThread() {
		return WebFluxHelper.fromCallable(() -> {
			Thread thread = Thread.currentThread();
			boolean isVt = ThreadHelper.isThreadVirtual(thread);
			return Result.success(isVt).setInfo("thread", thread.getName());
		});
	}
	
	@GetMapping("/bytes")
	public Mono<byte[]> bytes() {
		return WebFluxHelper.fromCallable(() -> StringHelper.toBytes("BYTES-" + System.nanoTime()));
	}
	
	@GetMapping("/string")
	public Mono<String> string() {
		return WebFluxHelper.fromCallable(() -> "STRING-" + System.nanoTime());
	}
	
	@GetMapping("/responseEntity")
	public Mono<ResponseEntity<String>> responseEntity() {
		return WebFluxHelper.fromCallable(() -> ResponseEntity.ok()
			.header("Test-Strre", "string")
			.body("STRRE-" + System.nanoTime())
		);
	}

	@GetMapping("/send")
	public void send(
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
	public Mono<Result> result() {
		return WebFluxHelper.fromCallable(() -> Result.success("RESULT-" + System.nanoTime()));
	}
	
	@GetMapping("/message")
	public Mono<Message> message() {
		return WebFluxHelper.fromCallable(() -> Message.of("MSG", "MESSAGE-{}", System.nanoTime()));
	}
	
	@GetMapping("/outstream")
    public Mono<Void> outstream(
    	ServerHttpResponse httpResponse
    ) {
        return Mono.defer(() -> {
            byte[] bytes = StringHelper.toBytes("Haloooo-" + System.nanoTime());
            DataBuffer buffer = httpResponse.bufferFactory().wrap(bytes);
            return httpResponse.writeWith(Mono.just(buffer));
        });
    }
	
	// contoh download streaming
	@GetMapping("/download")
    public ResponseEntity<Flux<DataBuffer>> download() {
        String filename = System.nanoTime() + ".txt";
        Flux<DataBuffer> dataStream = Flux.range(0, 10)
        .delayElements(Duration.ofSeconds(1)) // simulasi proses write yang lambat
        .map(i -> DefaultDataBufferFactory.sharedInstance.wrap(StringHelper.toBytes("DATA-" + i + "\n")));
        return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(dataStream);
    }
    
    // contoh streaming baca request dan kirim response
    @PostMapping(value = "/streaming")
	public Flux<DataBuffer> streaming(
		@RequestParam(name = "buffer", required = false) Integer buffer,
		@RequestParam(name = "delay", required = false) Integer delay,
		ServerHttpRequest httpRequest
	) {
		int sleep = (delay != null && delay > 0) ? delay : 100;
		int bufferSize = (buffer != null && buffer > 0) ? buffer : 16;
		AtomicLong totalBytes = new AtomicLong(0);
		Flux<DataBuffer> bodyFlux = httpRequest.getBody()
		.concatMap(incomingBuffer -> {
			List<DataBuffer> chunks = new ArrayList<>();
			try {
				while (incomingBuffer.readableByteCount() > 0) {
					int chunkSize = Math.min(bufferSize, incomingBuffer.readableByteCount());
					byte[] chunkBytes = new byte[chunkSize];
					incomingBuffer.read(chunkBytes);
					chunks.add(DefaultDataBufferFactory.sharedInstance.wrap(chunkBytes));
				}
			} finally {
				DataBufferUtils.release(incomingBuffer);
			}
			return Flux.fromIterable(chunks);
		})
		.delayElements(Duration.ofMillis(sleep)) // simulasi proses write yang lambat
		.doOnNext(chunk -> totalBytes.addAndGet(chunk.readableByteCount()));
		Mono<DataBuffer> endStreamingMono = Mono.fromSupplier(() -> {
			String message = "\n\nTotal Bytes: " + totalBytes.get();
			byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
			return DefaultDataBufferFactory.sharedInstance.wrap(bytes);
		});
		return Flux.concat(bodyFlux, endStreamingMono);
	}
	
    @PostMapping(value = "/multipart")
	public Flux<Result> multipart(
		@RequestPart(name = "name") String name,
		@RequestPart(name = "file", required = false) FilePart file
	) {
    	if (file != null) {
    		return file
    		.content()
    		.subscribeOn(WebFluxHelper.getScheduler())
    		.flatMap(dataBuffer -> {
    			byte[] bytes = WebFluxHelper.getByteArray(dataBuffer);
		        Result result = Result.success()
		        .setInfo("name", name)
		        .setInfo("length", bytes.length)
		        .setInfo("filename", file.filename());
		        return Flux.just(result);
    		});
    	} else {
    		Result result = Result.success()
			.setInfo("name", name);
			return Flux.just(result);
    	}
	}
	
	@GetMapping("/logger")
	public void logger() {
		Throwable throwable = new Exception(StringHelper.format("EXCEPTION: {}", System.nanoTime() + ""));
		log.debug("{}", StringHelper.lazy(() -> "DEBUG"), throwable);
		log.trace("{}", StringHelper.lazy(() -> "TRACE"), throwable);
		log.info("{}", StringHelper.lazy(() -> "INFO-" + System.nanoTime()), throwable);
		log.warn("{}", StringHelper.lazy(() -> "WARN"), throwable);
		log.error("{}", StringHelper.lazy(() -> "ERROR"), throwable);
	}
	
}
