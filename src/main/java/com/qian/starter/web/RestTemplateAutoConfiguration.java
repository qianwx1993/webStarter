package com.qian.starter.web;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Qian
 * @Version 1.0
 * @Since JDK1.8
 * @Company Bangsun
 * @Date 2021/2/5 10:03
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({HttpClientProperties.class})
public class RestTemplateAutoConfiguration {
	@Autowired
	private HttpClientProperties httpClientProperties;

	public RestTemplateAutoConfiguration() {
	}

	@Bean
	public RestTemplate restTemplate() {
		RestTemplate template = new RestTemplate(this.httpRequestFactory());
		template.getInterceptors().add(new LoggingReqRespInterceptor());
		return template;
	}

	@Bean
	public ClientHttpRequestFactory httpRequestFactory() {
		return new HttpComponentsClientHttpRequestFactory(this.httpClient());
	}

	@Bean
	public HttpClient httpClient() {
		Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", SSLConnectionSocketFactory.getSocketFactory()).build();
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
		connectionManager.setMaxTotal(this.httpClientProperties.getMaxTotal());
		connectionManager.setDefaultMaxPerRoute(this.httpClientProperties.getDefaultMaxPerRoute());
		connectionManager.setValidateAfterInactivity(this.httpClientProperties.getValidateAfterInactivity());
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(this.httpClientProperties.getSocketTimeout()).setConnectTimeout(this.httpClientProperties.getConnectTimeout()).setConnectionRequestTimeout(this.httpClientProperties.getConnectionRequestTimeout()).build();
		HttpClientBuilder clientBuilder = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).setConnectionManager(connectionManager);

		try {
			SSLContext sslContext = (new SSLContextBuilder()).loadTrustMaterial((KeyStore)null, new TrustStrategy() {
				public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
					return true;
				}
			}).build();
			SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, new String[]{"TLSv1"}, (String[])null, NoopHostnameVerifier.INSTANCE);
			clientBuilder.setConnectionManager(connectionManager).setSSLSocketFactory(csf);
			clientBuilder.setKeepAliveStrategy(this.connectionKeepAliveStrategy2());
		} catch (KeyStoreException | KeyManagementException | NoSuchAlgorithmException var7) {
			log.error("SSL context configuring failed, HTTPS cannot be used in RestTemplate.", var7);
		}

		return clientBuilder.build();
	}

	/**
	 * 金投的方法，e.getKey()出现报错
	 * @return
	 */
	/*public ConnectionKeepAliveStrategy connectionKeepAliveStrategy() {
		return (response, context) -> {
			BasicHeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator("Keep-Alive"));

			while(true) {
				String param;
				String value;
				do {
					do {
						if (!it.hasNext()) {
							HttpHost target = (HttpHost)context.getAttribute(HttpClientContext.HTTP_TARGET_HOST);
							Optional<Map.Entry<String, Integer>> any = ((Map)Optional.ofNullable(this.httpClientProperties.getKeepAliveTargetHost()).orElseGet(HashMap::new)).entrySet().stream().filter((e) -> {
								return ((String)e.getKey()).equalsIgnoreCase(target.getHostName());
							}).findAny();
							return (Long)any.map((en) -> {
								return (long)(Integer)en.getValue() * 1000L;
							}).orElse(this.httpClientProperties.getKeepAliveTime() * 1000L);
						}

						HeaderElement he = it.nextElement();
						log.info("HeaderElement:{}", JSON.toJSONString(he));
						param = he.getName();
						value = he.getValue();
					} while(value == null);
				} while(!"timeout".equalsIgnoreCase(param));

				try {
					return Long.parseLong(value) * 1000L;
				} catch (NumberFormatException var8) {
					log.error("Error occurs while parsing timeout settings of keep-alived connection.", var8);
				}
			}
		};
	}*/

	public ConnectionKeepAliveStrategy connectionKeepAliveStrategy2(){
		return (response, context) -> {
			// Honor 'keep-alive' header
			HeaderElementIterator it = new BasicHeaderElementIterator(
					response.headerIterator(HTTP.CONN_KEEP_ALIVE));
			while (it.hasNext()) {
				HeaderElement he = it.nextElement();
				log.info("HeaderElement:{}", JSON.toJSONString(he));
				String param = he.getName();
				String value = he.getValue();
				if (value != null && "timeout".equalsIgnoreCase(param)) {
					try {
						return Long.parseLong(value) * 1000;
					} catch(NumberFormatException ignore) {
						log.error("解析长连接过期时间异常",ignore);
					}
				}
			}
			HttpHost target = (HttpHost) context.getAttribute(
					HttpClientContext.HTTP_TARGET_HOST);
			//如果请求目标地址,单独配置了长连接保持时间,使用该配置
			Optional<Map.Entry<String, Integer>> any = Optional.ofNullable(httpClientProperties.getKeepAliveTargetHost()).orElseGet(HashMap::new)
					.entrySet().stream().filter(
							e -> e.getKey().equalsIgnoreCase(target.getHostName())).findAny();
			//否则使用默认长连接保持时间
			return any.map(en -> en.getValue() * 1000L).orElse(httpClientProperties.getKeepAliveTime() * 1000L);
		};
	}
	public static void main(String[] args) {
		System.out.println("###");
	}
}
