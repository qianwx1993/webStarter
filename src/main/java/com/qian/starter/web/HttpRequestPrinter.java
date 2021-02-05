package com.qian.starter.web;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpRequest;

/**
 * @author Qian
 * @Version 1.0
 * @Since JDK1.8
 * @Company Bangsun
 * @Date 2021/2/5 10:38
 */
public class HttpRequestPrinter {
	private HttpRequest innerRequest;
	private String requestString;
	private String traceId;

	public HttpRequestPrinter(String tarceid, HttpRequest request) {
		this.traceId = tarceid;
		this.innerRequest = request;
	}

	@Override
	public String toString() {
		if (StringUtils.isBlank(this.requestString)) {
			StringBuilder builder = new StringBuilder();
			builder.append("Method:'").append(this.innerRequest.getMethodValue()).append("'");
			builder.append(" URL:'").append(this.innerRequest.getURI()).append("'");
			if (null != this.innerRequest.getHeaders() && !this.innerRequest.getHeaders().isEmpty()) {
				builder.append("Headers:'").append(this.innerRequest.getHeaders()).append("'");
			}

			this.requestString = builder.toString();
		}

		return this.requestString;
	}
}
