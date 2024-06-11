package functions;

public class Output {
    private Integer date;
    private String firstName;
    private String lastName;
    private String text;
    private String message;

    public Output() {}

    public Output(Integer date, String firstName, String lastName, String text, String message) {
        this.date = date;
        this.firstName = firstName;
        this.lastName = lastName;
        this.text = text;
        this.message = message;
    }

    public Integer getDate() {
        return date;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getText() {
        return text;
    }

    public String getMessage() {
        return message;
    }

    public void setDate(Integer date) {
        this.date = date;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Output{" +
                "date='" + date + '\'' +
                "firstName='" + firstName + '\'' +
                "lastName='" + lastName + '\'' +
                "text='" + text + '\'' +
                "message='" + message + '\'' +
                '}';
    }
}
