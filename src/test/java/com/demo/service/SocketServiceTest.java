package com.demo.service;

import com.demo.manager.TcReceiveMessageManager;
import com.demo.model.its.TcInfo;
import com.demo.notification.DiscordNotifier;
import com.demo.repository.its.TcInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SocketServiceTest {
    @InjectMocks
    private SocketService socketService;

    @Mock
    private TcInfoRepository tcInfoRepository;

    @Mock
    private TcReceiveMessageManager tcReceiveMessageManager;

    @Mock
    private DiscordNotifier discordNotifier;

    @Mock
    private MqttClientService mqttClientService;

    Map<String, Socket> socketMap;

    @BeforeEach
    void setup() throws NoSuchFieldException, IllegalAccessException {
        Field field = SocketService.class.getDeclaredField("socketMap");
        field.setAccessible(true);

        socketMap = (Map<String, Socket>) field.get(socketService);
    }

    @Test
    void testGetConnection() {
        // put mock socket
        Socket mockSocket = mock(Socket.class);
        socketMap.put("1.2.3.4", mockSocket);

        Socket result = socketService.getConnection("1.2.3.4");
        assertEquals(mockSocket, result);
    }

    @Test
    void testRemoveConnection_ShouldCloseSocketAndNotify() throws IOException {
        String ip = "1.1.1.1";
        Socket mockSocket = mock(Socket.class);
        socketMap.put(ip, mockSocket);

        socketService.removeConnection(ip);

        verify(mockSocket).close();
        verify(discordNotifier).sendMessage(contains(ip));
        assertFalse(socketMap.containsKey(ip));
    }

    @Test
    void testIsHostConnected_WhenConnectedTrue() {
        Socket mockSocket = mock(Socket.class);
        when(mockSocket.isConnected()).thenReturn(true);
        when(mockSocket.isClosed()).thenReturn(false);

        socketMap.put("127.0.0.1", mockSocket);

        boolean result = socketService.isHostConnected("127.0.0.1");
        assertTrue(result);
    }

    @Test
    void testIsHostConnected_WhenSocketClosed_ShouldRemove() throws IOException {
        String ip = "10.0.0.1";
        Socket mockSocket = mock(Socket.class);
        when(mockSocket.isConnected()).thenReturn(false);
        //when(mockSocket.isClosed()).thenReturn(true); // redundant

        socketMap.put(ip, mockSocket);
        doNothing().when(mockSocket).close();
        doNothing().when(discordNotifier).sendMessage(anyString());

        boolean result = socketService.isHostConnected(ip);

        assertFalse(result);
        verify(mockSocket, atLeastOnce()).close();
        verify(discordNotifier).sendMessage(contains(ip));
    }

    @Test
    void testCloseConnection_WhenSocketExists() throws IOException {
        String ip = "192.168.0.10";
        Socket mockSocket = mock(Socket.class);
        socketMap.put(ip, mockSocket);

        doNothing().when(mockSocket).close();
        socketService.closeConnection(ip);

        verify(mockSocket, atLeastOnce()).close();
        verify(discordNotifier).sendMessage(contains(ip));
        assertFalse(socketMap.containsKey(ip));
    }

    @Test
    void testCloseConnection_WhenSocketNotFound() {
        socketService.closeConnection("no.ip");
        // only log warn, no exception thrown
    }

    @Test
    void testCheckAllConnections_ReconnectAndSubscribe() throws IOException {
        TcInfo tc1 = new TcInfo();
        tc1.setIp("1.1.1.1");
        tc1.setEnable((byte) 1);

        TcInfo tc2 = new TcInfo();
        tc2.setIp("2.2.2.2");
        tc2.setEnable((byte) 0);

        when(tcInfoRepository.findAll()).thenReturn(List.of(tc1, tc2));

        SocketService socketServiceSpy = spy(socketService);
        doReturn(true).when(socketServiceSpy).isHostConnected(anyString());
        doNothing().when(socketServiceSpy).closeConnection(any());

        socketServiceSpy.checkAllConnections();

        // 驗證 MQTT 行為
        verify(mqttClientService, times(0)).subscribeTc(argThat(list ->
                list.size() == 1 && list.getFirst().getIp().equals("1.1.1.1")
        ));
        verify(mqttClientService).unsubscribeTc(argThat(list ->
                list.size() == 1 && list.getFirst().getIp().equals("2.2.2.2")
        ));
    }

    @Test
    void testSocketConnect_handlesUnknownHostGracefully() {
        // simulate wrong IP
        TcInfo tc = new TcInfo();
        tc.setIp("invalid.host");
        tc.setPort(1234);
        tc.setEnable((byte) 1);
        when(tcInfoRepository.findByEnable((byte) 1)).thenReturn(List.of(tc));

        socketService.socketConnect();

        verify(tcInfoRepository).findByEnable((byte) 1);
    }

    @Test
    void testIsHostConnected_whenCloseThrowsIOException_shouldLogWarn() throws IOException {
        String ip = "10.10.10.10";
        Socket mockSocket = mock(Socket.class);
        when(mockSocket.isConnected()).thenReturn(false);
        doThrow(new IOException("close fail")).when(mockSocket).close();

        socketMap.put(ip, mockSocket);
        doNothing().when(discordNotifier).sendMessage(anyString());

        boolean result = socketService.isHostConnected(ip);

        assertFalse(result);
        verify(discordNotifier).sendMessage(contains(ip));
    }

    @Test
    void testCloseConnection_whenCloseThrowsIOException_shouldLogError() throws IOException {
        String ip = "3.3.3.3";
        Socket mockSocket = mock(Socket.class);
        doThrow(new IOException("close fail")).when(mockSocket).close();
        socketMap.put(ip, mockSocket);

        socketService.closeConnection(ip);

        // log.error 分支覆蓋
        verify(mockSocket).close();
        // 不會拋例外
    }

    @Test
    void testCheckAllConnections_ReconnectScenario() throws Exception {
        TcInfo tc = new TcInfo();
        tc.setIp("4.4.4.4");
        tc.setPort(1883);
        tc.setEnable((byte) 1);

        when(tcInfoRepository.findAll()).thenReturn(List.of(tc));

        SocketService spyService = spy(socketService);
        doReturn(false).when(spyService).isHostConnected(anyString()); // disconnected
        doNothing().when(spyService).singleSocketConnect(anyString(), anyInt());

        spyService.checkAllConnections();

        verify(spyService).singleSocketConnect("4.4.4.4", 1883);
        verify(mqttClientService).subscribeTc(argThat(list ->
                list.size() == 1 && list.getFirst().getIp().equals("4.4.4.4")
        ));
    }

    @Test
    void testSocketTimeoutException() throws Exception {
        Socket spySocket = spy(new Socket());
        doThrow(new SocketTimeoutException("timeout")).when(spySocket).connect(any(SocketAddress.class), anyInt());

        SocketService spyService = spy(socketService);
        doReturn(spySocket).when(spyService).createSocket();

        spyService.singleSocketConnect("127.0.0.1", 8080); // should catch SocketTimeoutException
    }

    @Test
    void testConnectException() throws Exception {
        Socket spySocket = spy(new Socket());
        doThrow(new ConnectException("connection refused")).when(spySocket).connect(any(SocketAddress.class), anyInt());

        SocketService spyService = spy(socketService);
        doReturn(spySocket).when(spyService).createSocket();

        spyService.singleSocketConnect("127.0.0.1", 8080); // should catch ConnectException
    }

    @Test
    void testIOException() throws Exception {
        Socket spySocket = spy(new Socket());
        doThrow(new IOException("io error")).when(spySocket).connect(any(SocketAddress.class), anyInt());

        SocketService spyService = spy(socketService);
        doReturn(spySocket).when(spyService).createSocket();

        spyService.singleSocketConnect("127.0.0.1", 8080); // should catch IOException
    }

    @Test
    void testGenericException() throws Exception {
        Socket spySocket = spy(new Socket());
        doThrow(new RuntimeException("generic error")).when(spySocket).connect(any(SocketAddress.class), anyInt());

        SocketService spyService = spy(socketService);
        doReturn(spySocket).when(spyService).createSocket();

        spyService.singleSocketConnect("127.0.0.1", 8080); // should catch Exception
    }
}
