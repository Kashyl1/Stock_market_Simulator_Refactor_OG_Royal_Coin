package com.tradingsimulator.backend.currency.provider.nbp;

import java.net.http.HttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.tradingsimulator.backend.currency.FxProperties;

@Configuration(proxyBeanMethods = false)
public class NbpClientConfig {

	@Bean
	RestClient nbpRestClient(RestClient.Builder builder, FxProperties properties) {
		FxProperties.Nbp nbp = properties.nbp();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(nbp.connectTimeout()).build());
		requestFactory.setReadTimeout(nbp.readTimeout());
		return builder.baseUrl(nbp.baseUrl()).requestFactory(requestFactory).build();
	}
}
