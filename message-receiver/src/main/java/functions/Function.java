package functions;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;

public class Function {

    @ConfigProperty(name = "telegram.token")
    String telegramToken;

    @ConfigProperty(name = "elasticsearch.url")
    String elasticsearchURL;

    @ConfigProperty(name = "elasticsearch.api-key")
    String elasticsearchAPIKey;

    @Funq
    public CloudEvent<Output> function(CloudEvent<Input> cloudEvent) {

        // print cloudEvent
        System.out.println(cloudEvent);

        // input
        var input = cloudEvent.data();
        var from = input.getFrom();
        var firstName = from.getFirstName();
        var text = input.getText();

        // create message
        var message = "";

        if (text == null || text == "" || text.startsWith("/start")) {
            message = "Olá " + firstName + ", eu sou o assistente 3s2! Como posso te ajudar?";
        } else {
            message = "Vou verificar...";
        }

        // send telegram response message
        new TelegramBot(telegramToken).execute(new SendMessage(input.getChat().getId(), message));

        // create output
        var output = new Output(message);

        // return
        return CloudEventBuilder.create().build(output);
    }

    private void saveOutput(Output output) {

        var restClient = RestClient
        .builder(HttpHost.create(elasticsearchURL))
        .setDefaultHeaders(new Header[]{ new BasicHeader("Authorization", "ApiKey " + elasticsearchAPIKey) })
        .build();

        var transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        var elasticsearchClient = new ElasticsearchClient(transport);
    }
}
