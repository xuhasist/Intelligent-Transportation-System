package com.demo.service;

import com.demo.manager.TCReceiveMessageManager;
import com.demo.model.its.TCInfo;
import com.demo.repository.its.TCInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SocketService {
    private static final Logger log = LoggerFactory.getLogger(SocketService.class);
    private final Map<String, Socket> socketMap = new ConcurrentHashMap<>();

    @Autowired
    private TCInfoRepository tcInfoRepository;

    @Autowired
    private TCReceiveMessageManager tcReceiveMessageManager;

    public void socketConnect() {
        List<TCInfo> tcDevices = tcInfoRepository.findByEnable((byte) 1);
        for (TCInfo tc : tcDevices) {
            singleSocketConnect(tc.getIp(), tc.getPort());
        }
    }

    private void singleSocketConnect(String ip, int port) {
        int connectionTimeout = 3000;   // 3 seconds

        try {
            InetSocketAddress socketAddress = new InetSocketAddress(ip, port);
            Socket socket = new Socket();
            socket.setReuseAddress(true);
            socket.connect(socketAddress, connectionTimeout);

            socketMap.put(ip, socket);

            log.info("Connected to TC: {}", ip);

            tcReceiveMessageManager.run(socket);

            Thread.sleep(100);  // wait a bit for connecting next tc
        } catch (UnknownHostException e) {
            log.error("Unknown host: {}", ip, e);
        } catch (SocketTimeoutException e) {
            log.error("Connection to {}:{} timed out", ip, port, e);
        } catch (ConnectException e) {
            log.error("Connection refused to {}:{}", ip, port, e);
        } catch (IOException e) {
            log.error("I/O error occurred when connecting to {}:{}", ip, port, e);
        } catch (Exception e) {
            log.error("Unexpected exception while connecting to {}:{}", ip, port, e);
        }
    }

    public Socket getConnection(String ip) {
        return socketMap.get(ip);
    }

    public void removeConnection(String ip) {
        socketMap.remove(ip);
    }

    public boolean isHostConnected(String ip) {
        Socket socket = getConnection(ip);
        if (socket != null && socket.isConnected() && !socket.isClosed()) {
            return true;
        } else {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.warn("Error closing socket {}", ip, e);
                }
            }
            removeConnection(ip);
        }

        return false;
    }

    public void closeConnection(String ip) {
        Socket socket = getConnection(ip);
        if (socket != null) {
            try {
                socket.close();     // only here throws exception, IOException
                removeConnection(ip);
                log.info("Closed connection to ip: {}", ip);
            } catch (IOException e) {
                log.error("Error closing connection to ip: {}", ip, e);
            }
        } else {
            log.warn("No active connection found for ip: {}", ip);
        }
    }

    public void checkAllConnections() {
        List<TCInfo> allTC = tcInfoRepository.findAll();
        for (TCInfo tc : allTC) {
            String ip = tc.getIp();

            if (!isHostConnected(ip)) {     // disconnected
                if (tc.getEnable() == 1) {   // Reconnect
                    singleSocketConnect(tc.getIp(), tc.getPort());
                    // resubscribe mqtt topic
                }
            } else {                          // connected
                if (tc.getEnable() == 0) {   // close connection
                    closeConnection(ip);
                    // unsubscribe mqtt topic
                }
            }
        }
    }
}
