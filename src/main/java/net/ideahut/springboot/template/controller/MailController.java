package net.ideahut.springboot.template.controller;


import java.util.ArrayList;
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
import net.ideahut.springboot.annotation.Body;
import net.ideahut.springboot.annotation.Public;
import net.ideahut.springboot.exception.ResultRuntimeException;
import net.ideahut.springboot.helper.ObjectHelper;
import net.ideahut.springboot.helper.StringHelper;
import net.ideahut.springboot.helper.WebFluxHelper;
import net.ideahut.springboot.mail.MailHandler;
import net.ideahut.springboot.mail.MailObject;
import net.ideahut.springboot.mail.MailObject.Attachment;
import net.ideahut.springboot.object.Result;
import reactor.core.publisher.Flux;

/*
 * Contoh penggunaan MailHandler
 */
@Public
@ComponentScan
@RestController
@RequestMapping("/mail")
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
	
	@Body
	@PostMapping("/send/sync")
	Flux<Result> sendSync(@ModelAttribute Form form) {
		return sendMail(form, false);
	}
	
	@Body
	@PostMapping("/send/async")
	Flux<Result> sendAsync(@ModelAttribute Form form) {
		return sendMail(form, true);
	}
	
	private MailObject createMail(Form form) {
		MailObject mail = new MailObject();
		String subject = ObjectHelper.useOrDefault(form.getSubject(), "");
		subject = ObjectHelper.useOrElse(!subject.trim().isEmpty(), subject, "Test-Mail");
		mail.setSubject(subject);
		String content = ObjectHelper.useOrDefault(form.getContent(), "");
		content = ObjectHelper.useOrElse(!content.trim().isEmpty(), content, "Ini adalah contoh email");
		mail.setHtmlText(content);
		String sender = ObjectHelper.useOrDefault(form.getFrom(), "").trim();
		ObjectHelper.callIf(!StringHelper.isBlank(sender), () -> mail.setFrom(new InternetAddress(sender, sender)));
		ObjectHelper.callIf(
			form.getTo() != null && !form.getTo().isEmpty(), 
			() -> {
				List<InternetAddress> to = new ArrayList<>();
				for (String email : form.getTo()) {
					to.add(new InternetAddress(email, email));
				}
				return mail.setTo(to.toArray(new InternetAddress[0]));
			}
		);
		ObjectHelper.callIf(
			form.getCc() != null && !form.getCc().isEmpty(), 
			() -> {
				List<InternetAddress> cc = new ArrayList<>();
				for (String email : form.getCc()) {
					cc.add(new InternetAddress(email, email));
				}
				return mail.setCc(cc.toArray(new InternetAddress[0]));
			}
		);
		ObjectHelper.callIf(
			form.getBcc() != null && !form.getBcc().isEmpty(), 
			() -> {
				List<InternetAddress> bcc = new ArrayList<>();
				for (String email : form.getBcc()) {
					bcc.add(new InternetAddress(email, email));
				}
				return mail.setBcc(bcc.toArray(new InternetAddress[0]));
			}
		);
		return mail;
	}
	
	private Flux<Result> sendMail(Form form, boolean async) {
		FilePart filePart = form.getAttachment();
		return ObjectHelper.callOrElse(
			filePart == null, 
			() -> Flux.just(Result.error("MAIL-0", "attachment required")), 
			() -> filePart.content()
			.flatMap(dataBuffer -> {
				try {
					byte[] bytes = WebFluxHelper.getDataBufferAsBytes(dataBuffer);
					Attachment attachment = Attachment.of("Attachment", bytes, filePart.headers().getContentType().toString());
					MailObject mail = createMail(form)
					.setMultipart(true)
					.setAttachment(attachment);
					mailHandler.send(mail, async);
					return Flux.just(Result.success());
				} catch (Exception e) {
					return Flux.just(ResultRuntimeException.of(e).getResult());
				}
            })
		);
	}
	
}
