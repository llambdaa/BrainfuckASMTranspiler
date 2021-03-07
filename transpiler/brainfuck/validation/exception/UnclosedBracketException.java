package transpiler.brainfuck.validation.exception;

public class UnclosedBracketException extends Exception {

    public UnclosedBracketException( int index ) {

        super( String.format( "Bracket at index '%s' is not closed.", index ) );

    }

}
