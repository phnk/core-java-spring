package eu.arrowhead.core.gateway.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.dto.CloudRequestDTO;
import eu.arrowhead.common.dto.GatewayProviderConnectionRequestDTO;
import eu.arrowhead.common.dto.RelayRequestDTO;
import eu.arrowhead.common.dto.RelayType;
import eu.arrowhead.common.dto.SystemRequestDTO;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.core.gateway.relay.GatewayRelayClient;

@RunWith(SpringRunner.class)
public class GatewayServiceTest {

	//=================================================================================================
	// members
	
	@InjectMocks
	private GatewayService testingObject;
	
	@Mock
	private Map<String,Object> arrowheadContext;
	
	@Spy
	private ApplicationContext appContext;
	
	private GatewayRelayClient relayClient;

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	@Before
	public void setUp() {
		relayClient = mock(GatewayRelayClient.class, "relayClient");
		ReflectionTestUtils.setField(testingObject, "relayClient", relayClient);
		ReflectionTestUtils.setField(testingObject, "gatewaySocketTimeout", 60000);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = ArrowheadException.class)
	public void testOnApplicationEventNoCommonName() {
		when(arrowheadContext.containsKey(CommonConstants.SERVER_COMMON_NAME)).thenReturn(false);
		testingObject.onApplicationEvent(null);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = ClassCastException.class)
	public void testOnApplicationEventCommonNameWrongType() {
		when(arrowheadContext.containsKey(CommonConstants.SERVER_COMMON_NAME)).thenReturn(true);
		when(arrowheadContext.get(CommonConstants.SERVER_COMMON_NAME)).thenReturn(new Object());
		testingObject.onApplicationEvent(null);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = ArrowheadException.class)
	public void testOnApplicationEventNoPublicKey() {
		when(arrowheadContext.containsKey(CommonConstants.SERVER_COMMON_NAME)).thenReturn(true);
		when(arrowheadContext.get(CommonConstants.SERVER_COMMON_NAME)).thenReturn("gateway.testcloud2.aitia.arrowhead.eu");
		when(arrowheadContext.containsKey(CommonConstants.SERVER_PUBLIC_KEY)).thenReturn(false);
		testingObject.onApplicationEvent(null);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = ClassCastException.class)
	public void testOnApplicationEventPublicKeyWrongType() {
		when(arrowheadContext.containsKey(CommonConstants.SERVER_COMMON_NAME)).thenReturn(true);
		when(arrowheadContext.get(CommonConstants.SERVER_COMMON_NAME)).thenReturn("gateway.testcloud2.aitia.arrowhead.eu");
		when(arrowheadContext.containsKey(CommonConstants.SERVER_PUBLIC_KEY)).thenReturn(true);
		when(arrowheadContext.get(CommonConstants.SERVER_PUBLIC_KEY)).thenReturn("not a public key");
		testingObject.onApplicationEvent(null);
	}
	
	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("serial")
	@Test(expected = ArrowheadException.class)
	public void testOnApplicationEventNoPrivateKey() {
		when(arrowheadContext.containsKey(CommonConstants.SERVER_COMMON_NAME)).thenReturn(true);
		when(arrowheadContext.get(CommonConstants.SERVER_COMMON_NAME)).thenReturn("gateway.testcloud2.aitia.arrowhead.eu");
		when(arrowheadContext.containsKey(CommonConstants.SERVER_PUBLIC_KEY)).thenReturn(true);
		when(arrowheadContext.get(CommonConstants.SERVER_PUBLIC_KEY)).thenReturn(new PublicKey() {
			public String getFormat() { return null; }
			public byte[] getEncoded() { return null; }
			public String getAlgorithm() { return null; }
		});
		when(arrowheadContext.containsKey(CommonConstants.SERVER_PRIVATE_KEY)).thenReturn(false);
		testingObject.onApplicationEvent(null);
	}
	
	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("serial")
	@Test(expected = ClassCastException.class)
	public void testOnApplicationEventPrivateKeyWrongType() {
		when(arrowheadContext.containsKey(CommonConstants.SERVER_COMMON_NAME)).thenReturn(true);
		when(arrowheadContext.get(CommonConstants.SERVER_COMMON_NAME)).thenReturn("gateway.testcloud2.aitia.arrowhead.eu");
		when(arrowheadContext.containsKey(CommonConstants.SERVER_PUBLIC_KEY)).thenReturn(true);
		when(arrowheadContext.get(CommonConstants.SERVER_PUBLIC_KEY)).thenReturn(new PublicKey() {
			public String getFormat() { return null; }
			public byte[] getEncoded() { return null; }
			public String getAlgorithm() { return null; }
		});
		when(arrowheadContext.containsKey(CommonConstants.SERVER_PRIVATE_KEY)).thenReturn(true);
		when(arrowheadContext.get(CommonConstants.SERVER_PRIVATE_KEY)).thenReturn("not a private key");
		testingObject.onApplicationEvent(null);
	}
	
	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("serial")
	@Test
	public void testOnApplicationEventEverythingOK() {
		when(arrowheadContext.containsKey(CommonConstants.SERVER_COMMON_NAME)).thenReturn(true);
		when(arrowheadContext.get(CommonConstants.SERVER_COMMON_NAME)).thenReturn("gateway.testcloud2.aitia.arrowhead.eu");
		when(arrowheadContext.containsKey(CommonConstants.SERVER_PUBLIC_KEY)).thenReturn(true);
		when(arrowheadContext.get(CommonConstants.SERVER_PUBLIC_KEY)).thenReturn(new PublicKey() {
			public String getFormat() { return null; }
			public byte[] getEncoded() { return null; }
			public String getAlgorithm() { return null; }
		});
		when(arrowheadContext.containsKey(CommonConstants.SERVER_PRIVATE_KEY)).thenReturn(true);
		when(arrowheadContext.get(CommonConstants.SERVER_PRIVATE_KEY)).thenReturn(new PrivateKey() {
			public String getFormat() { return null; }
			public byte[] getEncoded() { return null; }
			public String getAlgorithm() { return null;	}
		});
		testingObject.onApplicationEvent(null);
		final Object relayClient = ReflectionTestUtils.getField(testingObject, "relayClient");
		Assert.assertNotNull(relayClient);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderRequestNull() {
		testingObject.connectProvider(null);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderRelayNull() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.setRelay(null);
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderRelayAddressNull() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.getRelay().setAddress(null);
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderRelayAddressEmpty() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.getRelay().setAddress(" ");
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderRelayPortNull() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.getRelay().setPort(null);
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderRelayPortTooLow() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.getRelay().setPort(-192);
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderRelayPortTooHigh() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.getRelay().setPort(192426);
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderRelayTypeNull() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.getRelay().setType(null);
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderRelayTypeEmpty() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.getRelay().setType("\r\t");
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderRelayTypeInvalid1() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.getRelay().setType("invalid");
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderRelayTypeInvalid2() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.getRelay().setType(RelayType.GATEKEEPER_RELAY.name());
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderConsumerNull() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.setConsumer(null);
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderConsumerNameNull() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.getConsumer().setSystemName(null);
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderConsumerNameEmpty() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.getConsumer().setSystemName("");
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderConsumerAddressNull() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.getConsumer().setAddress(null);
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderConsumerAddressEmpty() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.getConsumer().setAddress(" ");
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderConsumerPortNull() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.getConsumer().setPort(null);
		
		testingObject.connectProvider(request);
	}

	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderConsumerPortTooLow() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.getConsumer().setPort(-192);
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderConsumerPortTooHigh() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.getConsumer().setPort(192426);
		
		testingObject.connectProvider(request);
	}
	
