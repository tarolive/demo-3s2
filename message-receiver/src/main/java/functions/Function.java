package functions;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;

import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
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

import nl.altindag.ssl.SSLFactory;

import java.util.List;

public class Function {

    @ConfigProperty(name = "telegram.token")
    String telegramToken;

    @ConfigProperty(name = "elasticsearch.host")
    String elasticsearchHost;

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
        var message = handleText(firstName, lastName, text);

        // send telegram response message
        new TelegramBot(telegramToken).execute(new SendMessage(input.getChat().getId(), message));

        // create output
        var output = new Output(date, firstName, lastName, text, message);
        saveOutput(output, "messages");

        // return
        return CloudEventBuilder.create().build(output);
    }

    private String handleText(String firstName, String lastName, String text) {

        if (text == null || text == "" || text.startsWith("/start")) {
            return "Olá " + firstName + ", eu sou o assistente 3s2! Como posso te ajudar?";
        }

        if (text.startsWith("/history")) {
            return getHistory(firstName, lastName);
        }

        if (text.startsWith("/itsm")) {
            var history = getHistory(firstName, lastName);
            var ticket = openTicket(history);
            return "Ticket aberto: " + ticket;
        }

        return "Não entendi sua pergunta, poderia repetir?";
    }

    private String getHistory(String firstName, String lastName) {
        try {
            var es = createElasticsearchClient();
            var history = "Seu histórico: \n";

            var byFirstName = MatchQuery.of(m -> m
                .field("firstName")
                .query(firstName)
            )._toQuery();;

            var byLastName = MatchQuery.of(m -> m
                .field("lastName")
                .query(lastName)
            )._toQuery();;

            SearchResponse<Output> response = es.search(s -> s
                .index("messages")
                .query(q -> q
                    .bool(b -> b
                        .must(byFirstName)
                        .must(byLastName)
                    )
                ),
                Output.class
            );

            List<Hit<Output>> hits = response.hits().hits();
            for(Hit<Output> hit : hits) {
                Output out = hit.source();
                history += "\nPergunta: " + out.getText() + "\nResposta: " + out.getMessage();
            }

            return history;
        } catch(Exception e) {
            System.out.println("Error on getHistory: " + e.getMessage());
            return "Ocorreu um erro ao buscar histórico :(";
        }
    }

    private String openTicket(String history) {
        return "123";
    }

    private void saveOutput(Output output, String index) {
        try {
            var elasticsearchClient = createElasticsearchClient();
            elasticsearchClient.index(i -> i.index(index).document(output));
        } catch(Exception e) {
            System.out.println("Error on saveOutput: " + e.getMessage());
        }
    }

    private ElasticsearchClient createElasticsearchClient() {
        var credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(elasticsearchUsername, elasticsearchPassword));

        var httpClientConfigCallback = new HttpClientConfigCallback(){
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpAsyncClientBuilder) {
                SSLFactory sslFactory = SSLFactory.builder()
                    .withUnsafeTrustMaterial()
                    .withUnsafeHostnameVerifier()
                    .build();

                return httpAsyncClientBuilder
                    .setSSLHostnameVerifier((host, sslSession) -> true)
                    .setSSLContext(sslFactory.getSslContext())
                    .setDefaultCredentialsProvider(credentialsProvider);
            }
        };

        var httpHost = HttpHost.create(elasticsearchHost);
        var restClient = RestClient.builder(httpHost).setHttpClientConfigCallback(httpClientConfigCallback).build();
        var transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        var elasticsearchClient = new ElasticsearchClient(transport);

        return elasticsearchClient;
    }
}
