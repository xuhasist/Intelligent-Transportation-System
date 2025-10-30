package com.demo.manager;

import com.demo.message.*;
import com.demo.service.MqttClientService;
import com.demo.service.SocketService;
import nl.altindag.log.LogCaptor;
import org.json.JSONException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.demo.model.its.TcInfo;
import com.demo.service.MessageService;
import com.demo.repository.its.TcInfoRepository;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TcSendMessageManagerTest {
    @InjectMocks
    private TcSendMessageManager manager;

    @Mock
    private TcInfoRepository tcInfoRepository;

    @Mock
    private MessageService messageService;

    @Mock
    private SocketService socketService;

    @Mock
    private MqttClientService mqttClientService;

    @Mock
    private TcReceiveMessageManager tcReceiveMessageManager;

    @Mock
    private Socket socket;

    @Test
    void testRun_all5FxxMessages_success() throws Exception {
        List<String> messageIds = List.of("5F10", "5F15", "5F18", "5F40", "5F45");

        for (String messageId : messageIds) {
            JSONObject msg = new JSONObject();
            msg.put("messageId", messageId);
            JSONObject value = new JSONObject();
            value.put("deviceId", "TC001");
            value.put("planId", 1); // for 5F45
            msg.put("value", value);

            switch (messageId) {
                case "5F10" -> when(messageService.buildMessage(any(JSONObject.class), eq("5f10")))
                        .thenReturn(mock(Message5F10.class));
                case "5F15" -> {
                    when(messageService.buildMessage(any(JSONObject.class), eq("5f15")))
                            .thenReturn(mock(Message5F15.class));
                    when(messageService.buildMessage(any(JSONObject.class), eq("5f14")))
                            .thenReturn(mock(Message5F14.class));
                }
                case "5F18" -> when(messageService.buildMessage(any(JSONObject.class), eq("5f18")))
                        .thenReturn(mock(Message5F18.class));
                case "5F40" -> when(messageService.buildMessage(any(JSONObject.class), eq("5f40")))
                        .thenReturn(mock(Message5F40.class));
                case "5F45" -> {
                    when(messageService.buildMessage(any(JSONObject.class), eq("5f45")))
                            .thenReturn(mock(Message5F45.class));
                    when(messageService.buildMessage(any(JSONObject.class), eq("5f44")))
                            .thenReturn(mock(Message5F44.class));
                }
            }

            TcInfo tcInfo = new TcInfo();
            tcInfo.setIp("127.0.0.1");
            tcInfo.setAddr(65535);
            when(tcInfoRepository.findByTcId("TC001")).thenReturn(tcInfo);
            when(tcInfoRepository.findByIp("127.0.0.1")).thenReturn(tcInfo);

            when(socketService.isHostConnected("127.0.0.1")).thenReturn(true);
            when(socketService.getConnection("127.0.0.1")).thenReturn(socket);

            when(messageService.generateMessage(anyString(), anyString(), any(MessageObject.class)))
                    .thenReturn(List.of(0xAA, 0xBB, 0x12));

            TcSendMessageManager spyManager = spy(manager);
            doReturn(false).when(spyManager)
                    .retrySendWithResponse(any(Socket.class), anyList(), anyString(), anyString(), anyString(), anyString());

            spyManager.run(msg.toString());

            verify(messageService, times(1)).buildMessage(any(JSONObject.class), eq(messageId.toLowerCase()));
            verify(socketService, times(1)).isHostConnected("127.0.0.1");

            clearInvocations(messageService, socketService, spyManager);
        }
    }

    @Test
    void testHandle5F10Message_failure() throws Exception {
        JSONObject msg = new JSONObject();
        msg.put("messageId", "5F10");
        JSONObject value = new JSONObject();
        value.put("deviceId", "TC001");
        msg.put("value", value);

        // buildMessage throw exception
        when(messageService.buildMessage(any(JSONObject.class), eq("5f10"))).thenThrow(new RuntimeException("fail"));

        boolean result = manager.handle5F10Message(msg);

        assertFalse(result);
    }

    @Test
    void testHandle5F15Message_success() throws Exception {
        JSONObject msg = new JSONObject();
        msg.put("messageId", "5F15");
        JSONObject value = new JSONObject();
        value.put("deviceId", "TC001");
        msg.put("value", value);

        Message5F14 msg14 = mock(Message5F14.class);
        Message5F15 msg15 = mock(Message5F15.class);
        when(messageService.buildMessage(any(), eq("5f14"))).thenReturn(msg14);
        when(messageService.buildMessage(any(), eq("5f15"))).thenReturn(msg15);

        TcSendMessageManager spy = spy(manager);
        doReturn(true).when(spy).sendMessage(anyString(), anyString(), any(), anyString(), anyString());

        boolean result = spy.handle5F15Message(msg);
        assertTrue(result);
    }

    @Test
    void testHandle5F15Message_failureOnFirstSend() throws Exception {
        JSONObject msg = new JSONObject();
        msg.put("messageId", "5F15");
        JSONObject value = new JSONObject();
        value.put("deviceId", "TC001");
        msg.put("value", value);

        Message5F14 msg14 = mock(Message5F14.class);
        Message5F15 msg15 = mock(Message5F15.class);
        when(messageService.buildMessage(any(), eq("5f14"))).thenReturn(msg14);
        when(messageService.buildMessage(any(), eq("5f15"))).thenReturn(msg15);

        TcSendMessageManager spy = spy(manager);
        doReturn(false).when(spy).sendMessage(anyString(), eq("5f14"), any(), anyString(), anyString());

        boolean result = spy.handle5F15Message(msg);
        assertFalse(result);
    }

    @Test
    void testHandle5F15Message_exception() throws Exception {
        JSONObject msg = new JSONObject();
        msg.put("messageId", "5F15");
        JSONObject value = new JSONObject();
        value.put("deviceId", "TC003");
        msg.put("value", value);

        when(messageService.buildMessage(any(), eq("5f15"))).thenThrow(new RuntimeException("fail"));

        boolean result = manager.handle5F18Message(msg);
        assertFalse(result);
    }

    @Test
    void testHandle5F18Message_success() throws Exception {
        JSONObject msg = new JSONObject();
        msg.put("messageId", "5F18");
        JSONObject value = new JSONObject();
        value.put("deviceId", "TC002");
        msg.put("value", value);

        Message5F18 msgObj = mock(Message5F18.class);
        when(messageService.buildMessage(any(), eq("5f18"))).thenReturn(msgObj);

        TcSendMessageManager spy = spy(manager);
        doReturn(true).when(spy).sendMessage(anyString(), anyString(), any(), anyString(), anyString());

        boolean result = spy.handle5F18Message(msg);
        assertTrue(result);
    }

    @Test
    void testHandle5F18Message_failureOnFirstSend() throws Exception {
        JSONObject msg = new JSONObject();
        msg.put("messageId", "5F18");
        JSONObject value = new JSONObject();
        value.put("deviceId", "TC002");
        msg.put("value", value);

        Message5F18 msgObj = mock(Message5F18.class);
        when(messageService.buildMessage(any(), eq("5f18"))).thenReturn(msgObj);

        TcSendMessageManager spy = spy(manager);
        doReturn(false).when(spy).sendMessage(anyString(), anyString(), any(), anyString(), anyString());

        boolean result = spy.handle5F18Message(msg);
        assertFalse(result);
    }

    @Test
    void testHandle5F18Message_exception() throws Exception {
        JSONObject msg = new JSONObject();
        msg.put("messageId", "5F18");
        JSONObject value = new JSONObject();
        value.put("deviceId", "TC003");
        msg.put("value", value);

        when(messageService.buildMessage(any(), eq("5f18"))).thenThrow(new RuntimeException("fail"));

        boolean result = manager.handle5F18Message(msg);
        assertFalse(result);
    }

    @Test
    void testHandle5F40Message_exception() throws Exception {
        JSONObject msg = new JSONObject();
        msg.put("messageId", "5F40");
        JSONObject value = new JSONObject();
        value.put("deviceId", "TC003");
        msg.put("value", value);

        when(messageService.buildMessage(any(), eq("5f40"))).thenThrow(new RuntimeException("fail"));

        boolean result = manager.handle5F40Message(msg);
        assertFalse(result);
    }

    @Test
    void testHandle5F45Message_success() throws Exception {
        JSONObject msg = new JSONObject();
        msg.put("messageId", "5F45");
        JSONObject value = new JSONObject();
        value.put("deviceId", "TC004");
        value.put("planId", 2);
        msg.put("value", value);

        Message5F44 msg44 = mock(Message5F44.class);
        Message5F45 msg45 = mock(Message5F45.class);
        when(messageService.buildMessage(any(), eq("5f44"))).thenReturn(msg44);
        when(messageService.buildMessage(any(), eq("5f45"))).thenReturn(msg45);

        TcSendMessageManager spy = spy(manager);
        doReturn(true).when(spy).sendMessage(anyString(), eq("5f44"), any(), anyString(), anyString());
        doReturn(true).when(spy).sendMessage(anyString(), eq("5f45"), any(), anyString(), anyString());

        boolean result = spy.handle5F45Message(msg);
        assertTrue(result);
    }

    @Test
    void testHandle5F45Message_buildMessageThrows() throws JSONException {
        JSONObject msg = new JSONObject();
        msg.put("messageId", "5F45");
        JSONObject value = new JSONObject();
        value.put("deviceId", "TC004");
        value.put("planId", 3);
        msg.put("value", value);

        when(messageService.buildMessage(any(), anyString())).thenThrow(new RuntimeException("error"));
        boolean result = manager.handle5F45Message(msg);
        assertFalse(result);
    }

    @Test
    void testSendMessage_whenSocketNotConnected() {
        TcInfo info = new TcInfo();
        info.setIp("1.1.1.1");
        when(tcInfoRepository.findByTcId(anyString())).thenReturn(info);
        when(socketService.isHostConnected(anyString())).thenReturn(false);

        boolean result = manager.sendMessage("TC001", "5f10", mock(MessageObject.class), "ok", "fail");
        assertFalse(result);
    }

    @Test
    void testSendMessageGenMsgReturnsNull() {
        TcInfo info = new TcInfo();
        info.setIp("1.1.1.1");
        when(tcInfoRepository.findByTcId(anyString())).thenReturn(info);
        when(socketService.isHostConnected(anyString())).thenReturn(true);

        // simulate genMsg return null
        TcSendMessageManager spyManager = Mockito.spy(manager);
        Mockito.doReturn(null).when(spyManager).genMsg(Mockito.anyString(), Mockito.anyString(), Mockito.any());

        boolean result = spyManager.sendMessage("TC001", "5f10", mock(MessageObject.class), "ok", "fail");
        assertFalse(result);
    }

    @Test
    void testSendMessageThrowsIOException() throws IOException, InterruptedException {
        Socket mockSocket = mock(Socket.class);
        when(socketService.getConnection(anyString())).thenReturn(mockSocket);

        TcInfo info = new TcInfo();
        info.setIp("1.1.1.1");
        when(tcInfoRepository.findByTcId(anyString())).thenReturn(info);
        when(socketService.isHostConnected(anyString())).thenReturn(true);

        TcSendMessageManager spyManager = Mockito.spy(manager);
        Mockito.doReturn(List.of()).when(spyManager).genMsg(Mockito.anyString(), Mockito.anyString(), Mockito.any());

        doThrow(new IOException("Socket error"))
                .when(spyManager)
                .retrySendWithResponse(any(Socket.class), anyList(), anyString(), anyString(), anyString(), anyString());

        boolean result = spyManager.sendMessage("TC001", "5f10", mock(MessageObject.class), "ok", "fail");
        assertFalse(result);
    }

    @Test
    void testWaitForSpecificResponse_timeout() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            Socket socket = mock(Socket.class);
            when(tcReceiveMessageManager.getResponseQueues()).thenReturn(new ConcurrentHashMap<>());

            CompletableFuture<List<Integer>> future = manager.waitForSpecificResponse(socket, 100, "A", "B", "C");
            List<Integer> result = future.get(200, TimeUnit.MILLISECONDS);

            assertNull(result);
        });
    }

    @Test
    void testWaitForSpecificResponse_success() throws Exception {
        Socket socket = mock(Socket.class);
        Map<Socket, Map<String, List<Integer>>> queues = new ConcurrentHashMap<>();
        Map<String, List<Integer>> innerMap = new ConcurrentHashMap<>();
        innerMap.put("A", List.of(1, 2, 3));
        queues.put(socket, innerMap);
        when(tcReceiveMessageManager.getResponseQueues()).thenReturn(queues);

        CompletableFuture<List<Integer>> future = manager.waitForSpecificResponse(socket, 1000, "A", "B", "C");
        List<Integer> result = future.get(500, TimeUnit.MILLISECONDS);

        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    void testPublish5FC0Message_success() {
        ReflectionTestUtils.setField(manager, "topic_tc_publish_prefix", "topic/");
        when(mqttClientService.publish(anyInt(), anyBoolean(), anyString(), anyString())).thenReturn(true);

        manager.publish5FC0Message("TC001", List.of(0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4));
        verify(mqttClientService).publish(anyInt(), anyBoolean(), contains("topic/TC001"), anyString());
    }

    @Test
    void testPublish0F80or0F81Message_with0F80() {
        ReflectionTestUtils.setField(manager, "topic_tc_publish_prefix", "prefix/");
        when(mqttClientService.publish(anyInt(), anyBoolean(), anyString(), anyString())).thenReturn(true);

        List<Integer> message = List.of(0, 0, 0, 0, 0, 0, 0, 0x0F, 0x80, 0x5F, 0x10);
        manager.publish0F80or0F81Message("TC001", message);
        verify(mqttClientService).publish(anyInt(), anyBoolean(), contains("prefix/TC001"), anyString());
    }

    @Test
    void testPublish0F80or0F81Message_with0F81() {
        ReflectionTestUtils.setField(manager, "topic_tc_publish_prefix", "prefix/");
        when(mqttClientService.publish(anyInt(), anyBoolean(), anyString(), anyString())).thenReturn(true);

        List<Integer> message = List.of(0, 0, 0, 0, 0, 0, 0, 0x0F, 0x81, 0x5F, 0x14, 0xCC, 0xDD);
        manager.publish0F80or0F81Message("TC001", message);
        verify(mqttClientService).publish(anyInt(), anyBoolean(), contains("prefix/TC001"), anyString());

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(mqttClientService).publish(Mockito.anyInt(), Mockito.anyBoolean(), Mockito.anyString(), jsonCaptor.capture());

        String jsonStr = jsonCaptor.getValue();
        assertTrue(jsonStr.contains("\"errorCode\":\"cc\""));          // resData.substring(8,10)
        assertTrue(jsonStr.contains("\"parameterNumber\":\"dd\""));    // resData.substring(10,12)
        assertTrue(jsonStr.contains("\"messageId\":\"0F81\""));
    }

    @Test
    void testPublish0F80or0F81Message_exception() {
        ReflectionTestUtils.setField(manager, "topic_tc_publish_prefix", "prefix/");
        when(mqttClientService.publish(anyInt(), anyBoolean(), anyString(), anyString())).thenThrow(new RuntimeException("fail"));

        List<Integer> message = List.of(0, 0, 0, 0, 0, 0, 0, 0x0F, 0x81, 0x5F, 0x14, 0xCC, 0xDD);

        LogCaptor logCaptor = LogCaptor.forClass(TcSendMessageManager.class);

        manager.publish0F80or0F81Message("TC001", message);
        List<String> errorLogs = logCaptor.getErrorLogs();
        assertTrue(errorLogs.stream().anyMatch(msg -> msg.contains("Failed to publish 0F80 or 0F81 message")));
    }

    @Test
    void testRetrySendWithResponse_NullResponse_ReturnsFalseAfterRetries() throws Exception {
        List<Integer> msg = List.of(0xAA, 0xBB, 0x01);
        String deviceId = "TC001";
        String command = "5f10";
        String successKey = "success";
        String failKey = "fail";

        BufferedOutputStream out = mock(BufferedOutputStream.class);
        when(socket.getOutputStream()).thenReturn(out);

        // 模擬 responseQueues 永遠沒有 response -> return false
        when(tcReceiveMessageManager.getResponseQueues()).thenReturn(new ConcurrentHashMap<>());

        boolean result = manager.retrySendWithResponse(socket, msg, deviceId, command, successKey, failKey);
        assertFalse(result);

        // verify sendMessageToSocket retry 3 times
        verify(out, times(3)).flush();
    }

    @Test
    void testRetrySendWithResponse_NAKResponse_ReturnsFalse() throws Exception {
        List<Integer> msg = List.of(0xAA, 0xBB, 0x01);
        String deviceId = "TC001";
        String command = "5f10";
        String successKey = "success";
        String failKey = "fail";

        BufferedOutputStream out = mock(BufferedOutputStream.class);
        when(socket.getOutputStream()).thenReturn(out);

        // 模擬 responseQueues 回傳 NAK
        Map<Socket, Map<String, List<Integer>>> responseQueues = new ConcurrentHashMap<>();
        Map<String, List<Integer>> map = new HashMap<>();
        map.put(successKey, List.of(0xAA, 0xEE, 0, 0, 0, 0, 0, 0)); // NAK
        responseQueues.put(socket, map);
        when(tcReceiveMessageManager.getResponseQueues()).thenReturn(responseQueues);

        boolean result = manager.retrySendWithResponse(socket, msg, deviceId, command, successKey, failKey);
        assertFalse(result);
    }

    @Test
    void testRetrySendWithResponse_SuccessResponse_ReturnsTrue() throws Exception {
        List<Integer> msg = List.of(0xAA, 0xBB, 0x01);
        String deviceId = "TC001";
        String command = "5f10";
        String successKey = "success";
        String failKey = "fail";

        BufferedOutputStream out = mock(BufferedOutputStream.class);
        when(socket.getOutputStream()).thenReturn(out);

        // 模擬 responseQueues 回傳正常 response
        Map<Socket, Map<String, List<Integer>>> responseQueues = new ConcurrentHashMap<>();
        Map<String, List<Integer>> map = new HashMap<>();
        map.put(successKey, List.of(0x00, 0x11)); // 任意非 NAK response
        responseQueues.put(socket, map);
        when(tcReceiveMessageManager.getResponseQueues()).thenReturn(responseQueues);

        TcSendMessageManager spyManager = spy(manager);
        doNothing().when(spyManager).publish0F80or0F81Message(anyString(), anyList());

        boolean result = spyManager.retrySendWithResponse(socket, msg, deviceId, command, successKey, failKey);
        assertTrue(result);
    }

    @Test
    void testHandleResponse_NAK_ReturnsFalse() {
        List<Integer> response = List.of(0xAA, 0xEE, 0, 0, 0, 0, 0, 0); // NAK
        boolean result = manager.handleResponse(response, "5f10", "TC001", socket, "successKey");
        assertFalse(result);
    }

    @Test
    void testHandleResponse_Success_ReturnsTrue() {
        List<Integer> response = List.of(0x00, 0x11, 0, 0, 0, 0, 0, 0); // normal response

        TcSendMessageManager spyManager = spy(manager);
        doNothing().when(spyManager).publish0F80or0F81Message(anyString(), anyList());

        Map<Socket, Map<String, List<Integer>>> responseQueues = new ConcurrentHashMap<>();
        Map<String, List<Integer>> map = new HashMap<>();
        responseQueues.put(socket, map);
        when(tcReceiveMessageManager.getResponseQueues()).thenReturn(responseQueues);

        boolean result = spyManager.handleResponse(response, "5f10", "TC001", socket, "successKey");
        assertTrue(result);

        result = spyManager.handleResponse(response, "5f40", "TC001", socket, "successKey");
        assertTrue(result);

        response = List.of(0x00, 0x11, 0, 0, 0, 0, 0, 0x0f, 0x81); // normal response
        result = spyManager.handleResponse(response, "5f14", "TC001", socket, "successKey");
        assertFalse(result);
    }
}
