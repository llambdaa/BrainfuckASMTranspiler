package transpiler.brainfuck.validation.exception;

public class UnknownOperatorException extends Exception {

    public UnknownOperatorException( String operator, int index ) {

        super( String.format( "Operator '%s' at index '%s' is not known.", operator, index ) );

    }

}
