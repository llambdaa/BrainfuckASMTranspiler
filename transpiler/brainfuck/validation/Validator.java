package transpiler.brainfuck.validation;

import transpiler.brainfuck.validation.exception.UnknownOperatorException;
import transpiler.brainfuck.validation.exception.UnclosedBracketException;
import transpiler.brainfuck.validation.exception.UnopenedBracketException;

import java.util.Stack;

/**
 * Not each and every 'Brainfuck' program might be valid because either
 * there are invalid and unknown characters or because brackets are not balanced.
 *
 * Therefore, there is need for a transpiler.brainfuck.validation tool.
 * This static implementation does exactly that:
 * It iterates over each operator and checks whether it is a valid character and whether
 * brackets are balanced.
 *
 * It throws a fitting exception if an error is found.
 */
public class Validator {

    public static boolean isValid( String source ) throws UnknownOperatorException, UnclosedBracketException, UnopenedBracketException {

        /*
        This stack contains the indices of the opening brackets that are
        found within the operator sequence.
        If an opening bracket ('[') is found, its index is put onto the stack;
        if, however, a closing bracket (']') is found, the topmost element is popped.

        By using this technique, it is simple to determine bracket pairs and
        whether there a brackets left over and at which position in the sequence.
         */
        Stack< Integer > brackets = new Stack<>();
        String[] operators = source.split( "" );

        for ( int i = 0; i < source.length(); i++ ) {

            String operator = operators[ i ];

            /*
            The first aspect of this validator is character transpiler.brainfuck.validation.
            If the currently inspected character matches this expression,
            it is a valid operator.
             */
            if ( operator.matches( "[><\\+\\-,\\.\\[\\]]" ) ) {

                if ( operator.equals( "[" ) ) {

                    brackets.add( i );

                } else if ( operator.equals( "]" ) ) {

                    if ( !brackets.isEmpty() ) {

                        brackets.pop();

                    } else {

                        throw new UnopenedBracketException( i );

                    }

                }

            } else {

                throw new UnknownOperatorException( operator, i );

            }

        }

        if ( !brackets.isEmpty() ) {

            throw new UnclosedBracketException( brackets.pop() );

        }

        return true;

    }

}
