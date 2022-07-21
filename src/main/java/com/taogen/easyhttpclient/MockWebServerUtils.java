package com.taogen.easyhttpclient;

import com.taogen.commons.collection.MapUtils;
import com.taogen.commons.network.HttpRequestUtil;
import com.taogen.easyhttpclient.vo.HttpRequest;
import com.taogen.easyhttpclient.vo.HttpRequestWithForm;
import com.taogen.easyhttpclient.vo.HttpRequestWithJson;
import com.taogen.easyhttpclient.vo.HttpRequestWithMultipart;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.taogen.commons.collection.MapUtils.multiValueMapEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Taogen
 */
@Log4j2
public class MockWebServerUtils {
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";
    /**
     * @param mockWebServer
     * @param responseBody
     * @param contentType   For example, "application/json"
     */
    public static void enqueueMockedResponse(MockWebServer mockWebServer, byte[] responseBody, String contentType) {
        Buffer buffer = new Buffer();
        buffer.write(responseBody);
        MockResponse mockedResponse = new MockResponse()
                .setBody(buffer)
                .addHeader(CONTENT_TYPE, contentType)
                .addHeader(CONTENT_LENGTH, responseBody.length);
        mockWebServer.enqueue(mockedResponse);
    }

    /**
     * @param mockWebServer
     * @param requestUri
     * @return http://127.0.0.1:{randomNumber}/
     */
    public static String getMockedUrlByUri(MockWebServer mockWebServer, String requestUri) {
        return mockWebServer.url(requestUri).toString();
    }

    /**
     * validate request
     * <p>
     * - url
     * - method
     * - headers
     * - query string params
     * - body
     *
     * @param mockedRealRequest
     * @param okHttpRequest
     */
    public static void validateRequestWithQueryString(RecordedRequest mockedRealRequest, HttpRequest okHttpRequest) {
        log.debug("mocked real request: {}", mockedRealRequest);
        String actualUrl = mockedRealRequest.getRequestUrl().toString();
        if (actualUrl.indexOf("?") >= 0) {
            actualUrl = actualUrl.substring(0, actualUrl.indexOf("?"));
        }
        // validate URL
        assertEquals(okHttpRequest.getUrl(), actualUrl);
        // validate method
        assertEquals(okHttpRequest.getMethod().name(), mockedRealRequest.getMethod());
        // validate headers
        log.debug("okHttpRequest header: {}", okHttpRequest.getHeaders());
        log.debug("mockedRealRequest header: {}", mockedRealRequest.getHeaders().toMultimap());
        assertTrue(MapUtils.multiStringValueMapContains(mockedRealRequest.getHeaders().toMultimap(),
                MapUtils.multiObjectValueMapToMultiStringValueMap(okHttpRequest.getHeaders())));
        // validate query string params
        Map<String, List<Object>> actualQueryStringParams = getQueryParamMapByRecordedRequest(mockedRealRequest);
        log.debug("okHttpRequest query param: {}", okHttpRequest.getQueryParams());
        log.debug("mockedRealRequest query param: {}", actualQueryStringParams);
        assertTrue(multiValueMapEquals(okHttpRequest.getQueryParams(), actualQueryStringParams));
        // validate body
    }

    public static Map<String, List<Object>> getQueryParamMapByRecordedRequest(RecordedRequest recordedRequest) {
        HttpUrl requestUrl = recordedRequest.getRequestUrl();
        return requestUrl.queryParameterNames().stream()
                .collect(HashMap::new, (map, key) -> map.put(key, new ArrayList<>(requestUrl.queryParameterValues(key))), Map::putAll);
    }

    public static void validateRequestWithJson(RecordedRequest mockedRealRequest,
                                               HttpRequestWithJson okHttpRequestWithJson) {
        validateRequestWithQueryString(mockedRealRequest, okHttpRequestWithJson);
        // validate body
        log.debug("okHttpRequestWithJson body: {}", okHttpRequestWithJson.getJson());
        String actualBody = mockedRealRequest.getBody().readUtf8();
        log.debug("mockedRealRequest body: {}", actualBody);
        assertEquals(okHttpRequestWithJson.getJson(), actualBody);
    }

    public static void validateRequestWithUrlEncodedForm(RecordedRequest mockedRealRequest, HttpRequestWithForm okHttpRequestWithFormData) {
        validateRequestWithQueryString(mockedRealRequest, okHttpRequestWithFormData);
        // validate body
        log.debug("okHttpRequestWithFormData formData: {}", okHttpRequestWithFormData.getFormData());
        String actualFormData = mockedRealRequest.getBody().readUtf8();
        String contentType = mockedRealRequest.getHeader(CONTENT_TYPE);
        log.debug("content type: {}", contentType);
        log.debug("mockedRealRequest formData: {}", actualFormData);
        LinkedHashMap<String, List<Object>> mockedFormDataMap = HttpRequestUtil.queryStringToMultiValueMap(actualFormData);
        log.debug("mockedRealRequest formDataMap: {}", mockedFormDataMap);
        Map<String, List<Object>> requestFormDataMap = okHttpRequestWithFormData.getFormData();
        log.debug("okHttpRequestWithFormData formDataMap: {}", requestFormDataMap);
        assertTrue(MapUtils.multiValueMapEquals(requestFormDataMap, mockedFormDataMap));
    }

    public static void validateRequestWithMultipartForm(RecordedRequest mockedRealRequest,
                                                        HttpRequestWithMultipart okHttpRequestWithFormData) {
        validateRequestWithQueryString(mockedRealRequest, okHttpRequestWithFormData);
        // validate body
        log.debug("okHttpRequestWithFormData formData: {}", okHttpRequestWithFormData.getFormData());
        String actualFormData = mockedRealRequest.getBody().readUtf8();
        String contentType = mockedRealRequest.getHeader(CONTENT_TYPE);
        log.debug("content type: {}", contentType);
        log.debug("mockedRealRequest formData: {}", actualFormData);
        LinkedHashMap<String, List<Object>> mockedFormDataMap = HttpRequestUtil.multipartDataToMultiValueMap(
                actualFormData.getBytes(StandardCharsets.UTF_8), "--" + HttpRequestUtil.getBoundaryByContentType(contentType));
        log.debug("mockedRealRequest formDataMap: {}", mockedFormDataMap);
        Map<String, List<Object>> requestFormDataMap = okHttpRequestWithFormData.getFormData();
        log.debug("okHttpRequestWithFormData formDataMap: {}", requestFormDataMap);
        assertTrue(MapUtils.multiValueMapEquals(requestFormDataMap, mockedFormDataMap));
    }

}
