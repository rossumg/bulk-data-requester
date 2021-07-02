package org.itech.fhirhose.config;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfig {
	HttpClientConfigProperties properties;

	public HttpClientConfig(HttpClientConfigProperties properties) {
		this.properties = properties;
	}

    @Bean
    public CloseableHttpClient httpClient() throws Exception {
        return HttpClientBuilder.create().setSSLSocketFactory(sslConnectionSocketFactory()).build();
    }

    public SSLConnectionSocketFactory sslConnectionSocketFactory() throws Exception {
        return new SSLConnectionSocketFactory(sslContext());
    }

    public SSLContext sslContext() throws Exception {
        return SSLContextBuilder.create()
				.loadKeyMaterial(properties.getKeyStore().getFile(), properties.getKeyStorePassword().toCharArray(),
						properties.getKeyPassword().toCharArray())
				.loadTrustMaterial(properties.getTrustStore().getFile(),
						properties.getTrustStorePassword().toCharArray())
				.build();
    }
}
