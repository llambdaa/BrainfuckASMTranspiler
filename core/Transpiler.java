package core;

import parser.Parser;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * This program transpiles 'Brainfuck' to 'NASM assembly' (Linux)
 * while making some optimizations.
 *
 * Author: Lukas Rapp
 * Date:   July 7th 2020
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
                The coarse structure of the transpiled programs is described by the file '/template/template.asm'.
                It first is loaded from inside the JAR with help of the Scanner implementation and then
                certain placeholders (e.g. %SOURCE% which will contain the transpiled source code) are
                replaced with the actual values.

                The transpilation work is actually done by the parser which reads in the operator sequence
                of the given 'Brainfuck' program and translates it into 'NASM assembly' while making optimizations.
                 */
                Scanner scanner = new Scanner( Transpiler.class.getResourceAsStream( "/template/template.asm" ) ).useDelimiter( "\\A" );
                String template = scanner.hasNext() ? scanner.next() : "";
                String modified = template.replaceAll( "%STACK_SIZE_REGISTER%", Parser.STACK_SIZE_REGISTER )
                                          .replaceAll( "%STACK_SIZE%", Parser.STACK_SIZE )
                                          .replaceAll( "%POINTER_REGISTER%", Parser.INDEX_REGISTER )
                                          .replaceAll( "%SOURCE%", Parser.parse( Paths.get( arguments[ 0 ] ) ) );

                /*
                After transpilation, the assembly source code is then
                transferred into the specified destination file.
                 */
                PrintWriter writer = new PrintWriter( arguments[ 1 ], "UTF-8" );
                writer.println( modified );
                writer.close();

            } else {

                throw new IllegalArgumentException( "Illegal argument count.\nArguments: <source> <destination>" );

            }

        } catch ( FileNotFoundException | UnsupportedEncodingException exception ) {

            exception.printStackTrace();

        }

    }

}
