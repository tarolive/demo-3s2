package functions;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;

public class Function {

    @ConfigProperty(name = "telegram.token")
    String telegramToken;

    @Funq
    public CloudEvent<Output> function(CloudEvent<Input> cloudEvent) {

        // print cloudEvent
        System.out.println(cloudEvent);

        // get input
        var input = cloudEvent.data();

        //
        // logic here!
        //

        var message = "The robot is working! Your name is " + input.getFrom().getFirstName() + "!";

        // send telegram response message
        new TelegramBot(telegramToken).execute(new SendMessage(input.getChat().getId(), message));

        // create output
        var output = new Output(message);

        // return
        return CloudEventBuilder.create().build(output);
    }

}
