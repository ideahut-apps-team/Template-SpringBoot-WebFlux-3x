package net.ideahut.springboot.template.controller.test;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.mail.internet.InternetAddress;
import lombok.Getter;
import lombok.Setter;
import net.ideahut.springboot.annotation.Public;
import net.ideahut.springboot.helper.ErrorHelper;
import net.ideahut.springboot.helper.StringHelper;
import net.ideahut.springboot.helper.WebFluxHelper;
import net.ideahut.springboot.mail.MailHandler;
import net.ideahut.springboot.mail.MailObject;
import net.ideahut.springboot.mail.MailObject.Attachment;
import net.ideahut.springboot.object.Result;
import reactor.core.publisher.Mono;

/*
 * Contoh penggunaan MailHandler
 */
@Public
@ComponentScan
@RestController
@RequestMapping("/test/mail")
class MailController {

	private final MailHandler mailHandler;
	
	@Autowired
	MailController(
		MailHandler mailHandler	
	) {
		this.mailHandler = mailHandler;
	}
	
	@Setter
	@Getter
	static class Form {
		private String from;
		private List<String> to;
		private List<String> cc;
		private List<String> bcc;
		private String subject;
		private String content;
		private FilePart attachment;
	}
	
	@PostMapping("/send/sync")
	public Mono<Result> sendSync(@ModelAttribute Form form) {
		return sendMail(form, false);
	}
	
	@PostMapping("/send/async")
	public Mono<Result> sendAsync(@ModelAttribute Form form) {
		return sendMail(form, true);
	}
	
	private InternetAddress toInternetAddress(
		String address, 
		String personal
	) {
		try {
			return new InternetAddress(address, personal);
		} catch (Exception e) {
			throw ErrorHelper.exception(e);
		}
	}
	
	private MailObject createMail(Form form) {
		MailObject mail = new MailObject();
		mail.setSubject(!StringHelper.isBlank(form.getSubject()) ? form.getSubject() : "Test-Mail");
		mail.setHtmlText(!StringHelper.isBlank(form.getContent()) ? form.getContent() : "Ini adalah contoh email");
		if (!StringHelper.isBlank(form.getFrom())) {
			mail.setFrom(toInternetAddress(form.getFrom(), form.getFrom()));
		}
		if (form.getTo() != null && !form.getTo().isEmpty()) {
			List<InternetAddress> lto = form.getTo()
			.stream()
			.filter(email -> !StringHelper.isBlank(email))
			.map(email -> toInternetAddress(email, email))
			.toList();
			mail.setTo(lto.toArray(new InternetAddress[0]));
		}
		if (form.getCc() != null && !form.getCc().isEmpty()) {
			List<InternetAddress> lcc = form.getCc()
			.stream()
			.filter(email -> !StringHelper.isBlank(email))
			.map(email -> toInternetAddress(email, email))
			.toList();
			mail.setCc(lcc.toArray(new InternetAddress[0]));
		}
		if (form.getBcc() != null && !form.getBcc().isEmpty()) {
			List<InternetAddress> lbcc = form.getBcc()
			.stream()
			.filter(email -> !StringHelper.isBlank(email))
			.map(email -> toInternetAddress(email, email))
			.toList();
			mail.setBcc(lbcc.toArray(new InternetAddress[0]));
		}
		return mail;
	}
	
	private Mono<Result> sendMail(Form form, boolean async) {
		if (form.getAttachment() != null) {
			return WebFluxHelper
			.fromDataBuffer(form.getAttachment().content())
			.flatMap(bytes -> {
				MailObject mail = createMail(form);
				Attachment attachment = Attachment.of("Attachment", bytes, form.getAttachment().headers().getContentType().toString());
		        mail
		        .setMultipart(true)
		        .setAttachment(attachment);
		        mailHandler.send(mail, async);
		        Result result = Result.success()
				.setInfo("name", form.getAttachment().name())
				.setInfo("fileName", form.getAttachment().filename())
				.setInfo("contentType", form.getAttachment().headers().getContentType().toString())
				.setInfo("contentLength", bytes.length);
		        form.getAttachment().delete(); // delete attachment from storage
				return Mono.just(result);
			});
		} else {
			return WebFluxHelper.fromCallable(() -> {
				MailObject mail = createMail(form);
				mailHandler.send(mail, async);
				return Result.success();
			});
		}
	}
	
}
