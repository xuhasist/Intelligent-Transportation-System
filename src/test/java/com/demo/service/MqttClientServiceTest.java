package com.demo.service;

import com.demo.manager.TcSendMessageManager;
import com.demo.model.its.TcInfo;
import com.demo.repository.its.TcInfoRepository;
import nl.altindag.log.LogCaptor;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MqttClientServiceTest {
    @InjectMocks
    private MqttClientService mqttClientService;

    @Mock
    private TcInfoRepository tcInfoRepository;

    @Mock
    private TcSendMessageManager tcSendMessageManager;

    @Mock
    private MqttClient mqttClient;

    @Mock
    private MqttTopic mqttTopic;

    @Mock
    private IMqttDeliveryToken token;

    @Mock
    private MqttWireMessage wireMessage;

    @BeforeEach
    void setUp() throws Exception {
        Field field = MqttClientService.class.getDeclaredField("topic_tc_subscribe_prefix");
        field.setAccessible(true);
        field.set(mqttClientService, "tc/");

        Field passwordfield = MqttClientService.class.getDeclaredField("mqtt_password");
        passwordfield.setAccessible(true);
        passwordfield.set(mqttClientService, "testPassword");
    }

    @Test
    void testIsMqttConnected_WhenConnected_ReturnsTrue() throws MqttException {
        when(mqttClient.isConnected()).thenReturn(true);

        boolean result = mqttClientService.isMqttConnected();

        assertTrue(result);
        verify(mqttClient, never()).disconnect();
    }

    @Test
    void testIsMqttConnected_WhenDisconnected_CleansUp() throws MqttException {
        when(mqttClient.isConnected()).thenReturn(false);

        boolean result = mqttClientService.isMqttConnected();

        assertFalse(result);
        verify(mqttClient).disconnect();
        verify(mqttClient).close();
    }

    @Test
    void testSubscribeAllTc_Success() throws Exception {
        TcInfo tc1 = new TcInfo();
        tc1.setTcId("123");
        when(tcInfoRepository.findByEnable((byte) 1)).thenReturn(List.of(tc1));

        mqttClientService.subscribeAllTc();

        verify(mqttClient).subscribe(eq(new String[]{"tc/123"}), ArgumentMatchers.<int[]>any());
    }

    @Test
    void testSubscribeAllTc_NoTcFound() throws MqttException {
        when(tcInfoRepository.findByEnable((byte) 1)).thenReturn(List.of());

        mqttClientService.subscribeAllTc();

        // shouldn't call subscribe
        verify(mqttClient, never()).subscribe(any(String[].class), ArgumentMatchers.<int[]>any());
    }

    @Test
    void testPublish_Success() throws Exception {
        String topic = "tc/123";
        String message = "hello";

        when(mqttClient.getTopic(topic)).thenReturn(mqttTopic);
        MqttDeliveryToken token = mock(MqttDeliveryToken.class);
        when(mqttTopic.publish(any())).thenReturn(token);
        when(token.isComplete()).thenReturn(true);

        boolean result = mqttClientService.publish(1, false, topic, message);

        assertTrue(result);
        verify(mqttTopic).publish(any());
    }

    @Test
    void testMessageArrived_ValidTopic_CallsManager() throws Exception {
        String topic = "tc/100";
        MqttMessage msg = new MqttMessage("payload".getBytes());

        mqttClientService.messageArrived(topic, msg);

        verify(tcSendMessageManager).run("payload");
    }

    @Test
    void testMessageArrived_IrrelevantTopic_NoAction() throws Exception {
        String topic = "other/100";
        MqttMessage msg = new MqttMessage("payload".getBytes());

        mqttClientService.messageArrived(topic, msg);

        verify(tcSendMessageManager, never()).run(anyString());
    }

    @Test
    void connect_mqttException_shouldLogError() throws Exception {
        // mock connect() throw exception
        doThrow(new MqttException(0)).when(mqttClient).connect(any(MqttConnectOptions.class));

        Field clientField = MqttClientService.class.getDeclaredField("mqttClient");
        clientField.setAccessible(true);
        clientField.set(mqttClientService, mqttClient);

        mqttClientService.connect();

        verify(mqttClient, times(1)).connect(any(MqttConnectOptions.class));

        verify(mqttClient, never()).disconnect();
        verify(mqttClient, never()).close();
    }

    @Test
    void subscribe_nullOrEmpty_shouldDoNothing() {
        mqttClientService.subscribe(null, new int[]{1});
        mqttClientService.subscribe(new String[]{}, new int[]{1});

        verifyNoInteractions(mqttClient);
    }

    @Test
    void unsubscribe_nullOrEmpty_shouldDoNothing() {
        mqttClientService.unsubscribe(null);
        mqttClientService.unsubscribe(new String[]{});

        verifyNoInteractions(mqttClient);
    }

    @Test
    void publish_exception_shouldReturnFalse() throws Exception {
        String topic = "tc/123";
        String message = "hello";

        when(mqttClient.getTopic(topic)).thenReturn(mqttTopic);
        when(mqttTopic.publish(any())).thenThrow(new MqttException(0));

        boolean result = mqttClientService.publish(1, false, topic, message);

        assertFalse(result);
    }

    @Test
    void subscribeAllTc_subscribeThrows_shouldCatch() throws Exception {
        TcInfo tc1 = new TcInfo();
        tc1.setTcId("123");
        when(tcInfoRepository.findByEnable((byte) 1)).thenReturn(List.of(tc1));

        doThrow(new MqttException(0)).when(mqttClient).subscribe(any(String[].class), any(int[].class));

        mqttClientService.subscribeAllTc(); // 不會 throw, 會被 catch
    }

    @Test
    void testConnectionLost_retriesAndResets() throws Exception {
        doThrow(new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION))
                .when(mqttClient).connect(any(MqttConnectOptions.class));

        mqttClientService.connectionLost(new RuntimeException("Test disconnect"));

        // 等待 scheduler 執行
        Thread.sleep(1500);

        verify(mqttClient, atLeastOnce()).connect(any(MqttConnectOptions.class));
    }

    @Test
    void testSubscribeTc_callsSubscribeWithCorrectTopics() throws MqttException {
        TcInfo tc1 = new TcInfo();
        tc1.setTcId("TC001");
        TcInfo tc2 = new TcInfo();
        tc2.setTcId("TC002");

        List<TcInfo> tcls = Arrays.asList(tc1, tc2);

        mqttClientService.subscribeTc(tcls);

        verify(mqttClient, times(1))
                .subscribe(eq(new String[]{"tc/TC001", "tc/TC002"}), any(int[].class));
    }

    @Test
    void testUnsubscribeTc_callsUnsubscribeWithCorrectTopics() throws MqttException {
        TcInfo tc1 = new TcInfo();
        tc1.setTcId("TC001");
        TcInfo tc2 = new TcInfo();
        tc2.setTcId("TC002");

        List<TcInfo> tcls = Arrays.asList(tc1, tc2);

        mqttClientService.unsubscribeTc(tcls);

        verify(mqttClient, times(1))
                .unsubscribe(eq(new String[]{"tc/TC001", "tc/TC002"}));
    }

    @Test
    void testDeliveryComplete_logsResponse() throws MqttException {
        when(token.getResponse()).thenReturn(wireMessage);
        when(wireMessage.toString()).thenReturn("mockResponse");

        LogCaptor logCaptor = LogCaptor.forClass(MqttClientService.class);

        assertDoesNotThrow(() -> mqttClientService.deliveryComplete(token));
        assertTrue(logCaptor.getInfoLogs().stream()
                .anyMatch(msg -> msg.contains("Delivery complete for token: mockResponse")));
        verify(token, times(1)).getResponse();
    }
}
