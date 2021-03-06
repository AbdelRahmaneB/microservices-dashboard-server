package be.ordina.msdashboard.nodes.aggregators;

import static com.google.common.collect.Lists.newArrayList;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.client.HttpRequestHeaders;
import io.reactivex.netty.protocol.http.client.HttpResponseHeaders;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import rx.Observable;
import rx.observers.TestSubscriber;

import com.google.common.collect.Lists;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RxNetty.class})
public class NettyServiceCallerTest {

	@InjectMocks
	private NettyServiceCaller nettyServiceCaller;
	
	@Mock
	private ErrorHandler errorHandler;
	
    @Before
    public void setUp() {
        PowerMockito.mockStatic(RxNetty.class);
    }
    
	@SuppressWarnings("unchecked")
	@Test
	public void badStatusCode(){
		HttpClientRequest<ByteBuf> request = mock(HttpClientRequest.class);
		when(request.getUri()).thenReturn("http://someUri.com");
		
		HttpClientResponse<ByteBuf> response = mock(HttpClientResponse.class);
		when(response.getStatus()).thenReturn(HttpResponseStatus.BAD_REQUEST);
		
		HttpResponseHeaders httpResponseHeaders = mock(HttpResponseHeaders.class);
		when(httpResponseHeaders.entries()).thenReturn(newArrayList());
		when(response.getHeaders()).thenReturn(httpResponseHeaders);
		
		Observable<HttpClientResponse<ByteBuf>> observable = Observable.just(response);
		when(RxNetty.createHttpRequest(request)).thenReturn(observable);
		
        TestSubscriber<Map<String, Object>> testSubscriber = new TestSubscriber<>();
		nettyServiceCaller.retrieveJsonFromRequest("serviceId", request).toBlocking().subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        
        verify(errorHandler).handleNodeWarning(Mockito.eq("serviceId"), Mockito.anyString());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void shouldReturnMapOfTwo(){
		String carJson ="{ \"brand\" : \"Mercedes\", \"doors\" : 5 }";
		
		HttpClientRequest<ByteBuf> request = mock(HttpClientRequest.class);
		
		HttpClientResponse<ByteBuf> response = mock(HttpClientResponse.class);
		when(response.getStatus()).thenReturn(OK);
		HttpResponseHeaders httpResponseHeaders = mock(HttpResponseHeaders.class);
		when(httpResponseHeaders.entries()).thenReturn(newArrayList());
		when(response.getHeaders()).thenReturn(httpResponseHeaders);
        ByteBuf byteBuf = (new PooledByteBufAllocator()).directBuffer();
        ByteBufUtil.writeUtf8(byteBuf, carJson);
		when(response.getContent()).thenReturn(Observable.just(byteBuf));
		
		Observable<HttpClientResponse<ByteBuf>> observable = Observable.just(response);
		when(RxNetty.createHttpRequest(request)).thenReturn(observable);
		
        TestSubscriber<Map<String, Object>> testSubscriber = new TestSubscriber<>();
		nettyServiceCaller.retrieveJsonFromRequest("serviceId", request).toBlocking().subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        
        List<Map<String, Object>> json = testSubscriber.getOnNextEvents();
        
        assertThat(json.size()).isEqualTo(1);
        assertThat(json.get(0)).containsKey("brand");
        assertThat(json.get(0).get("brand")).isEqualTo("Mercedes");
        assertThat(json.get(0)).containsKey("doors");
        assertThat(json.get(0).get("doors")).isEqualTo(5);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void httpRequestReturnsErroneousObservable(){
		HttpClientRequest<ByteBuf> request = mock(HttpClientRequest.class);
		when(request.getUri()).thenReturn("http://someUri.com");
		HttpRequestHeaders httpRequestHeaders = mock(HttpRequestHeaders.class);
		when(httpRequestHeaders.entries()).thenReturn(Lists.newArrayList());
		when(request.getHeaders()).thenReturn(httpRequestHeaders);
		
        when(RxNetty.createHttpRequest(request))
     		.thenReturn(Observable.error(new RuntimeException()));
        
        TestSubscriber<Map<String, Object>> testSubscriber = new TestSubscriber<>();
		nettyServiceCaller.retrieveJsonFromRequest("serviceId", request).toBlocking().subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        
        verify(errorHandler).handleNodeError(Mockito.eq("serviceId"), Mockito.anyString(), Mockito.any(RuntimeException.class));
	}
}