	// we skip the provider check tests because it uses the same method than consumer check
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderConsumerCloudNull() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.setConsumerCloud(null);
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderConsumerCloudOperatorNull() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.getConsumerCloud().setOperator(null);
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderConsumerCloudOperatorEmpty() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.getConsumerCloud().setOperator(" ");
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderConsumerCloudNameNull() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.getConsumerCloud().setName(null);
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderConsumerCloudNameEmpty() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.getConsumerCloud().setName("");
		
		testingObject.connectProvider(request);
	}
	
	// we skip the provider cloud check tests because it uses the same method than consumer cloud check
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderServiceDefinitionNull() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.setServiceDefinition(null);
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderServiceDefinitionEmpty() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.setServiceDefinition("");
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderConsumerGWPublicKeyNull() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.setConsumerGWPublicKey(null);
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testConnectProviderConsumerGWPublicKeyEmpty() {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		request.setConsumerGWPublicKey("\n\t\r");
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = ArrowheadException.class)
	public void testConnectProviderCannotConnectRelay() throws JMSException {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		when(relayClient.createConnection(any(String.class), anyInt())).thenThrow(new JMSException("test"));
		
		testingObject.connectProvider(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = ArrowheadException.class)
	public void testConnectProviderOtherRelayIssue() throws JMSException {
		final GatewayProviderConnectionRequestDTO request = getTestGatewayProviderConnectionRequestDTO();
		when(relayClient.createConnection(any(String.class), anyInt())).thenReturn(getTestSession());
		when(relayClient.isConnectionClosed(any(Session.class))).thenReturn(false);
		when(relayClient.initializeProviderSideRelay(any(Session.class), any(MessageListener.class))).thenThrow(new JMSException("test"));
		
		testingObject.connectProvider(request);
	}
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private GatewayProviderConnectionRequestDTO getTestGatewayProviderConnectionRequestDTO() {
		final RelayRequestDTO relay = new RelayRequestDTO("localhost", 1234, false, false, RelayType.GATEWAY_RELAY.name());
		final SystemRequestDTO consumer = new SystemRequestDTO();
		consumer.setSystemName("consumer");
		consumer.setAddress("abc.de");
		consumer.setPort(22001);
		consumer.setAuthenticationInfo("consAuth");
		final SystemRequestDTO provider = new SystemRequestDTO();
		provider.setSystemName("provider");
		provider.setAddress("fgh.de");
		provider.setPort(22002);
		provider.setAuthenticationInfo("provAuth");
		final CloudRequestDTO consumerCloud = new CloudRequestDTO();
		consumerCloud.setName("testcloud1");
		consumerCloud.setOperator("aitia");
		final CloudRequestDTO providerCloud = new CloudRequestDTO();
		providerCloud.setName("testcloud2");
		providerCloud.setOperator("elte");
		
		final String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAq5Jq4tOeFoLqxOqtYcujbCNZina3iuV9+/o8D1R9D0HvgnmlgPlqWwjDSxV7m7SGJpuc/rRXJ85OzqV3rwRHO8A8YWXiabj8EdgEIyqg4SOgTN7oZ7MQUisTpwtWn9K14se4dHt/YE9mUW4en19p/yPUDwdw3ECMJHamy/O+Mh6rbw6AFhYvz6F5rXYB8svkenOuG8TSBFlRkcjdfqQqtl4xlHgmlDNWpHsQ3eFAO72mKQjm2ZhWI1H9CLrJf1NQs2GnKXgHBOM5ET61fEHWN8axGGoSKfvTed5vhhX7l5uwxM+AKQipLNNKjEaQYnyX3TL9zL8I7y+QkhzDa7/5kQIDAQAB";
		
		return new GatewayProviderConnectionRequestDTO(relay, consumer, provider, consumerCloud, providerCloud, "test-service", publicKey);
	}

	//-------------------------------------------------------------------------------------------------
	private Session getTestSession() {
		return new Session() {

			//-------------------------------------------------------------------------------------------------
			public void close() throws JMSException {}
			public Queue createQueue(final String queueName) throws JMSException { return null;	}
			public Topic createTopic(final String topicName) throws JMSException { return null;	}
			public MessageConsumer createConsumer(final Destination destination) throws JMSException { return null; }
			public MessageProducer createProducer(final Destination destination) throws JMSException { return null;	}
			public TextMessage createTextMessage(final String text) throws JMSException { return null; }
			public BytesMessage createBytesMessage() throws JMSException { return null; }
			public MapMessage createMapMessage() throws JMSException { return null; }
			public Message createMessage() throws JMSException { return null; }
			public ObjectMessage createObjectMessage() throws JMSException { return null; }
			public ObjectMessage createObjectMessage(final Serializable object) throws JMSException { return null; }
			public StreamMessage createStreamMessage() throws JMSException { return null; }
			public TextMessage createTextMessage() throws JMSException { return null; }
			public boolean getTransacted() throws JMSException { return false; 	}
			public int getAcknowledgeMode() throws JMSException { return 0; }
			public void commit() throws JMSException {}
			public void rollback() throws JMSException {}
			public void recover() throws JMSException {}
			public MessageListener getMessageListener() throws JMSException { return null; }
			public void setMessageListener(final MessageListener listener) throws JMSException {}
			public void run() {}
			public MessageConsumer createConsumer(final Destination destination, final String messageSelector) throws JMSException { return null; }
			public MessageConsumer createConsumer(final Destination destination, final String messageSelector, final boolean noLocal) throws JMSException { return null; }
			public MessageConsumer createSharedConsumer(final Topic topic, final String sharedSubscriptionName) throws JMSException { return null; }
			public MessageConsumer createSharedConsumer(final Topic topic, final String sharedSubscriptionName, final String messageSelector) throws JMSException { return null; }
			public TopicSubscriber createDurableSubscriber(final Topic topic, final String name) throws JMSException { return null; }
			public TopicSubscriber createDurableSubscriber(final Topic topic, final String name, final String messageSelector, final boolean noLocal) throws JMSException { return null; }
			public MessageConsumer createDurableConsumer(final Topic topic, final String name) throws JMSException { return null; }
			public MessageConsumer createDurableConsumer(final Topic topic, final String name, final String messageSelector, final boolean noLocal) throws JMSException { return null; }
			public MessageConsumer createSharedDurableConsumer(final Topic topic, final String name) throws JMSException { return null; }
			public MessageConsumer createSharedDurableConsumer(final Topic topic, final String name, final String messageSelector) throws JMSException { return null;	}
			public QueueBrowser createBrowser(final Queue queue) throws JMSException { return null; }
			public QueueBrowser createBrowser(final Queue queue, final String messageSelector) throws JMSException { return null; }
			public TemporaryQueue createTemporaryQueue() throws JMSException { return null; }
			public TemporaryTopic createTemporaryTopic() throws JMSException { return null;	}
			public void unsubscribe(final String name) throws JMSException {}

		};
	}
}