    import java.io.*;
    import java.util.*;

    //SQRL parser
    public class Parse
    {
        private String line;
        private Scanner scan;

        private String[] names;
        private int[] vars;

        // construtor
        public Parse()
        {
            line = "";
            scan = new Scanner(System.in);

            names = new String[256];
            vars = new int[256];
            for (int i=0; i<256; i++)
            {
                names[i]="";
                vars[i]=0;
            }
        }

        // entry point into Parse
        public void run() throws IOException
        {
             String token = "";

             System.out.println("Welcome to SQRL...");

             token = getToken();
             parseCode(token);                   // <sqrl> ::= <code> .
        }


        // parse token for <code>
        private void parseCode(String token) throws IOException
        {
            do {
                parseStmt(token, true);          // <code> ::= <statement> <code>
                token = getToken();
            } while  (!token.equals("."));
        }


        // parse token for <statement>
        private void parseStmt(String token, Boolean execute) throws IOException
        {
            int val;
            String str;

            if (token.equals("load"))            // <statement> ::= load <string>
            {
                token = getToken();
                str = parseString(token);

                // execution part of my parser
                if (execute)
                    line = loadPgm(str) + line;
            }
            else if (token.equals("print"))      // <statement> ::= print <expr> | print <string>
            {
                token = getToken();
                if (token.equals("\""))
                {
                    str = parseString(token);

                    // execution part of my parser
                    if (execute)
                        System.out.println(str);
                }
                else
                {
                    val = parseExpr(token);

                    // exceution part of my parser
                    if (execute)
                        System.out.println(val);
                }
            }
            else if (token.equals("input"))      // <statement> ::= input <var>
            {
                token = getToken();
                if (!isVar(token))
                    reportError(token);

                // execution part of my parser
                if (execute)
                {
                    System.out.print("? ");
                    val = scan.nextInt();
                    storeVar(token, val);
                }
            }
            else if (token.equals("if"))         // <statement> ::= if <cond> <statement>
            {
                Boolean cond;

                token = getToken();
                cond = parseCond(token);

                token = getToken();
                parseStmt(token, execute && cond);

                token = getToken();  // look-ahead
                if (token.equals("else"))        // <statement> ::= if <cond> <statement> else <statement>
                {
                    token = getToken();
                    parseStmt(token, execute && !cond);
                }
                else
                    line = token + line;
            }
            else if (token.equals("repeat"))     // repeat <var> to <val> by <val> <statement>
            {
                String varToken, buffer;
                int start, stop, step;
                Boolean cond;

                varToken = getToken();
                start = parseVar(varToken);

                token = getToken();
                if (!token.equals("to"))
                    reportError(token);

                token = getToken();
                stop = parseVal(token);

                token = getToken();
                if (!token.equals("by"))
                    reportError(token);

                token = getToken();
                step = parseVal(token);

                buffer = line;   // so we can repeat loop
                val = start;

                while (true)
                {
                    cond = ( (step < 0 && val >= stop) || (step >= 0 && val <= stop) );

                    token = getToken();
                    parseStmt(token, execute && cond);

                    if (!(execute && cond))
                        break;

                    val = val + step;
                    storeVar(varToken, val);
                    line = buffer;
                }
                storeVar(varToken, start);

            }
            else if (isVar(token))               // <statement> ::= <var> = <expr>
            {
                String varToken = token;

                token = getToken();
                if (!token.equals("="))
                    reportError(token);

                token = getToken();
                val = parseExpr(token);

                // execution part of my parser
                if (execute)
                    storeVar(varToken, val);
            }
            else if(token.equals(":") || token.equals(";")){
                parseBlock(getToken(),execute);
            }
            else
                reportError(token);
        }


        private void parseBlock(String token, Boolean execute) throws IOException {
            do {
                parseStmt(token, execute); // <block> ::= <statement> <block>
                token = getToken();
            } while (!token.equals(";"));
        }

        // loads program from file
        private String loadPgm(String name) throws IOException
        {
            String buffer = "";
            File file = new File(name);
            Scanner fileScan = new Scanner(file);

            while (fileScan.hasNextLine())
                buffer += fileScan.nextLine() + "\n";
            return buffer;
        }


        // parse <string>
        private String parseString(String token)
        {
            int i;
            String str = "";

            if (!token.equals("\""))
                reportError(token);
            for (i=0; i<line.length(); i++)
                if (line.charAt(i) == '"' || line.charAt(i) == '\n' || line.charAt(i) == '\r')
                    break;
                else
                    str = str + line.charAt(i);

            if (i >= line.length() || line.charAt(i) != '"')
                reportError(token);

            line = line.substring(i+1);

            return str;
        }


        // parse <expr>
    //    private int parseExpr(String token)
    //    {
    //        int val;
    //
    //        val = parseVal(token);
    //        token = getToken();
    //
    //        switch (token.charAt(0))
    //        {
    //            case '+':
    //                token = getToken();
    //                val = val + parseVal(token);  // <expr> ::= <val> + <val>
    //                break;
    //            case '-':
    //                token = getToken();
    //                val = val - parseVal(token);  // <expr> ::= <val> - <val>
    //                break;
    //            case '*':
    //                token = getToken();
    //                val = val * parseVal(token);  // <expr> ::= <val> * <val>
    //                break;
    //            case '/':
    //                token = getToken();
    //                val = val / parseVal(token);  // <expr> ::= <val> / <val>
    //                break;
    //            default:   // oops, a unary expression
    //                line = token + line;
    //        }
    //        return val;
    //    }

    // Modify this method to handle full expression trees
    private int parseExpr(String token) {
        int val;

        if (token.equals("(")) {
            token = getToken();
            val = parseExpr(token); // <expr> ::= ( <expr> )
            if (!getToken().equals(")"))
                reportError(token);
        } else if (token.equals("-")) {
            token = getToken();
            val = -parseExpr(token); // <expr> ::= - <expr>
        } else {
            val = parseVal(token);
            token = getToken();

            switch (token.charAt(0)) {
                case '+':
                    val = val + parseExpr(getToken()); // <expr> ::= <val> + <expr>
                    break;
                case '-':
                    val = val - parseExpr(getToken()); // <expr> ::= <val> - <expr>
                    break;
                case '*':
                    val = val * parseExpr(getToken()); // <expr> ::= <val> * <expr>
                    break;
                case '/':
                    val = val / parseExpr(getToken()); // <expr> ::= <val> / <expr>
                    break;
                default: // oops, a unary expression
                    line = token + line;
            }
        }
        return val;
    }


        // parse <cond>
        private Boolean parseCond(String token)
        {
            int val;
            Boolean cond = false;

            val = parseVal(token);
            token = getToken();

            switch (token.charAt(0))
            {
                case '=':
                    token = getToken();
                    if (!token.equals("="))
                        reportError(token);
                    token = getToken();
                    cond = val == parseVal(token); // <cond> ::= <val> == <val>
                    break;
                case '>':
                    token = getToken();
                    cond = val > parseVal(token);  // <cond> ::= <val> > <val>
                    break;
                case '<':
                    token = getToken();
                    cond = val < parseVal(token);  // <cond> ::= <val> < <val>
                    break;
                default:
                    reportError(token);
            }
            return cond;
        }


        // parses token for <val> returns an integer value
        private int parseVal(String token)
        {
            if (isNumeric(token))
               return Integer.parseInt(token);
            if (isVar(token))
               return parseVar(token);

            reportError(token);

            return -1;  // will never happen
        }


        // hash function
        private int hash(String token)
        {
            int val = 0;
            for (int i=0; i<token.length(); i++)
                val = val + (i+1)*(int)token.charAt(i);

            return val % 256;
        }


        // store value into <var>
        private void storeVar(String token, int val)
        {
            int i, start = hash(token);

            i = start;
            while (true)
            {
                if (names[i].equals(token) || names[i].isEmpty())
                {
                    names[i] = token;
                    vars[i] = val;
                    break;
                }
                i = (i + 1) % 256;

                if (i == start)
                    reportError(token);
            }
        }


        // checks to see if token is a <num>
        private boolean isNumeric(String token)
        {
            for (int i=0; i<token.length(); i++)
                if ( !Character.isDigit(token.charAt(i)) )
                    return false;

            return true;
        }


        // checks to see if token is a <var>
        private boolean isVar(String token)
        {

            //return (token.length() == 1 && isAlpha(token.charAt(0)));

            if (token.charAt(0) != '_' && !isAlpha(token.charAt(0)))
                return false;
            for (int i=1; i<token.length(); i++)
                if (token.charAt(i) != '_' && !isAlpha(token.charAt(i)) && !Character.isDigit(token.charAt(i)))
                    return false;

            return true;
        }


        // is it a to z?
        private boolean isAlpha(char ch)
        {
            return (((int)ch) >= 65 && ((int)ch) <= 90) || (((int)ch) >= 97 && ((int)ch) <= 122);
        }


        // parses <var> and returns integer value
        private int parseVar(String token)
        {
            int i, start;

            if (!isVar(token))
                reportError(token);

            //return vars[ ((int)token.charAt(0)) - 97 ];

            i = start = hash(token);
            while (true)
            {
                if (names[i].equals(token) || names[i].isEmpty())
                {
                    names[i] = token;
                    return vars[i];
                }

                i = (i + 1) % 256;

                if (i == start)
                    reportError(token);
            }
        }


        // reports syntax error
        private void reportError(String token)
        {
            line += "\n";
            line = line.substring(0, line.indexOf("\n"));

            System.out.println("ERROR: " + token + line);
            for (int i=0; i<token.length()+6; i++)
                System.out.print(" ");
            System.out.println("^");

            System.exit(-1);
        }

    //--------------------------------- Lexical Analyzer -------------------------------

        // check for blank space
        private boolean isBlank(char ch)
        {
            switch(ch)
            {
                case ' ':
                case '\t':
                case '\n': case '\r':
                    return true;
                default:
                    return false;
            }
        }


        // check for delimeter
        private boolean isDelim(char ch)
        {
            switch(ch)
            {
                case '.':
                case '\"':
                case '+': case '-': case '*': case '/':
                case '>': case '<': case '=':
                    return true;
                default:
                    return isBlank(ch);
            }
        }


        // skips leading blanks
        private String skipLeadingBlanks(String buffer)
        {
            int i=0;

            while (i<buffer.length())
            {
                if (buffer.charAt(i) == '#')   // take care of comments
                {
                    buffer = buffer.substring(i);     // skip to # so we can
                    if (buffer.indexOf("\n") == -1)   // search from # to '\n'
                        buffer = "";
                    else
                        buffer = buffer.substring(buffer.indexOf("\n"));

                    i=0;
                    continue;
                }
                else if ( !isBlank(buffer.charAt(i)) )
                    break;

                i++;
            }

            return buffer.substring(i);
        }


        // grab the next token
        private String getToken()
        {
            String token;

            line = skipLeadingBlanks(line);
            while (line.length() == 0)
            {
                line = scan.nextLine();
                line = skipLeadingBlanks(line);
            }

            for (int i=0; i<line.length(); i++)
                if ( isDelim(line.charAt(i)) )  // we hit a delimeter
                {
                    if (i==0)   // if our token is a delimter it's 1 char long
                        i++;

                    token = line.substring(0,i);
                    line = line.substring(i);

                    return token;
                }

            // enter is out delimeter
            token = line;
            line = "";
            return token;
        }
    }
