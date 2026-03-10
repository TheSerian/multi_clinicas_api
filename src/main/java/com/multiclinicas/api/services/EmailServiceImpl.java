package com.multiclinicas.api.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {
	
	private final JavaMailSender mailSender;
	private final String remetentePadrao = "nao-responda@clinicas.com";
	
	@Autowired
	public EmailServiceImpl(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}
	
	@Async
	@Override
	public void enviarEmail(String destinatario, String assunto, String mensagem) {
		try {
			SimpleMailMessage email = new SimpleMailMessage();
			email.setFrom(remetentePadrao);
			email.setTo(destinatario);
			email.setSubject(assunto);
			email.setText(mensagem);
			
			mailSender.send(email);
			System.out.println("E-mail enviado com sucesso.");
		} catch (Exception e) {
			System.err.println("Erro ao tentar enviar o e-mail.");
			e.printStackTrace();
		}
	}
}