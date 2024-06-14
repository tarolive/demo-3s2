package functions;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.StringBuilder;
import java.net.URL;
import javax.net.ssl.HostnameVerifier;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.DownloadObjectArgs;
import io.minio.errors.MinioException;

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
        var chat = input.getChat();
        var chatId = chat.getId();
        var photo = input.getPhoto();

        // create message
        var message = handleText(firstName, lastName, text);

        // send telegram response message
        if (photo == null || photo.size() == 0) new TelegramBot(telegramToken).execute(new SendMessage(input.getChat().getId(), message));

        // create output
        var output = new Output(date, firstName, lastName, text, message);
        var index = "messages";
        if (text != null && text.startsWith("/itsm")) index = "tickets";
        if (text != null && !text.startsWith("/history")) saveOutput(output, index);

        if (photo != null && photo.size() > 0) {
            handlePhoto(chatId, photo);
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

    private void handlePhoto(Integer chatId, List<Input.Photo> photo) {
        try {
            var fileId = photo.get(0).getFileId();
            var endpoint = "https://api.telegram.org/bot" + telegramToken + "/getFile?file_id=" + fileId;
            var url = new URL(endpoint);
            var connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            connection.setDoOutput(true);
            var rawResponse = new StringBuilder();
            try (var in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                var line = "";
                while((line = in.readLine()) != null) {
                    rawResponse.append(line);
                }
            }
            connection.disconnect();
            var response = new JSONObject(rawResponse.toString());
            var filePath = String.valueOf(response.getJSONObject("result").get("file_path"));
            var fileUrl = new URL("https://api.telegram.org/file/bot" + telegramToken + "/" + filePath);
            var in = new BufferedInputStream(fileUrl.openStream());
            var out = new ByteArrayOutputStream();
            var buf = new byte[1024];
            var n = 0;
            while (-1!=(n=in.read(buf))) {
                out.write(buf, 0, n);
            }
            out.close();
            in.close();
            var r = out.toByteArray();
            var filename = String.valueOf(response.getJSONObject("result").get("file_unique_id")) + ".jpg";
            var fos = new FileOutputStream(filename);
            fos.write(r);
            fos.close();
            var minioClient = MinioClient.builder()
                .endpoint("https://minio-api-minio.apps.cluster-lsc68.dynamic.redhatworkshops.io")
                .credentials("minio", "minio123")
                .build();
            minioClient.uploadObject(UploadObjectArgs.builder()
                .bucket("yoloimages")
                .object("/upload/" + filename)
                .filename(filename)
                .build());
            new File(filename).delete();
            endpoint = "https://python-python-yolo.apps.cluster-lsc68.dynamic.redhatworkshops.io/yolo_infer";
            url = new URL(endpoint);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            connection.setDoOutput(true);
            try (var o = connection.getOutputStream()) {
                var input = ("{\"s3_image_name\": \"" + filename + "\"}").getBytes("utf-8");
                o.write(input, 0, input.length);           
            }
             rawResponse = new StringBuilder();
            try (var i = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                var line = "";
                while((line = i.readLine()) != null) {
                    rawResponse.append(line);
                }
            }
            connection.disconnect();
            minioClient.downloadObject(DownloadObjectArgs.builder()
                .bucket("yoloimages")
                .object("/inference/" + filename)
                .filename(filename)
                .build());

            new TelegramBot(telegramToken).execute(new SendMessage(chatId, "Segue análise da sua image..."));
            new TelegramBot(telegramToken).execute(new SendPhoto(chatId, new File(filename)));

        } catch (Exception e) {
            System.out.println("Error on handlePhoto: " + e.getMessage());
        }
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
