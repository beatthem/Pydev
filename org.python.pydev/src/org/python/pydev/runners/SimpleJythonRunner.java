/*
 * Created on 05/08/2005
 */
package org.python.pydev.runners;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.editor.codecompletion.revisited.TestDependent;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.IInterpreterManager;

public class SimpleJythonRunner extends SimpleRunner{
    /**
     * This is the method that actually does the running (all others are just 'shortcuts' to this one).
     * 
     * @param executionString this is the string that will be executed
     * @param workingDir this is the directory where the execution will happen
     * @param project this is the project that is related to the run (it is used to get the environment for the shell we are going to
     * execute with the correct pythonpath environment variable).
     * @param monitor this is the monitor used to communicate the progress to the user
     * 
     * @return the string that is the output of the process (stdout).
     */
    public String runAndGetOutput(String executionString, File workingDir, IProject project, IProgressMonitor monitor) {
        monitor.setTaskName("Executing: "+executionString);
        monitor.worked(5);
        Process process = null;
        try {
            monitor.setTaskName("Making pythonpath environment...");
            String[] envp = getEnvironment(project);
            monitor.setTaskName("Making exec.");
            process = Runtime.getRuntime().exec(executionString, envp, workingDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (process != null) {

            try {
                process.getOutputStream().close(); //we won't write to it...
            } catch (IOException e2) {
            }

            monitor.setTaskName("Reading output...");
            monitor.worked(5);
            ThreadStreamReader std = new ThreadStreamReader(process.getInputStream());
            ThreadStreamReader err = new ThreadStreamReader(process.getErrorStream());

            std.start();
            err.start();
            
            
            try {
                monitor.setTaskName("Waiting for process to finish.");
                monitor.worked(5);
                process.waitFor(); //wait until the process completion.
            } catch (InterruptedException e1) {
                throw new RuntimeException(e1);
            }

            return std.contents.toString();
            
        } else {
            try {
                throw new CoreException(PydevPlugin.makeStatus(IStatus.ERROR, "Error creating python process - got null process("
                        + executionString + ")", new Exception("Error creating python process - got null process.")));
            } catch (CoreException e) {
                PydevPlugin.log(IStatus.ERROR, e.getMessage(), e);
            }

        }
        return ""; //no output
    }

    @Override
    public String runAndGetOutput(String script, String args, File workingDir, IProject project) {
        //"C:\Program Files\Java\jdk1.5.0_04\bin\java.exe" "-Dpython.home=C:\bin\jython21" 
        //-classpath "C:\bin\jython21\jython.jar;%CLASSPATH%" org.python.util.jython %ARGS%

        String javaLoc = TestDependent.JAVA_LOCATION;
        javaLoc = formatParamToExec(javaLoc);
        
        IInterpreterManager interpreterManager = PydevPlugin.getInterpreterManager();
        String jythonJar = interpreterManager.getDefaultJythonJar();
        
        String jythonHome = interpreterManager.getDefaultJythonHome();
        String jythonPath = interpreterManager.getDefaultJythonPath();
        
        String executionString = javaLoc +
        " -Dpython.home="+jythonHome+ 
        " -Dpython.path="+ jythonPath+ 
        " -classpath "+jythonJar+
        " org.python.util.jython "+script;
        
        System.out.println("Executing "+executionString);
        return runAndGetOutput(executionString, workingDir, project);
    }

}

