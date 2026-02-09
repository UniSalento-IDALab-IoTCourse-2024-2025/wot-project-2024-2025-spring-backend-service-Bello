package bello.antonio.carrier_management_service.configuration;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;

@Configuration
public class MqttConfig {

    private static final String BROKER_URL = "tcp://mosquitto:1883";
    private static final String CLIENT_ID = "spring-backend";

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{BROKER_URL});
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        factory.setConnectionOptions(options);
        return factory;
    }

    // ✅ Channel per messaggi di telemetria
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    // ✅ NUOVO: Channel per messaggi di anomalie
    @Bean
    public MessageChannel mqttAnomalyChannel() {
        return new DirectChannel();
    }

    // ✅ Adapter per telemetria (topic: fridge/+/telemetry)
    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        CLIENT_ID + "-inbound-telemetry",
                        mqttClientFactory(),
                        "fridge/+/telemetry"  // + = qualsiasi vehicleName
                );
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    // ✅ NUOVO: Adapter per anomalie (topic: fridge/+/anomalies)
    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttAnomalyInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        CLIENT_ID + "-inbound-anomalies",
                        mqttClientFactory(),
                        "fridge/+/anomalies"  // + = qualsiasi vehicleName
                );
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttAnomalyChannel());
        return adapter;
    }
}
