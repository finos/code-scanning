package test;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;

class Test {
    @GetMapping("/api/bad1")
    @ResponseBody
    public String bad1(@RequestParam String input) {
        ExpressionParser expressionParser = new SpelExpressionParser();
        // ruleid: spring-tainted-code-execution
        Expression expression = expressionParser.parseExpression(input).getValue();
        String result = (String) expression.getValue();
        System.out.println(result);
    }
    @GetMapping("/api/bad1")
    @ResponseBody
    public String bad2(@RequestParam String input) {
        if (expression == null) {
            return null;
          }
        FacesContext context = getFacesContext();
        ELContext elContext = context.getELContext();
        String expressionString = input;
        ExpressionFactory factory = getExpressionFactory();
        // old syntax compatibility
        // #{car[column.property]}
        // new syntax is:
        // #{column.property} or even a method call
        if (expressionString.startsWith("#{" + getVar() + "[")) {
            expressionString = expressionString.substring(expressionString.indexOf("[") + 1, expressionString.indexOf("]"));
            expressionString = "#{" + expressionString + "}";
            // ruleid: spring-tainted-code-execution
            ValueExpression dynaVE = factory.createValueExpression(elContext, expressionString, String.class);
            return (String) dynaVE.getValue(elContext);
        }
        return (String) expression.getValue(elContext);
    }

    @GetMapping("/api/bad3")
    @ResponseBody
    public String bad3(Model model, @RequestParam String input) {
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        String foo = "40+"+input;
        // ruleid: spring-tainted-code-execution
        System.out.println(engine.eval(foo));
    }

    @GetMapping("/api/ok1")
    @ResponseBody
    public String ok1(Model model, @RequestParam String input) {
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        String foo = "40+3";
        // ok: spring-tainted-code-execution
        System.out.println(engine.eval(foo));
    }

    @SuppressWarnings("unchecked") // Forced cast on T
    @Override
    public <T> T evaluateExpressionGet(FacesContext context, String expression, Class<? extends T> expectedType)
        throws ELException {
      // ok: spring-tainted-code-execution
      ValueExpression ve = getExpressionFactory().createValueExpression(context.getELContext(), expression, expectedType);
      return (T) (ve.getValue(context.getELContext()));
    }
}
