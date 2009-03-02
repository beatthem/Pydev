package org.python.pydev.parser.grammarcommon;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.Token;

/**
 * This class contains the error-handling utilities.
 * 
 * @author Fabio
 */
public abstract class AbstractGrammarErrorHandlers extends AbstractGrammarWalkHelpers{

    public static boolean DEBUG = false;
    public final static boolean DEBUG_SHOW_PARSE_ERRORS = PyParser.DEBUG_SHOW_PARSE_ERRORS;
    public final static boolean DEBUG_SHOW_LOADED_TOKENS = false;


    /**
     * @return the actual jjtree used to build the nodes (tree)
     */
    protected abstract IJJTPythonGrammarState getJJTree();

    /**
     * List with the errors we handled during the parsing
     */
    private List<ParseException> parseErrors = new ArrayList<ParseException>();
    
    /**
     * @return a list with the parse errors. Note that the returned value is not a copy, but the actual
     * internal list used to store the errors.
     */
    public List<ParseException> getParseErrors(){
        return parseErrors;
    }
    

    /**
     * Adds some parse exception to the list of parse exceptions found.
     */
    private void addParseError(ParseException e) {
        parseErrors.add(e);
    }

    /**
     * @return the 1st error that happened while parsing (or null if no error happened)
     */
    public Throwable getErrorOnParsing() {
        if(this.parseErrors != null && this.parseErrors.size() > 0){
            return this.parseErrors.get(0);
        }
        return null;
    }
    
    //---------------------------- Helpers to handle errors in the grammar.

    protected final void addAndReport(ParseException e, String msg){
        if(DEBUG_SHOW_PARSE_ERRORS){
            
            System.err.println("\n\n\n\n\n---------------------------------\n"+msg);
            e.printStackTrace();
        }
        addParseError(e);
    }
    
    /**
     * Called when there was an error trying to indent.
     */
    protected final void handleErrorInIndent(ParseException e) throws ParseException{
        addAndReport(e, "Handle indent");
    }
    
    /**
     * Called when there was an error trying to indent.
     */
    protected final void handleNoEof(ParseException e) throws ParseException{
        addAndReport(e, "Handle no EOF");
    }
    
    /**
     * Happens when we could find a parenthesis close (so, we don't create it), but it's
     * not the current, so, we have an error making the match.
     */
    protected final void handleRParensNearButNotCurrent(ParseException e) {
        addAndReport(e, "Handle parens near but not current");
        Token t = getCurrentToken();
        
        AbstractTokenManager tokenManager = getTokenManager();
        final int rparenId = tokenManager.getRparenId();
        while (t != null && t.kind != rparenId) {
            t = t.next;
        }
        if(t != null && t.kind == rparenId){
            //found it
            setCurrentToken(t);
        }
    }
    
    /**
     * Called when there was an error trying to dedent. At this point, we must try to sync it to an
     * actual dedent.
     */
    protected final void handleErrorInDedent(ParseException e) throws ParseException{
        addAndReport(e, "Handle dedent");
        //lot's of tokens, but we'll bail out on an indent, so, that's OK.
        AbstractTokenManager tokenManager = getTokenManager();
        int indentId = tokenManager.getIndentId();
        int dedentId = tokenManager.getDedentId();

        
        int level = 0;

        //lot's of tokens, but we'll bail out on an indent, so, that's OK.
        TokensIterator iterTokens = this.getTokensIterator(getCurrentToken(), 50, false);
        while (iterTokens.hasNext()) {
            Token next = iterTokens.next();
            if(level == 0){
                //we can only do it if we're in the same level we started.
                if(next.kind == dedentId){
                    setCurrentToken(next);
                    break;
                }
            }
            if(next.kind == indentId){
                level += 1;
            }else if(next.kind == dedentId){
                level -= 1;
            }
        }
    }
    
    protected final void handleErrorInStmt(ParseException e) throws ParseException{
        addAndReport(e, "Handle error in stmt");
    }
    

    /**
     * Called when there was an error while resolving a statement.
     */
    protected final void handleErrorInCompountStmt(ParseException e) throws ParseException{
        addAndReport(e, "Handle error in compount stmt");
    }
    
    /**
     * Called when there was an error while resolving a statement.
     */
    protected final void handleNoNewline(ParseException e) throws ParseException{
        addAndReport(e, "Handle no newline");
    }
    
    /**
     * Called when there was an error because the value for a given key was not found.
     */
    protected final void handleNoValInDict(ParseException e) throws ParseException{
        addAndReport(e, "No value for dict key");
    }
    
    
    /**
     * This is called when recognized a suite without finding its indent.
     * 
     * @throws EmptySuiteException if it was called when an empty suite was actually matched (and thus, we should
     * go out of the suite context).
     */
    protected final void handleNoNewlineInSuiteFound() throws EmptySuiteException{
        addAndReport(new ParseException("No new line found.", getCurrentToken()), "Handle no new line in suite");
    }
    
    
    protected final void handleNoSuiteMatch(ParseException e){
        addAndReport(e, "Handle no suite match");
    }

    
    /**
     * Called when there was an error trying to indent.
     * 
     * Actually creates a name so that the parsing can continue.
     */
    protected final Token handleErrorInName(ParseException e) throws ParseException{
        addAndReport(e, "Handle name");
        Token currentToken = getCurrentToken();
        
        return this.getTokenManager().createFrom(currentToken, this.getTokenManager().getNameId(), "!<MissingName>!");
    }
    
}
