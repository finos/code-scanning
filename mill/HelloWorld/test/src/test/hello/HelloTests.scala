package hello
  
import org.scalatest.funsuite.AnyFunSuite

class HelloSuite extends AnyFunSuite {

    test("Test that ‘Hello’ string is correct") {
        assert(Constants.hello == "Hello, world")
    }

    test ("Another test ...") (pending)

}
