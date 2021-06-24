package org.itech.fhircore.util;

import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class URIUtil {

	public static URI createHttpUrlFromString(String url) {
		log.debug("creating http url from \"" + url + "\"");
		if (!url.startsWith("http://")) {
			url = "http://" + url;
		}
		return UriComponentsBuilder.fromUriString(url)
				.scheme("http")
				.build().toUri();
	}

	public static URI appendPathToHttpUrl(URI httpUrl, String pathSuffix) {
		log.debug("appending \"" + pathSuffix + "\" to \"" + httpUrl.getPath() + "\"");
		return UriComponentsBuilder.fromUri(httpUrl)
				.path(pathSuffix)
				.build().toUri();
	}

	public static URI addQueryParametersToHttpUrl(URI httpUrl, Map<String, String> queryParameters) {
		MultiValueMap<String, String> multiValueMapParameters = new LinkedMultiValueMap<>();
		for (Entry<String, String> entry : queryParameters.entrySet()) {
			multiValueMapParameters.add(entry.getKey(), entry.getValue());
		}

		return UriComponentsBuilder.fromUri(httpUrl)
				.queryParams(multiValueMapParameters)
				.build().toUri();
	}

	public static URI setQueryParametersToHttpUrl(URI httpUrl, Map<String, String> queryParameters) {
		MultiValueMap<String, String> multiValueMapParameters = new LinkedMultiValueMap<>();
		multiValueMapParameters.setAll(queryParameters);

		return UriComponentsBuilder.fromUri(httpUrl)
				.queryParams(multiValueMapParameters)
				.build().toUri();
	}

	public static URI appendPathAndAddQueryParametersToHttpUrl(URI httpUrl, String pathSuffix,
			Map<String, String> queryParameters) {
		MultiValueMap<String, String> multiValueMapParameters = new LinkedMultiValueMap<>();
		for (Entry<String, String> entry : queryParameters.entrySet()) {
			multiValueMapParameters.add(entry.getKey(), entry.getValue());
		}

		return UriComponentsBuilder.fromUri(httpUrl).queryParams(multiValueMapParameters)
				.path(pathSuffix)
				.build().toUri();
	}

	public static URI appendPathAndSetQueryParametersToHttpUrl(URI httpUrl, String pathSuffix,
			Map<String, String> queryParameters) {
		MultiValueMap<String, String> multiValueMapParameters = new LinkedMultiValueMap<>();
		multiValueMapParameters.setAll(queryParameters);

		return UriComponentsBuilder.fromUri(httpUrl)
				.queryParams(multiValueMapParameters)
				.path(pathSuffix)
				.build().toUri();
	}

}
