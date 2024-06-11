package functions;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback;

public class Function {

    @ConfigProperty(name = "telegram.token")
    String telegramToken;

    @ConfigProperty(name = "elasticsearch.host")
    String elasticsearchHost;

    @ConfigProperty(name = "elasticsearch.port")
    Integer elasticsearchPort;

    @ConfigProperty(name = "elasticsearch.username")
    String elasticsearchUsername;

    @ConfigProperty(name = "elasticsearch.password")
    String elasticsearchPassword;

    @Funq
    public CloudEvent<Output> function(CloudEvent<Input> cloudEvent) {

        // print cloudEvent
        System.out.println(cloudEvent);

        // input
        var input = cloudEvent.data();
        var from = input.getFrom();
        var date = input.getDate();
        var firstName = from.getFirstName();
        var lastName = from.getLastName();
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
        var output = new Output(date, firstName, lastName, text, message);
        saveOutput(output);

        // return
        return CloudEventBuilder.create().build(output);
    }

    private void saveOutput(Output output) {
        try {
            var credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(elasticsearchUsername, elasticsearchPassword));

            var httpClientConfigCallback = new HttpClientConfigCallback(){
                @Override
                public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpAsyncClientBuilder) {
                    return httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                }
            };

            var httpHost = new HttpHost(elasticsearchHost, elasticsearchPort);
            var restClient = RestClient.builder(httpHost).setHttpClientConfigCallback(httpClientConfigCallback).build();
            var transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            var elasticsearchClient = new ElasticsearchClient(transport);
            elasticsearchClient.index(i -> i.index("messages").document(output));
        } catch(Exception e) {
            System.out.println("Error on saveOutput: " + e.getMessage());
        }
    }
}
