package com.redhat.service.smartevents.integration.tests.cucumber;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.cucumber.plugin.EventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.HookTestStep;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestStepFinished;
import io.cucumber.plugin.event.WriteEvent;

public class TestCaseFileLogger implements EventListener {

    private SafeOutputStreamWriter mainLogWriter;
    private Map<UUID, SafeOutputStreamWriter> testCaseWriters = new ConcurrentHashMap<UUID, SafeOutputStreamWriter>();

    private static final File LOG_FOLDER = new File("target/cucumber-logs");

    public TestCaseFileLogger(OutputStream out) {
        LOG_FOLDER.mkdirs();
        mainLogWriter = new SafeOutputStreamWriter(out);
    }

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseStarted.class, this::handleTestCaseStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::handleTestStepFinished);
        publisher.registerHandlerFor(TestCaseFinished.class, this::handleTestCaseFinished);
        publisher.registerHandlerFor(WriteEvent.class, this::handleWrite);
    }

    /**
     * Prepare test case logging by creating a new log file and storing file log writer for later usage.
     * Print test case start note on main log.
     *
     * @param event
     */
    private void handleTestCaseStarted(TestCaseStarted event) {
        String testCaseFileName = event.getTestCase().getName().replaceAll(" ", "_").concat(".log");
        try {
            File file = new File(LOG_FOLDER, testCaseFileName);
            file.createNewFile();
            testCaseWriters.put(event.getTestCase().getId(), new SafeOutputStreamWriter(new FileOutputStream(file)));
        } catch (IOException e) {
            throw new RuntimeException("error preparing log file", e);
        }
        mainLogWriter.writeln(String.format("Test case '%s' started", event.getTestCase().getName()));
    }

    /**
     * Store test step result in log file.
     *
     * @param event
     */
    private void handleTestStepFinished(TestStepFinished event) {
        SafeOutputStreamWriter writer = testCaseWriters.get(event.getTestCase().getId());

        if (event.getTestStep() instanceof PickleStepTestStep) {
            PickleStepTestStep testStep = (PickleStepTestStep) event.getTestStep();
            String keyword = testStep.getStep().getKeyword();
            String stepText = testStep.getStep().getText();
            switch (event.getResult().getStatus()) {
                case PASSED:
                    writer.writeln(String.format("%s%s", keyword, stepText));
                    break;
                case FAILED:
                    writer.writeln(String.format("%s%s  FAILED", keyword, stepText));
                    event.getResult().getError().printStackTrace(writer.getPrintWriter());
                    writer.flush();
                    break;
                case UNDEFINED:
                    writer.writeln(String.format("'%s' step undefined!", stepText));
                    break;
                default:
                    break;
            }
        } else if (event.getTestStep() instanceof HookTestStep) {
            switch (event.getResult().getStatus()) {
                case FAILED:
                    event.getResult().getError().printStackTrace(writer.getPrintWriter());
                    writer.flush();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Print test case finish note on main log.
     *
     * @param event
     */
    private void handleTestCaseFinished(TestCaseFinished event) {
        mainLogWriter.writeln(String.format("Test case '%s' finished with result '%s' in %s", event.getTestCase().getName(), event.getResult().getStatus(), event.getResult().getDuration()));

        testCaseWriters.get(event.getTestCase().getId()).close();
    }

    /**
     * Handle direct log writes used in test step implementations.
     *
     * @param event
     */
    private void handleWrite(WriteEvent event) {
        testCaseWriters.get(event.getTestCase().getId()).writeln(String.format("# %s", event.getText()));
    }

    /**
     * OutputStreamWriter wrapping IOException into runtime exceptions.
     */
    private class SafeOutputStreamWriter {
        private OutputStreamWriter writer;

        public SafeOutputStreamWriter(OutputStream out) {
            writer = new OutputStreamWriter(out);
        }

        public synchronized void writeln(String content) {
            write(content + "\n");
        }

        public synchronized void write(String content) {
            try {
                writer.write(content);
                writer.flush();
            } catch (IOException e) {
                throw new RuntimeException("failed to write to output stream", e);
            }
        }

        public synchronized void flush() {
            try {
                writer.flush();
            } catch (IOException e) {
                throw new RuntimeException("failed to write to output stream", e);
            }
        }

        public synchronized void close() {
            try {
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to close output stream", e);

            }
        }

        public PrintWriter getPrintWriter() {
            return new PrintWriter(writer);
        }
    }
}
