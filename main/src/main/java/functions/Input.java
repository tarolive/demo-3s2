package functions;

public class Input {
    private Double date;
    private From from;
    private String text;

    public Input() {}

    public Input(Double date, From from, String text) {
        this.date = date;
        this.from = from;
        this.text = text;
    }

    public Double getDate() {
        return date;
    }

    public From getFrom() {
        return from;
    }

    public String getText() {
        return text;
    }

    public void setDate(Double date) {
        this.date = date;
    }

    public void setFrom(From from) {
        this.from = from;
    }

    public void setText(String text) {
        this.text = text;
    }

    public static class From {
        private Double id;
        private String firstName;
        private String lastName;

        public From() {}

        public From(Double id, String firstName, String lastName) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public Double getId() {
            return id;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setId(Double id) {
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

    @Override
    public String toString() {
        return "Input{" +
                "date='" + date + '\'' +
                "from='" + from + '\'' +
                "text='" + text + '\'' +
                '}';
    }
}
