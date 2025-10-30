package com.demo.service;

import com.demo.message.*;
import com.demo.model.its.TcMessageLog;
import com.demo.repository.its.TcMessageLogRepository;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageServiceTest {
    @InjectMocks
    private MessageService messageService;

    @Mock
    private TcMessageLogRepository tcMessageLogRepository;

    @Mock
    private MessageBuilder messageBuilder;

    @Mock
    private MessageGenerator messageGenerator;

    @Test
    void testBuildMessage() {
        JSONObject obj = new JSONObject();
        Message5F10 mockMsg5F10 = mock(Message5F10.class);
        Message5F14 mockMsg5F14 = mock(Message5F14.class);
        Message5F15 mockMsg5F15 = mock(Message5F15.class);
        Message5F18 mockMsg5F18 = mock(Message5F18.class);
        Message5F40 mockMsg5F40 = mock(Message5F40.class);
        Message5F44 mockMsg5F44 = mock(Message5F44.class);
        Message5F45 mockMsg5F45 = mock(Message5F45.class);

        when(messageBuilder.buildMessage5F10(obj)).thenReturn(mockMsg5F10);
        when(messageBuilder.buildMessage5F14(obj)).thenReturn(mockMsg5F14);
        when(messageBuilder.buildMessage5F15(obj)).thenReturn(mockMsg5F15);
        when(messageBuilder.buildMessage5F18(obj)).thenReturn(mockMsg5F18);
        when(messageBuilder.buildMessage5F40(obj)).thenReturn(mockMsg5F40);
        when(messageBuilder.buildMessage5F44(obj)).thenReturn(mockMsg5F44);
        when(messageBuilder.buildMessage5F45(obj)).thenReturn(mockMsg5F45);

        MessageObject result = messageService.buildMessage(obj, "5f10");
        assertEquals(mockMsg5F10, result);

        result = messageService.buildMessage(obj, "5f14");
        assertEquals(mockMsg5F14, result);

        result = messageService.buildMessage(obj, "5f15");
        assertEquals(mockMsg5F15, result);

        result = messageService.buildMessage(obj, "5f18");
        assertEquals(mockMsg5F18, result);

        result = messageService.buildMessage(obj, "5f40");
        assertEquals(mockMsg5F40, result);

        result = messageService.buildMessage(obj, "5f44");
        assertEquals(mockMsg5F44, result);

        result = messageService.buildMessage(obj, "5f45");
        assertEquals(mockMsg5F45, result);

        assertNull(messageService.buildMessage(obj, "unknown"));
    }

    @Test
    void testGenerateMessage() {
        // 5f10
        Message5F10 msg5F10 = mock(Message5F10.class);
        when(messageGenerator.gen5F10("addr1", msg5F10)).thenReturn(Arrays.asList(1, 2, 3));
        List<Integer> result10 = messageService.generateMessage("addr1", "5f10", msg5F10);
        assertEquals(Arrays.asList(1, 2, 3), result10);

        // 5f14
        Message5F14 msg5F14 = mock(Message5F14.class);
        when(messageGenerator.gen5F14("addr1", msg5F14)).thenReturn(Arrays.asList(4, 5, 6));
        List<Integer> result14 = messageService.generateMessage("addr1", "5f14", msg5F14);
        assertEquals(Arrays.asList(4, 5, 6), result14);

        // 5f15
        Message5F15 msg5F15 = mock(Message5F15.class);
        when(messageGenerator.gen5F15("addr1", msg5F15)).thenReturn(Arrays.asList(7, 8, 9));
        List<Integer> result15 = messageService.generateMessage("addr1", "5f15", msg5F15);
        assertEquals(Arrays.asList(7, 8, 9), result15);

        // 5f18
        Message5F18 msg5F18 = mock(Message5F18.class);
        when(messageGenerator.gen5F18("addr1", msg5F18)).thenReturn(Arrays.asList(10, 11));
        List<Integer> result18 = messageService.generateMessage("addr1", "5f18", msg5F18);
        assertEquals(Arrays.asList(10, 11), result18);

        // 5f40
        Message5F40 msg5F40 = mock(Message5F40.class);
        when(messageGenerator.gen5F40("addr1", msg5F40)).thenReturn(Arrays.asList(12));
        List<Integer> result40 = messageService.generateMessage("addr1", "5f40", msg5F40);
        assertEquals(Arrays.asList(12), result40);

        // 5f44
        Message5F44 msg5F44 = mock(Message5F44.class);
        when(messageGenerator.gen5F44("addr1", msg5F44)).thenReturn(Arrays.asList(13, 14));
        List<Integer> result44 = messageService.generateMessage("addr1", "5f44", msg5F44);
        assertEquals(Arrays.asList(13, 14), result44);

        // 5f45
        Message5F45 msg5F45 = mock(Message5F45.class);
        when(messageGenerator.gen5F45("addr1", msg5F45)).thenReturn(Arrays.asList(15, 16));
        List<Integer> result45 = messageService.generateMessage("addr1", "5f45", msg5F45);
        assertEquals(Arrays.asList(15, 16), result45);

        // default / unknown
        assertNull(messageService.generateMessage("addr1", "unknown", msg5F10));
    }

    @Test
    void testSaveMessageLog() throws JSONException {
        JSONObject valueObj = new JSONObject();
        valueObj.put("deviceId", "dev001");
        JSONObject obj = new JSONObject();
        obj.put("value", valueObj);
        obj.put("messageId", "msg01");

        ArgumentCaptor<TcMessageLog> captor = ArgumentCaptor.forClass(TcMessageLog.class);

        // return the saved entity
        when(tcMessageLogRepository.save(any(TcMessageLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TcMessageLog log = messageService.saveMessageLog(obj, "raw", "return", 100);

        assertNotNull(log);
        assertEquals("MSG01", log.getMessageId().toUpperCase());
        assertEquals("dev001", log.getDeviceId());
        assertEquals(100, log.getNoteCode());
        assertEquals("raw", log.getRawValue());
        assertEquals("return", log.getReturnResult());

        verify(tcMessageLogRepository, times(2)).save(captor.capture());
        TcMessageLog firstSave = captor.getAllValues().get(0);
        TcMessageLog secondSave = captor.getAllValues().get(1);
        assertEquals("dev001", firstSave.getDeviceId());
        assertEquals("raw", secondSave.getRawValue());
        assertEquals("return", secondSave.getReturnResult());
    }

    @Test
    void testSaveMessageLog_whenException_shouldLogError() throws JSONException {
        JSONObject valueObj = new JSONObject();
        valueObj.put("deviceId", "dev001");
        JSONObject obj = new JSONObject();
        obj.put("value", valueObj);
        obj.put("messageId", "msg01");

        when(tcMessageLogRepository.save(any(TcMessageLog.class))).thenThrow(new RuntimeException("DB error"));

        TcMessageLog log = messageService.saveMessageLog(obj, "raw", "return", 100);

        assertNull(log.getId());
    }

    @Test
    void testBuildMessage_withNullCommand_returnsNull() {
        JSONObject obj = new JSONObject();
        assertNull(messageService.buildMessage(obj, "notExistCommand"));
    }

    @Test
    void testGenerateMessage_withNullMessageObject_returnsNull() {
        assertTrue(messageService.generateMessage("addr1", "5f10", null).isEmpty());
    }

    @Test
    void testGenerateMessage_wrongType_shouldThrow() {
        MessageObject wrongObj = mock(Message5F14.class); // 傳 5F14 但 command 5f10
        assertThrows(ClassCastException.class,
                () -> messageService.generateMessage("addr1", "5f10", wrongObj));
    }

}
