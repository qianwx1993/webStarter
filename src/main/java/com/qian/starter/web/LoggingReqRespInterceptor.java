package com.qian.starter.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author Qian
 * @Version 1.0
 * @Since JDK1.8
 * @Company Bangsun
 * @Date 2021/2/5 10:23
 */
@Slf4j
public class LoggingReqRespInterceptor implements ClientHttpRequestInterceptor {

	public LoggingReqRespInterceptor() {
	}
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
		log.info("######，来自于{}","com.qian.web.starter");
		String traceId = UUID.randomUUID().toString();
		this.loggingRequest(traceId, request, body);
		StopWatch sw = new StopWatch("HttpRequestWatcher");
		sw.start();
		ClientHttpResponse response = clientHttpRequestExecution.execute(request, body);
		sw.stop();
		HttpResponseWrapper responseWrapper = new HttpResponseWrapper(traceId, response);
		this.loggingResponose(traceId, responseWrapper, sw.getTotalTimeMillis());
		return responseWrapper;
	}

	private void loggingResponose(String traceId, ClientHttpResponse response, long totalTimeMillis) {
		log.info("http request id {} is completed, total elasped {} ms, response is {}.", new Object[]{traceId, totalTimeMillis, response});
	}

	private void loggingRequest(String traceId, HttpRequest request, byte[] body) {
		if (null != body && body.length > 0) {
			log.info("http request id {} is kickoff, request is {}, body is {}.", new Object[]{traceId, new HttpRequestPrinter(traceId, request), new String(body, StandardCharsets.UTF_8)});
		} else {
			log.info("http request id {} is kickoff, request is {}.", traceId, new HttpRequestPrinter(traceId, request));
		}

	}
}
