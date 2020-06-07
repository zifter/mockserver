package org.mockserver.matchers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.mockserver.log.model.LogEntry;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.model.NottableString;

import java.util.regex.PatternSyntaxException;

import static org.mockserver.model.NottableString.string;
import static org.slf4j.event.Level.DEBUG;

/**
 * @author jamesdbloom
 */
public class RegexStringMatcher extends BodyMatcher<NottableString> {

    private static final String[] EXCLUDED_FIELDS = {"mockServerLogger"};
    private final MockServerLogger mockServerLogger;
    private final NottableString matcher;
    private final boolean controlPlaneMatcher;

    public RegexStringMatcher(MockServerLogger mockServerLogger, boolean controlPlaneMatcher) {
        this.mockServerLogger = mockServerLogger;
        this.controlPlaneMatcher = controlPlaneMatcher;
        this.matcher = null;
    }

    RegexStringMatcher(MockServerLogger mockServerLogger, NottableString matcher, boolean controlPlaneMatcher) {
        this.mockServerLogger = mockServerLogger;
        this.controlPlaneMatcher = controlPlaneMatcher;
        this.matcher = matcher;
    }

    public boolean matches(String matched) {
        return matches(null, string(matched));
    }

    public boolean matches(final MatchDifference context, NottableString matched) {
        boolean result = false;

        if (matcher == null || matches(matcher.getValue(), matched.getValue(), true)) {
            result = true;
        }

        if (!result && context != null) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(DEBUG)
                    .setMatchDifference(context)
                    .setMessageFormat("string or regex match failed expected:{}found:{}")
                    .setArguments(this.matcher, matched)
            );
        }

        return matched.isNot() == (matcher.isNot() == (not != result));
    }

    public boolean matches(NottableString matcher, NottableString matched, boolean ignoreCase) {
        if (matcher.isNot() && matched.isNot()) {
            // mutual notted control plane match
            return matches(matcher.getValue(), matched.getValue(), ignoreCase);
        } else {
            // data plane & control plan match
            return (matcher.isNot() || matched.isNot()) ^ matches(matcher.getValue(), matched.getValue(), ignoreCase);
        }
    }

    public boolean matches(String matcher, String matched, boolean ignoreCase) {
        if (StringUtils.isBlank(matcher)) {
            return true;
        } else if (matched != null) {
            // match as exact string
            if (matched.equals(matcher)) {
                return true;
            }

            // match as regex - matcher -> matched (data plane or control plane)
            try {
                if (matched.matches(matcher)) {
                    return true;
                }
            } catch (PatternSyntaxException pse) {
                if (MockServerLogger.isEnabled(DEBUG)) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(DEBUG)
                            .setMessageFormat("error while matching regex [" + matcher + "] for string [" + matched + "] " + pse.getMessage())
                            .setThrowable(pse)
                    );
                }
            }
            // match as regex - matched -> matcher (control plane only)
            try {
                if (controlPlaneMatcher && matcher.matches(matched)) {
                    return true;
                }
            } catch (PatternSyntaxException pse) {
                if (MockServerLogger.isEnabled(DEBUG)) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(DEBUG)
                            .setMessageFormat("error while matching regex [" + matched + "] for string [" + matcher + "] " + pse.getMessage())
                            .setThrowable(pse)
                    );
                }
            }
            // case insensitive comparison is mainly to improve matching in web containers like Tomcat that convert header names to lower case
            if (ignoreCase) {
                // match as exact string lower-case
                if (matched.equalsIgnoreCase(matcher)) {
                    return true;
                }
                // match as regex - matcher -> matched (data plane or control plane)
                try {
                    if (matched.toLowerCase().matches(matcher.toLowerCase())) {
                        return true;
                    }
                } catch (PatternSyntaxException pse) {
                    if (MockServerLogger.isEnabled(DEBUG)) {
                        mockServerLogger.logEvent(
                            new LogEntry()
                                .setLogLevel(DEBUG)
                                .setMessageFormat("error while matching regex [" + matcher.toLowerCase() + "] for string [" + matched.toLowerCase() + "] " + pse.getMessage())
                                .setThrowable(pse)
                        );
                    }
                }
                // match as regex - matched -> matcher (control plane only)
                try {
                    if (controlPlaneMatcher && matcher.toLowerCase().matches(matched.toLowerCase())) {
                        return true;
                    }
                } catch (PatternSyntaxException pse) {
                    if (MockServerLogger.isEnabled(DEBUG)) {
                        mockServerLogger.logEvent(
                            new LogEntry()
                                .setLogLevel(DEBUG)
                                .setMessageFormat("error while matching regex [" + matched.toLowerCase() + "] for string [" + matcher.toLowerCase() + "] " + pse.getMessage())
                                .setThrowable(pse)
                        );
                    }
                }
            }
        }

        return false;
    }

    public boolean isBlank() {
        return matcher == null || StringUtils.isBlank(matcher.getValue());
    }

    @Override
    @JsonIgnore
    protected String[] fieldsExcludedFromEqualsAndHashCode() {
        return EXCLUDED_FIELDS;
    }
}
