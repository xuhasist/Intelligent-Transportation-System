package com.demo.service;

import com.demo.model.its.TCInfo;
import com.demo.repository.its.TCInfoRepository;
import com.demo.manager.TCSendMessageManager;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Service
public class MqttClientService implements MqttCallback {
    private static final Logger log = LoggerFactory.getLogger(MqttClientService.class);

    @Autowired
    private TCInfoRepository tcInfoRepository;

    @Autowired
    private TCSendMessageManager tcSendMessageManager;

    private MqttClient mqttClient;

    @Value("${mqtt.host}")
    private String mqtt_host;

    @Value("${mqtt.port}")
    private String mqtt_port;

    @Value("${mqtt.username}")
    private String mqtt_username;

    @Value("${mqtt.password}")
    private String mqtt_password;

    @Value("${mqtt.clientId}")
    private String mqtt_clientId;

    // for connection lost retry
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private int retryDelay = 0;

    private final String topic_tc_subscribe_prefix = "/v3/demo/center/TC/";

    public void connect() {
        try {
            if (mqttClient == null) {
                String url = "tcp://%s:%s".formatted(mqtt_host, mqtt_port);
                mqttClient = new MqttClient(url, mqtt_clientId, new MemoryPersistence());
                mqttClient.setCallback(this);
            }

            if (!mqttClient.isConnected()) {
                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);

                options.setUserName(mqtt_username);
                options.setPassword(mqtt_password.toCharArray());
                options.setConnectionTimeout(10);   // 10 secs
                options.setKeepAliveInterval(60);   // 60 secs
                options.setWill("mqtt/disconnect", (mqttClient + " lost connection").getBytes(), 1, false);

                mqttClient.connect(options);

                log.info("MQTT connection success !");

                this.subscribeTCDevice();
            }

        } catch (MqttException e) {
            log.error("MQTT connection failed", e);
        }
    }

    public boolean isMqttConnected() {
        if (mqttClient != null) {
            if (mqttClient.isConnected()) {
                return true;
            } else {
                try {
                    mqttClient.disconnect();
                    mqttClient.close();     // release resource
                    log.info("MQTT client has been completely cleared.");
                } catch (MqttException e) {
                    log.error("Failed to disconnect and close MQTT client", e);
                }
            }
        }

        return false;
    }

    public void subscribeTCDevice() {
        try {
            List<TCInfo> tcls = tcInfoRepository.findByEnable((byte) 1);
            if (tcls == null || tcls.isEmpty()) {
                log.warn("No TCInfo found");
                return;
            }

            List<String> topics = tcls.stream()
                    .map(tc -> topic_tc_subscribe_prefix + tc.getTcId())
                    .toList();

            int[] qos = IntStream.generate(() -> 1).limit(tcls.size()).toArray();

            this.subscribe(topics.toArray(String[]::new), qos);
        } catch (Exception e) {
            log.error("Failed to subscribe to TC devices", e);
        }
    }

    public void subscribe(String[] topic, int[] qos) {
        if (topic == null || topic.length == 0) {
            return;
        }

        try {
            mqttClient.subscribe(topic, qos);
            log.info("Subscribed to topics: {}", String.join(", ", topic));
        } catch (Exception e) {
            log.error("Mqtt subscribe Exception ", e);
        }
    }

    public boolean publish(int qos, boolean retained, String topic, String message) {

        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(qos);
        mqttMessage.setRetained(retained);
        mqttMessage.setPayload(message.getBytes());

        MqttTopic mqttTopic = mqttClient.getTopic(topic);

        try {
            MqttDeliveryToken token = mqttTopic.publish(mqttMessage);
            token.waitForCompletion();
            return token.isComplete();
        } catch (Exception e) {
            log.error("Mqtt publish Exception ", e);
        }
        return false;
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.error("MQTT client connection lost {}", cause.getMessage());

        int delay = (int) Math.pow(2, retryDelay);  // 2^retryDelay, exponential backoff, avoid congestion
        log.info("Retrying connection in {} seconds...", delay);

        scheduler.schedule(() -> {
            try {
                this.connect();
                if (this.isMqttConnected()) {
                    retryDelay = 0;
                    log.info("Reconnected successfully.");
                    this.subscribeTCDevice();
                } else {
                    retryDelay++;
                    // for next time mqtt connectionLost is triggered
                }
            } catch (Exception e) {
                log.error("Reconnect attempt failed", e);
                retryDelay++;
            }
        }, delay, TimeUnit.SECONDS);

        int maxRetries = 3;
        if (retryDelay >= maxRetries) {
            retryDelay = 0;
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        log.info("Topic : {}", topic);
        log.info("Message : {}", new String(message.getPayload()));
        log.info("Qos : {}", message.getQos());
        log.info("isRetained : {}", message.isRetained());

        try {
            if (topic.startsWith(topic_tc_subscribe_prefix)) {
                tcSendMessageManager.run(new String(message.getPayload()));
            }
        } catch (Exception e) {
            log.error("messageArrived processing failed. ", e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        log.info("Delivery complete for token: {}", token.getResponse());
    }
}
