package com.multiclinicas.api.services;

public interface EmailService {
	void enviarEmail(String destinatario, String assunto, String mensagem);
}