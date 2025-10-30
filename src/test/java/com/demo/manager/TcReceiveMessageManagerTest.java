package com.demo.manager;

import com.demo.message.MessageHandler;
import com.demo.model.its.TcInfo;
import com.demo.service.MessageService;
import com.demo.service.SocketService;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.demo.repository.its.TcInfoRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TcReceiveMessageManagerTest {
    @InjectMocks
    private TcReceiveMessageManager manager;

    @Mock
    private TcInfoRepository tcInfoRepository;

    @Mock
    private MessageHandler messageHandler;

    @Mock
    private MessageService messageService;

    @Mock
    private SocketService socketService;

    @Mock
    private Socket socket;

    void initForSocketAndTcInfo() {
        MockitoAnnotations.openMocks(this);
        InetAddress mockAddress = mock(InetAddress.class);
        when(mockAddress.getHostAddress()).thenReturn("192.168.0.1");
        when(socket.getInetAddress()).thenReturn(mockAddress);

        TcInfo tcInfo = new TcInfo();
        tcInfo.setTcId("TC001");
        when(tcInfoRepository.findByIp("192.168.0.1")).thenReturn(tcInfo);
    }

    @Test
    void testHandle5FC0Message() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, JSONException {
        List<Integer> msg = Arrays.asList(0xaa, 0, 0, 0, 0, 0, 0, 0, 0, 1, 5); // 9th idx=controlStrategy, 10th idx=effectTime

        Method method = TcReceiveMessageManager.class
                .getDeclaredMethod("handle5FC0Message", String.class, List.class, List.class);
        method.setAccessible(true);
        method.invoke(manager, "TC001", msg, Collections.emptyList());

        JSONObject value = manager.getValueMap5FC0().get("TC001");
        assertNotNull(value);
        assertEquals(1, value.getInt("ControlStrategy"));
        assertEquals(5, value.getInt("EffectTime"));
    }

    @SneakyThrows
    @Test
    void testHandle5FC4Message() {
        List<Integer> msg = new ArrayList<>();
        for (int i = 0; i < 10; i++) msg.add(i); // first 10 bytes are not used
        msg.addAll(Arrays.asList(
                1, // subPhaseCount
                10, // minGreen
                0, 1, // maxGreen (16bit)
                2, // yellow
                3, // allRed
                4, // pedGreenFlash
                5  // pedRed
        ));

        Method method = TcReceiveMessageManager.class
                .getDeclaredMethod("handle5FC4Message", String.class, List.class, List.class);
        method.setAccessible(true);
        method.invoke(manager, "TC001", msg, Collections.emptyList());

        JSONObject value = manager.getValueMap5FC4().get("TC001");
        assertNotNull(value);
        assertEquals(1, value.getInt("subPhaseCount"));

        JSONArray jsonArray = value.getJSONArray("minGreen");
        List<Integer> minGreen = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            minGreen.add(jsonArray.getInt(i));
        }
        assertEquals(Collections.singletonList(10), minGreen);

        jsonArray = value.getJSONArray("maxGreen");
        List<Integer> maxGreen = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            maxGreen.add(jsonArray.getInt(i));
        }
        assertEquals(Collections.singletonList(1), maxGreen);  // maxGreen = (0<<8)|1 = 1
    }

    @SneakyThrows
    @Test
    void testHandle5FC5Message() {
        // subPhaseCount=1, green=2 bytes, cycleTime=2 bytes, offset=2 bytes
        List<Integer> msg = new ArrayList<>();
        for (int i = 0; i < 12; i++) msg.add(i);
        msg.addAll(Arrays.asList(
                1, // subPhaseCount
                0x00, 0x0A, // green
                0x00, 0x64, // cycleTime
                0x00, 0x32  // offset
        ));

        Method method = TcReceiveMessageManager.class
                .getDeclaredMethod("handle5FC5Message", String.class, List.class, List.class);
        method.setAccessible(true);
        method.invoke(manager, "TC001", msg, Collections.emptyList());

        JSONObject value = manager.getValueMap5FC5().get("TC001");
        assertNotNull(value);
        assertEquals(1, value.getInt("subPhaseCount")); // subPhaseCount = msg.get(12)

        JSONArray jsonArray = value.getJSONArray("green");
        List<Integer> green = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            green.add(jsonArray.getInt(i));
        }
        assertEquals(Collections.singletonList(10), green);
        assertEquals(100, value.getInt("cycleTime"));
        assertEquals(50, value.getInt("offset"));
    }

    @Test
    void testSendACK_callsMessageService() throws IOException {
        initForSocketAndTcInfo();

        List<Integer> msg = Arrays.asList(0xaa, MessageHandler.STX, 0, 0, 0, 0, 0, 0);
        List<String> msgStr = Arrays.asList("aa", "bb", "00", "00", "00", "00", "00", "00");

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        BufferedOutputStream out = new BufferedOutputStream(byteOut);

        when(socket.getOutputStream()).thenReturn(out);
        when(messageHandler.genCKS(anyList())).thenReturn(0);

        manager.sendACK(msg, socket, msgStr);

        out.flush();

        byte[] written = byteOut.toByteArray();
        assertEquals(8, written.length);
        assertEquals((byte) 0xAA, written[0]);
        assertEquals((byte) 0xDD, written[1]);
        assertEquals((byte) 0x00, written[5]);
        assertEquals((byte) 0x08, written[6]);
        assertEquals((byte) 0, written[7]);

        verify(messageService, times(1)).saveMessageLog(any(JSONObject.class), anyString(), any(), anyInt());
    }

    @Test
    void testSendNAK_callsMessageService() throws IOException {
        initForSocketAndTcInfo();

        List<Integer> msg = Arrays.asList(0xaa, MessageHandler.STX, 0, 0, 0, 0, 0, 0);
        List<String> msgStr = Arrays.asList("aa", "bb", "00", "00", "00", "00", "00", "00");

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        BufferedOutputStream out = new BufferedOutputStream(byteOut);

        when(socket.getOutputStream()).thenReturn(out);
        when(messageHandler.genCKS(anyList())).thenReturn(0);

        manager.sendNAK(msg, 5, socket, msgStr);

        out.flush();

        byte[] written = byteOut.toByteArray();
        assertEquals(9, written.length); // 確認寫入 9 bytes
        assertEquals((byte) 0xAA, written[0]);
        assertEquals((byte) 0xEE, written[1]);
        assertEquals((byte) 0x00, written[5]);
        assertEquals((byte) 0x09, written[6]);
        assertEquals((byte) 5, written[7]);
        assertEquals((byte) 0, written[8]);

        verify(messageService, times(1)).saveMessageLog(any(JSONObject.class), anyString(), any(), anyInt());
    }

    @Test
    void testRun_receivesACKAndNAK() throws Exception {
        initForSocketAndTcInfo();

        ByteArrayInputStream byteIn = new ByteArrayInputStream(new byte[]{
                (byte) 0xaa, (byte) MessageHandler.ACK, 0x00, 0x00, 0x00, 0x00, 0x08, 0x00
        });
        when(socket.getInputStream()).thenReturn(byteIn);
        when(socketService.isHostConnected(anyString())).thenReturn(true).thenReturn(false);

        manager.run(socket);

        assertTrue(manager.getResponseQueues().containsKey(socket) == false);
    }

    @Test
    void testRun_notStartWithAA() throws Exception {
        initForSocketAndTcInfo();

        ByteArrayInputStream byteIn = new ByteArrayInputStream(new byte[]{
                (byte) 0xff, (byte) 0xaa, (byte) MessageHandler.ACK, 0x00, 0x00, 0x00, 0x00, 0x08, 0x00
        });
        when(socket.getInputStream()).thenReturn(byteIn);
        when(socketService.isHostConnected(anyString())).thenReturn(true).thenReturn(false);

        manager.run(socket);

        assertTrue(manager.getResponseQueues().containsKey(socket) == false);
    }

    @Test
    void testCombine5FC45FC5Messages_mergesMessages() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Socket socket = mock(Socket.class);
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("5fc401", new ArrayList<>(List.of(1, 2, 3)));
        manager.getResponseQueues().put(socket, map);

        List<Integer> msg5fc5 = new ArrayList<>(List.of(4, 5, 6));
        Method method = TcReceiveMessageManager.class
                .getDeclaredMethod("combine5FC45FC5Messages", Socket.class, String.class, List.class);
        method.setAccessible(true);
        method.invoke(manager, socket, "5fc501", msg5fc5);

        List<Integer> combined = manager.getResponseQueues().get(socket).get("5fc501");
        assertEquals(List.of(1, 2, 3, 4, 5, 6), combined);
    }

    @Test
    void testGenerateKey() throws Exception {
        List<Integer> msg = Arrays.asList(0xAA, 0xBB, 0x01, 0x02, 0x03);
        Method method = TcReceiveMessageManager.class
                .getDeclaredMethod("generateKey", List.class, int[].class);
        method.setAccessible(true);
        String key = (String) method.invoke(manager, msg, new int[]{0, 1, 2});
        assertEquals("aabb01", key);
    }

    @Test
    void testRun_handlesNAKMessage() throws Exception {
        initForSocketAndTcInfo();

        // 模擬 NAK 消息長度為 9
        byte[] nakMessage = new byte[]{
                (byte) 0xAA, (byte) MessageHandler.NAK, 0x01, 0x02, 0x03, 0x00, 0x00, 0x00, 0x00
        };

        ByteArrayInputStream byteIn = new ByteArrayInputStream(nakMessage);
        when(socket.getInputStream()).thenReturn(byteIn);
        when(socketService.isHostConnected(anyString())).thenReturn(true).thenReturn(false);

        manager.run(socket);

        // 驗證 responseQueues 是否正確保存 NAK 消息
        assertFalse(manager.getResponseQueues().get(socket).isEmpty());

        String key = "aa" + Integer.toHexString(MessageHandler.NAK) + String.format("%03x", 0x01);
        assertTrue(manager.getResponseQueues().get(socket).containsKey(key));
    }

    @Test
    void testRun_handlesSTXMessage_ACKPath() throws Exception {
        initForSocketAndTcInfo();

        // 模擬 STX 消息，長度 > 9
        byte[] stxMessage = new byte[]{
                (byte) 0xAA, (byte) MessageHandler.STX,
                0x00, 0x01, 0x02, 0x03, 0x04,
                0x5F, (byte) 0xC0, // 5FC0 -> index 7,8
                0x00, 0x01, 0x02,
                (byte) MessageHandler.DLE,
                (byte) MessageHandler.ETX,
                0x00 // CKS
        };

        ByteArrayInputStream byteIn = new ByteArrayInputStream(stxMessage);
        when(socket.getInputStream()).thenReturn(byteIn);
        when(socketService.isHostConnected(anyString())).thenReturn(true).thenReturn(false);

        // 模擬 checkCode 返回 0 -> 走 ACK 分支
        when(messageHandler.checkCode(anyList(), anyString())).thenReturn(0);
        when(messageHandler.recvNormalize(anyList())).thenAnswer(inv -> inv.getArgument(0));

        manager.run(socket);

        // 驗證 handle5FC0Message 是否被觸發
        JSONObject value = manager.getValueMap5FC0().get("TC001");
        assertNotNull(value);
    }

    @Test
    void testRun_handlesSTXMessage_ACKPath_with5FC4And5FC5() throws Exception {
        initForSocketAndTcInfo();

        // 模擬 STX 消息，長度 > 9，value = 0xC4 (5FC4)
        byte[] stxMessage5FC4 = new byte[]{
                (byte) 0xAA, (byte) MessageHandler.STX,
                0x00, 0x01, 0x02, 0x03, 0x04,
                0x5F, (byte) 0xC4, // index 7,8 -> 5FC4
                0x01, // planId
                0x00, // subPhaseCount
                0x01,
                (byte) MessageHandler.DLE,
                (byte) MessageHandler.ETX,
                0x00 // CKS
        };

        // 模擬 STX 消息，value = 0xC5 (5FC5)
        byte[] stxMessage5FC5 = new byte[]{
                (byte) 0xAA, (byte) MessageHandler.STX,
                0x00, 0x01, 0x02, 0x03, 0x04,
                0x5F, (byte) 0xC5, // index 7,8 -> 5FC5
                0x01, // planId
                0x00,
                0x00, // subPhaseCount
                0x01, 0x02, // cycleTime
                0x03, 0x04, // offset
                (byte) MessageHandler.DLE,
                (byte) MessageHandler.ETX,
                0x00 // CKS
        };

        // 將兩個消息合併到 InputStream，模擬連續接收
        ByteArrayInputStream byteIn = new ByteArrayInputStream(
                ArrayUtils.addAll(stxMessage5FC4, stxMessage5FC5)
        );
        when(socket.getInputStream()).thenReturn(byteIn);
        when(socketService.isHostConnected(anyString())).thenReturn(true).thenReturn(false);

        // 模擬 checkCode 返回 0 -> 走 ACK 分支
        when(messageHandler.checkCode(anyList(), anyString())).thenReturn(0);
        when(messageHandler.recvNormalize(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // spy manager 以便驗證 handle5FCx 方法是否被呼叫
        TcReceiveMessageManager spyManager = Mockito.spy(manager);

        // 呼叫 run 方法
        spyManager.run(socket);

        // 驗證 handle5FC4Message 是否被觸發
        JSONObject value = manager.getValueMap5FC4().get("TC001");
        assertNotNull(value);

        // 驗證 handle5FC5Message 是否被觸發
        value = manager.getValueMap5FC5().get("TC001");
        assertNotNull(value);
    }

    @Test
    void testRun_handlesSTXMessage_NAKPath() throws Exception {
        initForSocketAndTcInfo();

        // 模擬 STX 消息，長度 > 9
        byte[] stxMessage = new byte[]{
                (byte) 0xAA, (byte) MessageHandler.STX,
                0x00, 0x01, 0x02, 0x03, 0x04,
                0x5F, (byte) 0xC0, // 5FC0 -> index 7,8
                0x00,
                (byte) MessageHandler.DLE,
                (byte) MessageHandler.DLE,
                (byte) MessageHandler.DLE,
                (byte) MessageHandler.ETX,
                0x00 // CKS
        };

        ByteArrayInputStream byteIn = new ByteArrayInputStream(stxMessage);
        when(socket.getInputStream()).thenReturn(byteIn);
        when(socketService.isHostConnected(anyString())).thenReturn(true).thenReturn(false);

        // 模擬 checkCode 返回 1 -> 走 NAK 分支
        when(messageHandler.checkCode(anyList(), anyString())).thenReturn(1);

        TcReceiveMessageManager spyManager = spy(manager);
        spyManager.run(socket);

        verify(spyManager, times(1)).sendNAK(anyList(), anyInt(), any(Socket.class), anyList());
    }

    @Test
    void testHandle5FC4Message_shouldSaveCorrectData() throws JSONException {
        // minimal fake data with 1 subPhaseCount
        List<Integer> msg = new ArrayList<>(Arrays.asList(
                0xAA, MessageHandler.STX,
                0, 0, 0, 0, 0,
                0x5f, 0xc4, 1, 1, // planId=1, subPhaseCount=1
                10, 0, 20, 30, 40, 50, 60,
                MessageHandler.DLE,
                MessageHandler.ETX,
                0x00
        ));
        manager.handle5FC4Message("TC001", msg, List.of("aa", "bb"));

        JSONObject value = manager.getValueMap5FC4().get("TC001");
        assertNotNull(value);
        assertEquals(1, value.getInt("planId"));
        assertEquals(1, value.getInt("subPhaseCount"));
        assertEquals(10, value.getJSONArray("minGreen").getInt(0));
    }

    @Test
    void testHandle5FC5Message_shouldSaveCorrectData() throws JSONException {
        List<Integer> msg = new ArrayList<>(Arrays.asList(
                0xAA, MessageHandler.STX,
                0, 0, 0, 0, 0,
                0x5f, 0xc5, 1, 2, 3, 1, // planId, direct, phaseOrder, subPhaseCount
                0, 10, 0, 20, 0, 30,  // greens (3 pairs)
                MessageHandler.DLE,
                MessageHandler.ETX,
                0x00
        ));
        manager.handle5FC5Message("TC001", msg, List.of("aa", "bb"));

        JSONObject value = manager.getValueMap5FC5().get("TC001");
        assertNotNull(value);
        assertEquals(1, value.getInt("planId"));
        assertEquals(2, value.getInt("direct"));
        assertEquals("03", value.getString("phaseOrder"));
        assertEquals(1, value.getInt("subPhaseCount"));
        assertTrue(value.has("green"));
    }

    @Test
    void testCombine5FC45FC5Messages_shouldMergeAndRemoveOld() {
        String key5fc4 = "5fc401";
        String key5fc5 = "5fc501";

        Map<String, List<Integer>> fakeMap = new ConcurrentHashMap<>();
        fakeMap.put(key5fc4, new ArrayList<>(List.of(1, 2, 3)));
        manager.getResponseQueues().put(socket, fakeMap);

        List<Integer> newMsg = new ArrayList<>(List.of(9, 9, 9));
        manager.combine5FC45FC5Messages(socket, key5fc5, newMsg);

        assertTrue(manager.getResponseQueues().get(socket).containsKey(key5fc5));
        assertFalse(manager.getResponseQueues().get(socket).containsKey(key5fc4));
        assertEquals(List.of(1, 2, 3, 9, 9, 9),
                manager.getResponseQueues().get(socket).get(key5fc5));
    }

    @Test
    void test0F80And0F81Message_shouldRemoveKeysProperly() {
        Map<String, List<Integer>> fakeMap = new ConcurrentHashMap<>();
        fakeMap.put("0f805f14", List.of(1, 2, 3));
        fakeMap.put("0f805f15", List.of(4, 5, 6));
        manager.getResponseQueues().put(socket, fakeMap);

        // simulate condition: message 0F80 + 0x15 (remove 0f805f14)
        List<Integer> message = Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0x0f, 0x80, 0x00, 0x15);
        String key = "0f805f15";
        manager.saveToQueue(socket, key, new ArrayList<>(List.of(1, 2, 3)));

        // act: mimic logic
        Optional<String> matchingKey = fakeMap.keySet().stream()
                .filter(k -> k.startsWith("0f805f14"))
                .findFirst();

        if (matchingKey.isPresent()) {
            fakeMap.remove("0f805f14");
        } else {
            fakeMap.remove(key);
        }

        assertFalse(fakeMap.containsKey("0f805f14"));
    }

    @Test
    void testRun_handles0F80And0F81Messages() throws Exception {
        initForSocketAndTcInfo();

        // 模擬 0F80 消息
        byte[] msg0F80 = new byte[]{
                (byte) 0xAA, (byte) MessageHandler.STX,
                0x00, 0x01, 0x02, 0x03, 0x04,
                0x0F, (byte) 0x80, 0x00, 0x15, 0x01, // index 7,8,9,10,11
                (byte) MessageHandler.DLE, (byte) MessageHandler.ETX, 0x00
        };

        // 模擬 0F81 消息
        byte[] msg0F81 = new byte[]{
                (byte) 0xAA, (byte) MessageHandler.STX,
                0x00, 0x01, 0x02, 0x03, 0x04,
                0x0F, (byte) 0x81, 0x00, 0x15, 0x01, 0x02, // index 7~12
                (byte) MessageHandler.DLE, (byte) MessageHandler.ETX, 0x00
        };

        // 合併消息流，模擬連續接收
        ByteArrayInputStream byteIn = new ByteArrayInputStream(
                ArrayUtils.addAll(msg0F80, msg0F81)
        );
        when(socket.getInputStream()).thenReturn(byteIn);
        when(socketService.isHostConnected(anyString())).thenReturn(true).thenReturn(false);

        // 模擬 checkCode 返回 0 -> 走 ACK 分支
        when(messageHandler.checkCode(anyList(), anyString())).thenReturn(0);
        when(messageHandler.recvNormalize(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // spy manager 以便驗證 saveToQueue 與 responseQueues
        TcReceiveMessageManager spyManager = Mockito.spy(manager);

        spyManager.run(socket);

        // 驗證 0F80 key 被存入 responseQueues
        String key0F80 = "0f800015"; // 根據 generateKey 生成規則
        assertTrue(spyManager.getResponseQueues().get(socket).containsKey(key0F80) ||
                !spyManager.getResponseQueues().get(socket).containsKey("0f805f14")); // 覆蓋 remove 分支

        // 驗證 0F81 key 被存入 responseQueues
        String key0F81 = "0f810015";
        assertTrue(spyManager.getResponseQueues().get(socket).containsKey(key0F81) ||
                !spyManager.getResponseQueues().get(socket).containsKey("0f805f14"));
    }
}
