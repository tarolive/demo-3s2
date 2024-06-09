package functions;

public class Input {
    private String url;
    private String from;
    private String chat;

    public Input() {}

    public Input(String url, String from, String chat) {
        this.url = url;
        this.from = from;
        this.chat = chat;
    }

    public String getUrl() {
        return url;
    }

    public String getFrom() {
        return from;
    }

    public String getChat() {
        return chat;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setChat(String chat) {
        this.chat = chat;
    }

    @Override
    public String toString() {
        return "Input{" +
                "url='" + url + '\'' +
                "from='" + from + '\'' +
                "chat='" + chat + '\'' +
                '}';
    }
}
