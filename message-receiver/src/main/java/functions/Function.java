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
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.StringBuilder;
import java.net.URL;
import javax.net.ssl.HostnameVerifier;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

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
        var photo = input.getPhoto();

        // create message
        var message = handleText(firstName, lastName, text);

        // send telegram response message
        new TelegramBot(telegramToken).execute(new SendMessage(input.getChat().getId(), message));

        // create output
        var output = new Output(date, firstName, lastName, text, message);
        var index = "messages";
        if (text != null && text.startsWith("/itsm")) index = "tickets";
        if (text != null && !text.startsWith("/history")) saveOutput(output, index);

        if (photo != null && photo.lenght() > 0) {
            handlePhoto(photo);
        }

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
            history = history.replaceAll("Seu histórico de mensagens:", "");
            return "Ticket aberto com o histórico de mensagens: " + history;
        }

        return callGemini(firstName, lastName, text);
    }

    private void handlePhoto(List<Photo> photo) {
        System.out.println("Analisando image...");
    }

    private String callGemini(String firstName, String lastName, String text) {
            try {
                var endpoint = "https://python-gemini-python-gemini.apps.cluster-lsc68.dynamic.redhatworkshops.io/ask_gemini";
                var url = new URL(endpoint);
                var connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
                connection.setDoOutput(true);
                try (var os = connection.getOutputStream()) {
                    var input = ("{\"text\": \"" + text + "\"}").getBytes("utf-8");
                    os.write(input, 0, input.length);           
                }
                var rawResponse = new StringBuilder();
                try (var in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    var line = "";
                    while((line = in.readLine()) != null) {
                        rawResponse.append(line);
                    }
                }
                connection.disconnect();
                var response = new JSONObject(rawResponse.toString().replaceAll("answer: ", ""));
                return String.valueOf(response.get("gemini_response"));
            } catch(Exception e) {
                System.out.println("Error on callGemini: " + e.getMessage());
                return "Ocorreu um erro ao chamar API do Gemini :(";
            }
    }

    private String getHistory(String firstName, String lastName) {
        try {
            var es = createElasticsearchClient();
            var history = "Seu histórico de mensagens:";

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
