package hello;

import org.joda.time.LocalTime;

public class HelloWorld {
    public static void main(String[] args) {
      LocalTime currentTime = new LocalTime();
        // nosemgrep
		System.out.println("The current local time is: " + currentTime);

        Greeter greeter = new Greeter();
        // nosemgrep
        System.out.println(greeter.sayHello());
    }
}
