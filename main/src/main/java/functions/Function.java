package functions;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventBuilder;

/**
 * Your Function class
 */
public class Function {

    /**
     * Use the Quarkus Funq extension for the function. This example
     * function simply echoes its input data.
     * @param input a CloudEvent
     * @return a CloudEvent
     */
    @Funq
    public CloudEvent<Output> function(CloudEvent<Input> input) {

        // Add your business logic here

        System.out.println(input);
        Output output = new Output("The robot is working! Your name is " + input.data().getFrom().getFirstName() + "!");
        return CloudEventBuilder.create().build(output);
    }

}
