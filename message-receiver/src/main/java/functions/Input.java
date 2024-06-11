package functions;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Input {
    private Integer date;
    private From from;
    private String text;
    private Chat chat;

    public Input() {}

    public Input(Integer date, From from, String text, Chat chat) {
        this.date = date;
        this.from = from;
        this.text = text;
        this.chat = chat;
    }

    public Integer getDate() {
        return date;
    }

    public From getFrom() {
        return from;
    }

    public String getText() {
        return text;
    }

    public Chat getChat() {
        return chat;
    }

    public void setDate(Integer date) {
        this.date = date;
    }

    public void setFrom(From from) {
        this.from = from;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public static class From {
        private Integer id;
    
        @JsonProperty("first_name")
        private String firstName;
    
        @JsonProperty("last_name")
        private String lastName;

        public From() {}

        public From(Integer id, String firstName, String lastName) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public Integer getId() {
            return id;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        @Override
        public String toString() {
            return "From{" +
                    "id='" + id + '\'' +
                    "firstName='" + firstName + '\'' +
                    "lastName='" + lastName + '\'' +
                    "}";
        }
    }

    public static class Chat {
        private Integer id;

        public Chat() {}

        public Chat(Integer id) {
            this.id = id;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "Chat{" +
                    "id='" + id + '\'' +
                    "}";
        }
    }

    @Override
    public String toString() {
        return "Input{" +
                "date='" + date + '\'' +
                "from='" + from + '\'' +
                "text='" + text + '\'' +
                "chat='" + chat + '\'' +
                '}';
    }
}
