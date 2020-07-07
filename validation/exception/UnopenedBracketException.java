package validation.exception;

public class UnopenedBracketException extends Exception {

    public UnopenedBracketException( int index ) {

        super( String.format( "Bracket at index '%s' is not opened.", index ) );

    }

}
