package com.qian.starter.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author Qian
 * @Version 1.0
 * @Since JDK1.8
 * @Company Bangsun
 * @Date 2021/2/5 10:24
 */
@Slf4j
public class HttpResponseWrapper implements ClientHttpResponse{
	private final ClientHttpResponse response;
	private byte[] body;
	private final String traceId;

	public HttpResponseWrapper(String traceId, ClientHttpResponse response) {
		this.traceId = traceId;
		this.response = response;

		try {
			this.body = StreamUtils.copyToByteArray(this.response.getBody());
		} catch (IOException var4) {
			log.error("IOException is thrown while processing response body of http request id {}.", traceId);
		}

	}

	public HttpStatus getStatusCode() throws IOException {
		return this.response.getStatusCode();
	}

	public int getRawStatusCode() throws IOException {
		return this.response.getRawStatusCode();
	}

	public String getStatusText() throws IOException {
		return this.response.getStatusText();
	}

	public HttpHeaders getHeaders() {
		return this.response.getHeaders();
	}

	public InputStream getBody() throws IOException {
		return new ByteArrayInputStream(this.body);
	}

	public void close() {
		this.response.close();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		try {
			builder.append("Status Code:'").append(this.getStatusCode()).append("' ");
			builder.append("Status Text:'").append(this.getStatusText()).append("'");
			builder.append("Body:").append(new String(this.body, StandardCharsets.UTF_8));
		} catch (IOException var3) {
			log.error("IOException is thrown while processing response of http request id {}.", this.traceId);
			return "RESPONSE PRINT ERROR.";
		}

		return builder.toString();
	}
}
