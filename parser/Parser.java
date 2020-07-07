package parser;

import validation.Validator;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Parser {

    /**
     * These static members of the Parser class are
     * the placeholder replacements for the framework
     * that is defined within '/template/template.asm'
     *
     * Furthermore 'PATTERNS' holds each pattern with
     * its respective expressions that are used for transpilation.
     */
    private static final List< Pattern > PATTERNS           = new ArrayList<>();
    public static final String STACK_SIZE           = "0x4";
    public static final String STACK_SIZE_REGISTER  = "rcx";
    public static final String INDEX_REGISTER       = "rbx";
    private static final String CACHE_REGISTER       = "cl";

    /**
     * Statically, the pattern used for operator recognition are loaded.
     *
     * They contain the rules for translation of several patterns.
     * Since that also requires knowledge of '(NASM) assembly' and describing several
     * steps is neither very illuminating (e.g. certain syscalls) nor helps
     * understanding what this structures do, detailed commenting cannot
     * be found here.
     * However, there is an explanation to what each pattern recognizes and
     * how it optimizes the resulting code.
     */
    static {

        /*
        This pattern matches sequences that have to do with printing characters.
        These use the printing operator ('.').

        Calls to the operating system (syscalls) are expensive and thus the goal
        is to reduce these calls to a minimum (of only one per coherent sequence).
        The pointer shifting operators ('>' and '<') as well as the cell value
        changing operators ('+' and '-') can be found between single print calls.

        These only effect the values or rather characters that are printed.
        For the progression of the program, the print calls do not
        really have to take place after another since only values but not
        the amount of characters to be printed can change.
        The final print call can therefore happen when the last one of
        a coherent sequence would take place.

        For the purpose of reducing syscalls, a buffer is made which can hold
        each character that might either be saved inside a cell that the pointer
        has been navigated to or that might be changed.
        Inside the operator sequence the printing operators denote that the current
        value shall be buffered until the last operator of this kind which tells
        the program to print the whole buffer at once only using one syscall.
         */
        PATTERNS.add( new Pattern( "^\\.([\\.><+-]*\\.+)?" ) {

            @Override
            public List< String > translate( String slice ) {

                List< String > transpiled = new ArrayList<>();

                String buffer = Integer.toHexString( slice.replaceAll( "[^\\.]", "" ).length() );
                transpiled.add( String.format( "\tsub\trsp, 0x%s\n", buffer ) );

                String[] fragments = slice.split( "((?<=\\.)|(?=\\.))" );
                int calls = 0x0;

                for ( String fragment : fragments ) {

                    if ( fragment.equals( "." ) ) {

                        transpiled.add( String.format( "\tmov\t%s, [%s]\n", CACHE_REGISTER, INDEX_REGISTER ) );
                        transpiled.add( String.format( "\tmov\t[rsp+0x%s], %s\n", Integer.toHexString( calls ), CACHE_REGISTER ) );
                        calls++;

                    } else {

                        transpiled.addAll( Parser.parse( fragment ) );

                    }

                }

                transpiled.add( "\tmov\trsi, rsp\n" );
                transpiled.add( String.format( "\tmov\trdx, 0x%s\n", buffer ) );
                transpiled.add( "\tmov\trdi, 0x1\n" );
                transpiled.add( "\tmov\trax, 0x1\n" );
                transpiled.add( "\tsyscall\n" );
                transpiled.add( String.format( "\tadd\trsp, 0x%s\n", buffer ) );

                return transpiled;

            }

        } );

        /*
        This pattern matches sequences that have to do with reading keyboard input.
        These use the printing operator (',').

        Calls to the operating system (syscalls) are expensive and thus the goal
        is to reduce these calls to a minimum (of only one per coherent sequence).
        The pointer shifting operators ('>' and '<') as well as the cell value
        changing operators ('+' and '-') can be found between single reading calls.

        Operators that change cell states between two read operators can savely be
        ignored in certain cases since these state changes are overwritten as soon
        as a character is read.

        Coherent sequences in this context are defined as sequences that hold
        character sequences between the read operators that equal a pointer
        redirection to the right ('>').
        Only if the sub sequences are equivalent to this operator, multiple
        read operations can be bundled and executed with one syscall since
        then subsequent characters of the sequence that is inputted by the keyboard
        map to subsequent cells on the program stack.
        The keyboard input is then read and saved into subsequent cells.
         */
        PATTERNS.add( new Pattern( "^,([,><+-]*,+)?" ) {

            @Override
            public List< String > translate( String slice ) {

                List< String > transpiled = new ArrayList<>();
                String[] fragments = slice.split( "((?<=,)|(?=,))" );

                int length = 0x0;
                int element = 0x0;
                for ( String fragment : fragments ) {

                    if ( fragment.equals( "," ) ) {

                        length++;

                    } else {

                        int shifts = 0x0;

                        for ( char operator : fragment.toCharArray() ) {

                            shifts += ( operator == '>' ? 0x1 : ( operator == '<' ? -0x1 : 0x0 ) );

                        }

                        if ( shifts != 1 ) {

                            break;

                        }

                    }

                    element++;

                }

                transpiled.add( String.format( "\tmov\trsi, %s\n", INDEX_REGISTER ) );
                transpiled.add( String.format( "\tmov\trdx, 0x%s\n", Integer.toHexString( length ) ) );
                transpiled.add( "\tmov\trdi, 0x0\n" );
                transpiled.add( "\tmov\trax, 0x0\n" );
                transpiled.add( "\tsyscall\n" );
                transpiled.add( String.format( "\tadd\t%s, 0x%s\n", INDEX_REGISTER, Integer.toHexString( length - 1 ) ) );

                if ( element < fragments.length ) {

                    StringBuilder builder = new StringBuilder();
                    for ( int i = element; i < fragments.length; i++ ) {

                        builder.append( fragments[ i ] );

                    }

                    transpiled.addAll( Parser.parse( builder.toString() ) );

                }

                return transpiled;

            }

        } );

        /*
        Addition and subtraction operations annihilate their respective
        effects to the cells state.

        If these operations follow each other in a coherent sequence that
        is described by the patterns regular expression, the total
        outcome (pure addition, pure subtraction, nothing) can be precomputed.

        There is simply no need to split the sequence up into smaller
        additions or subtractions and hence performing more operations
        if there is a way to achieve the same outcome with less calculations.
         */
        PATTERNS.add( new Pattern( "^[+-]+" ) {

            @Override
            public List< String > translate( String slice ) {

                List< String > transpiled = new ArrayList<>();

                int value = 0x0;
                for ( char operator : slice.toCharArray() ) {

                    value += ( operator == '+' ? 0x1 : -0x1 );

                }

                if ( value != 0 ) {

                    transpiled.add( String.format( "\tmov\t%s, [%s]\n", CACHE_REGISTER, INDEX_REGISTER ) );
                    transpiled.add( String.format( "\t%s\t%s, %s\n", ( value > 0 ? "add" : "sub" ), CACHE_REGISTER, Math.abs( value ) ) );
                    transpiled.add( String.format( "\tmov\t[%s], %s\n", INDEX_REGISTER, CACHE_REGISTER ) );

                }

                return transpiled;

            }

        } );

        /*
        Pointer redirection operations (left and right) annihilate their respective
        effects to the program pointer.

        If these operations follow each other in a coherent sequence that
        is described by the patterns regular expression, the total
        outcome (right shift, left shift, no shift) can be precomputed.

        There is simply no need to split the sequence up into smaller
        left shifts or right shifts of the pointer and hence performing more operations
        if there is a way to achieve the same outcome with less calculations.
         */
        PATTERNS.add( new Pattern( "^[><]+" ) {

            @Override
            public List< String > translate( String slice ) {

                List< String > transpiled = new ArrayList<>();

                int value = 0x0;
                for ( char operator : slice.toCharArray() ) {

                    value += ( operator == '>' ? 0x1 : -0x1 );

                }

                if ( value != 0 ) {

                    transpiled.add( String.format( "\t%s\t%s, 0x%s\n", ( value > 0x0 ? "add" : "sub" ), INDEX_REGISTER, Integer.toHexString( Math.abs( value ) ) ) );

                }

                return transpiled;

            }

        } );

        /*
        This pattern, with the help of bracket matchers inside the parser
        implementation itself, matches sequences that contain loops.

        It generated the basic framework for loops (conditions, counter, labels
        to jump to) and deposits the code that is being held by the loops brackets
        inside this framework.

        The loops content itself is parsed recursively.
        This allows for nested (loops inside loops) and neighbored loops while
        also providing simplifications and optimizations (see above patterns).

        Each loops needs custom label naming.
        Generators might generate strings that are the same, hence, it might, depending
        on the generator being used, be necessary to use some kind of dictionary of
        sequences that already have been generated.

        This procedure is unnecessary:
        It can be simplified by using timestamps (System.nanoTime()) as a label extension
        which guarantees different labels.
         */
        PATTERNS.add( new Pattern( "^\\[" ) {

            @Override
            public List< String > translate( String slice ) {

                List< String > transpiled = new ArrayList<>();

                if ( slice.length() > 0x2 ) {

                    List< String > parsed = Parser.parse( slice.substring( 0x1, slice.length() - 0x1 ) );

                    if ( !parsed.isEmpty() ) {

                        String id = String.valueOf( System.nanoTime() );

                        transpiled.add( String.format( ".loop_%s:\n", id ) );
                        transpiled.add( String.format( "\tcmp\tbyte [%s], 0x0\n", INDEX_REGISTER ) );
                        transpiled.add( String.format( "\tje\t.exit_%s\n", id ) );
                        transpiled.addAll( parsed );
                        transpiled.add( String.format( "\tjmp\t.loop_%s\n", id ) );
                        transpiled.add( String.format( ".exit_%s:\n", id ) );

                    }

                }

                return transpiled;

            }

        } );

    }

    /**
     * This method is the entry point for the parsing process
     * which is in turn defined recursively in another method.
     *
     * It takes in the path to the source file which contains the 'Brainfuck'
     * source code, validates it and subsequently parses it, effectively
     * translating it into 'NASM assembly'.
     */
    public static String parse( Path path ) {

        try {

            String content = new String( Files.readAllBytes( path ), StandardCharsets.US_ASCII ).replaceAll( "\n", "" );

            if ( content.length() > 0x0 ) {

                if ( Validator.isValid( content ) ) {

                    StringBuilder builder = new StringBuilder();
                    Parser.parse( content ).forEach( builder::append );

                    return builder.toString();

                }

            } else {

                return new String();

            }

        } catch ( Exception exception ) {

            exception.printStackTrace();

        }

        return null;

    }

    /**
     * Parsing itself is defined recursively for this purpose.
     *
     * The parser takes in a sequence and searches for the pattern
     * that matches the best, that is, the one that matches the longest
     * sub sequence.
     *
     * Then, this sub sequence is removed from the input sequence and the rest
     * is given back to this method - basically recursively calling it.
     *
     * The recursive nature of this method is also very useful when parsing
     * brackets.
     * Their content can be passed into this method and if there is no
     * useful output because there either is no code inside the brackets
     * or operations inside them annihilate each other, the brackets can
     * simply be discarded.
     */
    private static List< String > parse( String slice ) {

        // Search for the optimal pattern
        String match = null;
        Pattern matcher = null;

        for ( Pattern pattern : PATTERNS ) {

            Optional< String > result = pattern.match( slice );

            if ( result.isPresent() && ( match == null || match.length() < result.get().length() ) ) {

                match = result.get();
                matcher = pattern;

            }

        }

        /*
        Brackets can be nested but regular expression engines
        don't support recursive descriptions.
        Therefore, it isn't possible to describe a pattern using regular expressions that
        is initiated with an opening bracket and terminated with a closing bracket
        while guaranteeing that these brackets are indeed a pair in all situations.
        Thus, in order for the 'bracket pattern' to work, the matching bracket to the
        one the sequence starts with must be found, so that the sequence between these two
        can be given to just this pattern so that it can translate.

        The algorithm is very similar to the one that is used in the Validator (@Validator.java).
         */
        if ( slice.startsWith( "[" ) ) {

            int brackets = 0x0;
            for ( int i = 0x0; i < slice.length(); i++ ) {

                char operator = slice.charAt( i );

                if ( operator == '[' ) {

                    brackets++;

                } else if ( operator == ']' ) {

                    brackets--;

                    if ( brackets == 0x0 ) {

                        match = slice.substring( 0x0, i + 0x1 );
                        break;

                    }

                }

            }

        }

        List< String > transpiled = matcher.translate( match );

        if ( match.length() < slice.length() ) {

            // Recursively calling this method in order to parse
            // the remaining sequence
            transpiled.addAll( Parser.parse( slice.substring( match.length() ) ) );

        }

        return transpiled;

    }

}
