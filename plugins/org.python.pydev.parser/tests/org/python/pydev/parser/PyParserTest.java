/*
 * Created on 27/08/2005
 */
package org.python.pydev.parser;

import java.io.File;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.python.pydev.core.ICallback;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.performanceeval.Timer;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.prettyprinter.PrettyPrinter;
import org.python.pydev.parser.prettyprinter.PrettyPrinterPrefs;
import org.python.pydev.parser.prettyprinter.WriterEraser;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;

public class PyParserTest extends PyParserTestBase{

    public static void main(String[] args) {
        try {
            PyParserTest test = new PyParserTest();
            test.setUp();
            
            //Timer timer = new Timer();
            //test.parseFilesInDir(new File("D:/bin/Python251/Lib/site-packages/wx-2.8-msw-unicode"), true);
            //test.parseFilesInDir(new File("D:/bin/Python251/Lib/"), false);
            //timer.printDiff();
            test.testParser14();
//            test.testErr();
            test.tearDown();
            
            
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParserTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        PyParser.USE_FAST_STREAM = true;
    }
    
    
    public void testTryReparse() throws BadLocationException{
        Document doc = new Document("");
        for(int i=0; i< 5; i++){
            doc.replace(0, 0, "this is a totally and completely not parseable doc\n");
        }
        
        PyParser.ParserInfo parserInfo = new PyParser.ParserInfo(doc, true, IPythonNature.LATEST_GRAMMAR_VERSION);
        Tuple<SimpleNode,Throwable> reparseDocument = PyParser.reparseDocument(parserInfo);
        assertTrue(reparseDocument.o1 == null);
        assertTrue(reparseDocument.o2 != null);
    }
    
    public void testCorrectArgs() {
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
        
            public Boolean call(Integer arg) {
                String s = "" +
                "class Class1:         \n" +
                "    def met1(self, a):\n" +
                "        pass";
                SimpleNode node = parseLegalDocStr(s);
                Module m = (Module) node;
                ClassDef d = (ClassDef) m.body[0];
                FunctionDef f = (FunctionDef) d.body[0];
                assertEquals("self",((Name)f.args.args[0]).id);
                assertEquals("a",((Name)f.args.args[1]).id);
                return true;
            }
        });
    }
    
    public void testMultilineStr() {
        final String s = "" +
        "a = '''\n" +
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n" +
        "really really big string\n" +
        "really really big string\n" +
        "'''";
        
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });

    }
    
    public void testErr() {
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                String s = "" +
                "def m():\n" +
                "    call(a,";
                Tuple<SimpleNode, Throwable> tup = parseILegalDocSuccessfully(s);
                Module m = (Module) tup.o1;
                assertEquals(1, m.body.length);
                FunctionDef f = (FunctionDef) m.body[0];
                assertEquals("m", NodeUtils.getRepresentationString(f));
                return true;
            }
        });

    }
    
    
    public void testEmptyBaseForClass() {
        final String s = "" +
        "class B2(): pass\n" +
        "\n" +
        "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                if(arg == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_4){
                    parseILegalDocSuccessfully(s);
                }else{
                    parseLegalDocStr(s);
                }
                return true;
            }
        });
    }
    
    public void testFor2() {
        final String s = "" +
        "[x for x in 1,2,3,4]\n" +
        "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                if(arg == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0){
                    //yeap, invalid in python 3.0
                    parseILegalDocStr(s);
                }else{
                    parseLegalDocStr(s);
                }
                return true;
            }
        });
    }
    
    public void testFor2a() {
        final String s = "" +
        "[x for x in 2,3,4 if x > 2]\n" +
        "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                if(arg == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0){
                    //yeap, invalid in python 3.0
                    parseILegalDocStr(s);
                }else{
                    parseLegalDocStr(s);
                }
                return true;
            }
        });
    }
    
    public void testFor3() {
        final String s = "" +
        "[x() for x in lambda: True, lambda: False if x() ] \n" +
        "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                if(arg == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0){
                    //yeap, invalid in python 3.0
                    parseILegalDocStr(s);
                }else{
                    parseLegalDocStr(s);
                }
                return true;
            }
        });
    }
    
    
    public void testYield() {
        final String s = "" +
                "def m():\n" +
                "    yield 1";
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }
    
    public void testYield2() {
        final String s = "" +
        "class Generator:\n" +
        "    def __iter__(self): \n" +
        "        for a in range(10):\n" +
        "            yield foo(a)\n" +
        "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }

    
    public void testDecorator() {
        final String s = "" +
            "class C:\n" +
            "    \n" +
            "    @staticmethod\n" +
            "    def m():\n" +
            "        pass\n" +
            "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }
    
    public void testDecorator2() {
        final String s = "" +
            "@funcattrs(status=\"experimental\", author=\"BDFL\")\n" +
            "@staticmethod\n" +
            "def longMethodNameForEffect(*args):\n" +
            "    pass\n" +
            "\n" +
            "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }
    
    public void testDecorator4() {
        final String s = "" +
        "@funcattrs(1)\n" +
        "def longMethodNameForEffect(*args):\n" +
        "    pass\n" +
        "\n" +
        "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }
    
    public void testDecorator5() {
        final String s = "" +
        "@funcattrs(a)\n" +
        "def longMethodNameForEffect(*args):\n" +
        "    funcattrs(1)\n" +
        "\n" +
        "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }
    
    public void testDecorator3() {
        final String s = "" +
        "@funcattrs(a, 1, status=\"experimental\", author=\"BDFL\", *args, **kwargs)\n" +
        "@staticmethod1\n" +
        "@staticmethod2(b)\n" +
        "def longMethodNameForEffect(*args):\n" +
        "    pass\n" +
        "\n" +
        "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }
    
    public void testDecorator6() {
        final String s = "" +
        "@funcattrs(b for b in x)\n" +
        "def longMethodNameForEffect(*args):\n" +
        "    pass\n" +
        "\n" +
        "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }
    
    public void testOnNumarray() {
        if(TestDependent.HAS_NUMARRAY_INSTALLED){
            
            File file = new File(TestDependent.PYTHON_NUMARRAY_PACKAGES);
            parseFilesInDir(file);
            file = new File(TestDependent.PYTHON_NUMARRAY_PACKAGES+"linear_algebra/");
            parseFilesInDir(file);
        }
    }
    
    public void testOnWxPython() {
        if(TestDependent.HAS_WXPYTHON_INSTALLED){
            File file = new File(TestDependent.PYTHON_WXPYTHON_PACKAGES+"wxPython");
            parseFilesInDir(file);
            file = new File(TestDependent.PYTHON_WXPYTHON_PACKAGES+"wx");
            parseFilesInDir(file);
        }
    }

    public void testOnCompleteLib() {
        File file = new File(TestDependent.PYTHON_LIB);
        parseFilesInDir(file);
    }

    private void parseFilesInDir(File file) {
        parseFilesInDir(file, false);
    }
    
    
//    not removed completely because we may still want to debug it later...
//    public void testOnCsv() {
//        PyParser.USE_FAST_STREAM = false;
//        String loc = TestDependent.PYTHON_LIB+"csv.py";
//        String s = REF.getFileContents(new File(loc));
//        parseLegalDocStr(s);
//        
//        PyParser.USE_FAST_STREAM = true;
//        loc = TestDependent.PYTHON_LIB+"csv.py";
//        s = REF.getFileContents(new File(loc));
//        parseLegalDocStr(s);
//    }
    
    
    public void testOnCgiMod() {
        final String s = "dict((day, index) for index, daysRep in enumeratedDays for day in daysRep)";
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }
    
    public void testOnCgiMod2() {
        String loc = TestDependent.PYTHON_LIB+"cgi.py";
        String s = REF.getFileContents(new File(loc));
        parseLegalDocStr(s);
    }
    
//    this should really give errors (but is not a priority)
//    public void testErrOnFor() {
//        //ok, it should throw errors in those cases (but that's not so urgent)
//        String s = "foo(x for x in range(10), 100)\n";
//        parseILegalDoc(new Document(s));
//        
//        String s1 = "foo(100, x for x in range(10))\n";
//        parseILegalDoc(new Document(s1));
//        
//    }
    
    public void testOnTestGrammar() {
        String loc = TestDependent.PYTHON_LIB+"test/test_grammar.py";
        String s = REF.getFileContents(new File(loc));
        parseLegalDocStr(s,"(file: test_grammar.py)");
    }
    
    
    public void testSimple() {
        final String s = "" +
                "if maxint == 10:\n"+
                "    for s in 'a':\n"+
                "        pass\n"+
                "else:\n"+
                "    pass\n"+
        		"";
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }
    
    public void testOnTestContextLib() {
        if(TestDependent.HAS_PYTHON_TESTS){
            String loc = TestDependent.PYTHON_LIB+"test/test_contextlib.py";
            String s = REF.getFileContents(new File(loc));
            parseLegalDocStr(s,"(file: test_contextlib.py)");
        }
    }
    
    public void testOnCalendar() {
        String loc = TestDependent.PYTHON_LIB+"hmac.py";
        String s = REF.getFileContents(new File(loc));
        parseLegalDocStr(s);
    }
    
    public void testOnUnittestMod() {
        String loc = TestDependent.PYTHON_LIB+"unittest.py";
        String s = REF.getFileContents(new File(loc));
        parseLegalDocStr(s);
    }
    
    public void testOnCodecsMod() {
        String loc = TestDependent.PYTHON_LIB+"codecs.py";
        String s = REF.getFileContents(new File(loc));
        parseLegalDocStr(s);
    }
    
    public void testOnDocBaseHTTPServer() {
        String loc = TestDependent.PYTHON_LIB+"BaseHTTPServer.py";
        String s = REF.getFileContents(new File(loc));
        parseLegalDocStr(s);
    }
    
    public void testOnDocXMLRPCServerMod() {
        String loc = TestDependent.PYTHON_LIB+"DocXMLRPCServer.py";
        String s = REF.getFileContents(new File(loc));
        parseLegalDocStr(s);
    }
    
    public void testNewImportParser() {
        final String s = "" +
        "from a import (b,\n" +
        "            c,\n" +
        "            d)\n" +
        "\n" +
        "\n" +
        "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }
    
    public void testNewImportParser2() {
        final String s = "" +
        "from a import (b,\n" +
        "            c,\n" +
        "            )\n" +
        "\n" +
        "\n" +
        "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }
    
    public void testNewImportParser3() {
        final String s = "" +
        "from a import (b,\n" +
        "            c,,\n" + //err
        "            )\n" +
        "\n" +
        "\n" +
        "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                parseILegalDocStr(s);
//        Tuple<SimpleNode, Throwable> tup = parseILegalDocSuccessfully(s);
//        Module m = (Module) tup.o1;
//        ImportFrom i = (ImportFrom) m.body[0];
//        assertEquals("a", NodeUtils.getRepresentationString(i.module));
                return true;
            }
        });
    }
    
    public void testParser() {
        String s = "class C: pass";
        parseLegalDocStr(s);
    }

    public void testEndWithComment() {
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                String s = 
                    "class C: \n" +
                    "    pass\n" +
                    "#end\n" +
                    "";
                Module ast = (Module) parseLegalDocStr(s);
                ClassDef d = (ClassDef) ast.body[0];
                assertEquals(1, d.specialsAfter.size());
                commentType c = (commentType) d.specialsAfter.get(0);
                assertEquals("#end", c.id);
                return true;
            }
        });
        
    }
    
    public void testOnlyComment() {
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                String s = 
                    "#end\n" +
                    "\n" +
                    "";
                Module ast = (Module) parseLegalDocStr(s);
                assertEquals(1, ast.specialsBefore.size());
                commentType c = (commentType) ast.specialsBefore.get(0);
                assertEquals("#end", c.id);
                return true;
            }
        });
        
    }
    
    public void testEmpty() {
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                String s = 
                    "";
                Module ast = (Module) parseLegalDocStr(s);
                assertNotNull(ast);
                return true;
            }
        });
    }
    
    public void testParser7() {
        String s = "" +
        "if a < (2, 2):\n"+
        "    False, True = 0, 1\n"+
        "\n"+
        "\n";
        parseLegalDocStr(s);
    }
    
    public void testParser8() {
        String s = "" +
"if type(clsinfo) in (types.TupleType, types.ListType):\n"+
"    pass\n"+
"\n"+
"\n"+
"\n";
        parseLegalDocStr(s);
    }
    
    public void testParser2() {
        String s = "" +
        "td = dict()                                                            \n"+
        "                                                                       \n"+
        "for foo in sorted(val for val in td.itervalues() if val[0] == 's'):    \n"+
        "    print foo                                                          \n";
        
        parseLegalDocStr(s);
    }
    
    public void testParser13() throws Exception {
        final String s = "plural = lambda : None";
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
        
    }
    
    public void testParser3() {
        String s = "print (x for x in y)";
        
        parseLegalDocStr(s);
    }

    public void testParser4() {
        String s = "print sum(x for x in y)";
        
        parseLegalDocStr(s);
    }
    
    public void testParser5() {
        String s = "print sum(x.b for x in y)";
        
        parseLegalDocStr(s);
    }
    
    public void testParser6() {
        String s = "" +
        "import re\n"+
        "def firstMatch(s,regexList):\n"+
        "    for match in (regex.search(s) for regex in regexList):\n"+
        "        if match: return match\n"+
        "\n"+
        "\n";        
        parseLegalDocStr(s);
    }
    
    
    public void testParser9() {
        String s = "" +
        "a[1,]\n"+
        "a[1,2]\n"+
        "\n"+
        "\n"+
        "\n"+
        "\n";        
        parseLegalDocStr(s);
    }
    
    /**
     * l = [ "encode", "decode" ]
     * 
     * expected beginCols at: 7 and 17
     */
    public void testParser10() {
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                String s = "" +
                "l = [ \"encode\", \"decode\" ] \n"+
                "\n";        
                SimpleNode node = parseLegalDocStr(s);
                List<ASTEntry> strs = SequencialASTIteratorVisitor.create(node).getAsList(new Class[]{Str.class});
                assertEquals(7, strs.get(0).node.beginColumn);
                assertEquals(17, strs.get(1).node.beginColumn);
                return true;
            }
        });
    }
    
    
    public void testParser11() {
        final String s = "" +
        "if True:\n"+        
        "    pass\n"+        
        "elif True:\n"+        
        "    pass\n"+        
        "else:\n"+        
        "    pass\n"+        
        "\n"+        
        "\n";        
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }
    
    public void testParser12() {
        final String s = "" +
        "m1()\n"+        
        "\n";        
        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }
    
    
    public void testParser14() {
        final String s = "" +
        "assert False\n"+
        "result = []\n"+
        "for text in header_values:\n"+
        "    pass\n";

        checkWithAllGrammars(new ICallback<Boolean, Integer>(){
            
            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }
    
    
	public void testThreadingInParser() throws Exception {
    	String loc = TestDependent.PYTHON_LIB+"unittest.py";
        String s = REF.getFileContents(new File(loc));

        final Integer[] calls = new Integer[]{0};
        final Boolean[] failedComparisson = new Boolean[]{false};
        
        ICallback<Object, Boolean> callback = new ICallback<Object, Boolean>(){

			public Object call(Boolean failTest) {
			    synchronized (calls) {
			        calls[0] = calls[0]+1;
			        if(failTest){
			            failedComparisson[0] = true;
			        }
			        return null;
                }
			}
        	
        };
        SimpleNode node = parseLegalDocStr(s);
        String expected = printNode(node);
        
        int expectedCalls = 70;
        Timer timer = new Timer();
		for(int j=0;j<expectedCalls;j++){
			startParseThread(s, callback, expected);
		}
		
		while(calls[0] < expectedCalls){
			synchronized(this){
				wait(5);
			}
		}
		timer.printDiff();
		assertTrue(!failedComparisson[0]);
	}

	private String printNode(SimpleNode node) {
      final WriterEraser stringWriter = new WriterEraser();
      PrettyPrinterPrefs prettyPrinterPrefs = new PrettyPrinterPrefs("\n");
      prettyPrinterPrefs.setSpacesAfterComma(1);
      prettyPrinterPrefs.setSpacesBeforeComment(1);
      PrettyPrinter printer = new PrettyPrinter(prettyPrinterPrefs, stringWriter);
      try {
          node.accept(printer);
          return stringWriter.getBuffer().toString();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
	}

	
	private void startParseThread(final String contents, final ICallback<Object, Boolean> callback, 
			final String expected) {
		
		new Thread(){
			public void run() {
				try{
					SimpleNode node = parseLegalDocStr(contents);
					if(!printNode(node).equals(expected)){
						callback.call(true); //Comparison failed
					}else{
						callback.call(false);
					}
				}catch(Throwable e){
					e.printStackTrace();
					callback.call(true); //something bad happened... so, the test failed!
				}
				
			}
		}.start();
		
	}
}