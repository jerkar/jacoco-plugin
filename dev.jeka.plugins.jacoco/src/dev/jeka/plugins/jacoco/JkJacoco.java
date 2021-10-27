package dev.jeka.plugins.jacoco;

import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkInternalClassloader;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.testing.JkTestProcessor;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides convenient methods to deal with Jacoco agent and report tool.
 *
 * Note : May sometime fall in this issue when running from IDE :
 * https://stackoverflow.com/questions/31720139/jacoco-code-coverage-report-generator-showing-error-classes-in-bundle-code-c
 *
 * See command-line documentation :
 * https://www.jacoco.org/jacoco/trunk/doc/cli.html
 * https://www.jacoco.org/jacoco/trunk/doc/agent.html
 *
 */
public final class JkJacoco {

    private Path agent;

    private Path execFile;

    private Path classDir;

    private JkPathMatcher classDirFilter;

    private final List<String> agentOptions = new LinkedList<>();

    private final List<String> reportOptions = new LinkedList<>();

    private JkJacoco(Path agent, Path destFile) {
        super();
        this.agent = agent;
        this.execFile = destFile;
    }

    public static JkJacoco of(Path destFile) {
        final URL agentJarUrl = JkPluginJacoco.class.getResource("org.jacoco.agent-0.8.7-runtime.jar");
        final Path agentJarFile = JkUtilsIO.copyUrlContentToCacheFile(agentJarUrl, System.out, JkInternalClassloader.URL_CACHE_DIR);
        return new JkJacoco(agentJarFile, destFile);
    }

    public JkJacoco setAgent(Path jacocoagent) {
        this.agent = jacocoagent;
        return this;
    }

    public JkJacoco addAgentOptions(String ...args) {
        agentOptions.addAll(Arrays.asList(args));
        return this;
    }

    /**
     * Necessary to produce XML report
     */
    public JkJacoco setClassDir(Path classDir) {
        this.classDir = classDir;
        return this;
    }

    public JkJacoco setClassDirFilter(JkPathMatcher pathMatcher) {
        this.classDirFilter = pathMatcher;
        return this;
    }

    /**
     * See https://www.jacoco.org/jacoco/trunk/doc/cli.html for report option
     */
    public JkJacoco addReportOptions(String ...args) {
        reportOptions.addAll(Arrays.asList(args));
        return this;
    }

    public List<String> getReportOptions() {
        return reportOptions;
    }

    public void configure(JkTestProcessor testProcessor) {
        testProcessor.getPreActions().append(() -> {
            String agentOptions = agentOptions();
            JkJavaProcess process = JkUtilsObject.firstNonNull(testProcessor.getForkingProcess(),
                    JkJavaProcess.ofJava(JkTestProcessor.class.getName()));
            process.addAgent(agent, agentOptions);
            JkLog.info("Instrumenting tests with Jacoco agent " + agentOptions);
            testProcessor.setForkingProcess(process);
            testProcessor.getPostActions().append(new Reporter());
        });

    }

    private String agentOptions() {
        String result = String.join(",", agentOptions);
        boolean hasDestFile = agentOptions.stream()
                .filter(option -> option.startsWith("destfile="))
                .findFirst().isPresent();
        if (!hasDestFile) {
            if (!JkUtilsString.isBlank(result)) {
                result = result + ",";
            }
            result = result + "destfile=" + JkUtilsPath.relativizeFromWorkingDir(execFile);
        }
        return result;
    }

    private class Reporter implements Runnable {

        @Override
        public void run() {
            JkLog.info("Jacoco internal report created at " + execFile.toAbsolutePath());
            if (!reportOptions.isEmpty()) {
                if (classDir == null) {
                    JkLog.warn("No class dir specified. Cannot run jacoco report.");
                    return;
                }
                if (!Files.exists(execFile)) {
                    JkLog.warn("File " + execFile + " not found. Cannot run jacoco report.");
                    return;
                }
                JkPathTree pathTree = null;
                if (classDirFilter != null) {
                    pathTree = JkPathTree.of(classDir).withMatcher(classDirFilter);
                }
                final URL cliJarUrl = JkPluginJacoco.class.getResource("org.jacoco.cli-0.8.7-nodeps.jar");
                final Path cliJarFile = JkUtilsIO.copyUrlContentToCacheFile(cliJarUrl, System.out,
                        JkInternalClassloader.URL_CACHE_DIR);
                List<String> args = new LinkedList<>();
                args.add("report");
                args.add(execFile.toString());
                if (classDirFilter == null) {
                    args.add("--classfiles");
                    args.add(classDir.toString());
                } else {
                    pathTree.getFiles().forEach(file ->  {
                        args.add("--classfiles");
                        args.add(file.toString());
                    });
                }
                args.add("--encoding");
                args.add("utf-8");
                args.addAll(reportOptions);
                if (!JkLog.isVerbose()) {
                    args.add("--quiet");
                }
                JkLog.info("Generate Jacoco report");
                JkJavaProcess.ofJavaJar(cliJarFile, null)
                        .setFailOnError(true)
                        .setLogCommand(JkLog.isVerbose())
                        .addParams(args)
                        .exec();
            }
        }
    }

}
