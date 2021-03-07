package transpiler.brainfuck.core;

import transpiler.brainfuck.parser.Parser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * This program transpiles 'Brainfuck' to 'NASM assembly' (Linux)
 * while making some optimizations.
 */
public class Transpiler {

    public static void main( String ... arguments ) {

        try {

            /*
            There are only two valid arguments:

                -> arguments[0x0] = source directory
                -> arguments[0x1] = destination directory
             */
            if ( arguments.length == 0x2 ) {

                /*
                The coarse structure of the transpiled programs is described by the file '/transpiler.brainfuck.template/transpiler.brainfuck.template.asm'.
                It first is loaded from inside the JAR with help of the Scanner implementation and then
                certain placeholders (e.g. %SOURCE% which will contain the transpiled source code) are
                replaced with the actual values.

                The transpilation work is actually done by the parser which reads in the operator sequence
                of the given 'Brainfuck' program and translates it into 'NASM assembly' while making optimizations.
                 */
                Scanner scanner = new Scanner( Transpiler.class.getResourceAsStream("/transpiler/brainfuck/template/template.asm") ).useDelimiter( "\\A" );
                String template = scanner.hasNext() ? scanner.next() : "";
                String modified = template.replaceAll( "%STACK_SIZE_REGISTER%", Parser.STACK_SIZE_REGISTER )
                                          .replaceAll( "%STACK_SIZE%", Parser.STACK_SIZE )
                                          .replaceAll( "%POINTER_REGISTER%", Parser.INDEX_REGISTER )
                                          .replaceAll( "%SOURCE%", Parser.parse( Paths.get( arguments[ 0 ] ) ) );

                /*
                After transpilation, the assembly source code is then
                transferred into the specified destination file.
                 */
                PrintWriter writer = new PrintWriter( arguments[ 1 ], StandardCharsets.UTF_8 );
                writer.println( modified );
                writer.close();

            } else {

                throw new IllegalArgumentException( "Illegal argument count.\nArguments: <source> <destination>" );

            }

        } catch ( IOException exception ) {

            exception.printStackTrace();

        }

    }

}
