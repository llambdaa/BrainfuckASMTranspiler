package parser;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

/**
This abstract class provides a framework implementation
for operator patterns which can be found in a 'Brainfuck' program.

It holds a regular expression (regex) pattern which describes the
sequences that can be translated with its help.
Hence, it can also translate a given sequence but the implementation
has to be given to a respective object of this class.
 */
public abstract class Pattern {

    private java.util.regex.Pattern expression;

    public Pattern( String expression ) {

        this.expression = java.util.regex.Pattern.compile( expression );

    }

    /**
     * When given a sequence, this method returns another sequence (might be a substring
     * of the given sequence) that matches the inherent regular expression of this object.
     *
     * It might as well return 'nothing' (at least nothing useful) if the given sequence
     * doesn't match the pattern.
     * In order to maintain functionality if no match has been made and provide a clear
     * interface for the use of this method, it returns an Optional<String> which is empty
     * if no match is made and otherwise contains the matching sequence.
     */
    public Optional< String > match( String sequence ) {

        Matcher matcher = expression.matcher( sequence );
        return ( matcher.find() ? Optional.of( matcher.group() ) : Optional.empty() );

    }

    /**
     * This method is abstract and hence must be implemented when constructing
     * an object of this class.
     *
     * It returns the translated code in the form of List<String> because
     * each line is saved as one single string.
     */
    public abstract List< String > translate( String slice );

}
