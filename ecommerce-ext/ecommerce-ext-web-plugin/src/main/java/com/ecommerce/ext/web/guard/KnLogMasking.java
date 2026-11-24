package com.ecommerce.ext.web.guard;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import com.tvd12.ezyfox.bean.annotation.EzyPostInit;
import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import java.util.Iterator;
import java.util.regex.Pattern;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * The platform's request log writes the raw query string of every request, so the secret of a
 * guest order link ({@code key=}) or a shop-confirm link ({@code token=}) would land in the log
 * file, the console and every plugin log listener. At startup the request logger stops handing
 * its events straight to the root appenders and hands them to {@link MaskingAppender}, which
 * replaces those values with {@code ***} and then passes the event to the same root appenders.
 * Events without a secret pass through unchanged.
 */
@EzySingleton
public class KnLogMasking {

    static final String REQUEST_LOGGER =
        "org.youngmonkeys.ezyplatform.web.interceptor.WebLogInterceptor";
    static final String APPENDER_NAME = "kn-secret-masking";
    static final String MASK = "***";

    private static final Pattern SECRET = Pattern.compile(
        "(?i)(^|[?&;,\\s])(key|token|accesstoken|access_token)=[^&\\s]+"
    );

    @EzyPostInit
    public void install() {
        ILoggerFactory factory = LoggerFactory.getILoggerFactory();
        if (factory instanceof LoggerContext) {
            install((LoggerContext) factory);
        }
    }

    static void install(LoggerContext context) {
        Logger target = context.getLogger(REQUEST_LOGGER);
        if (target.getAppender(APPENDER_NAME) != null) {
            return;
        }
        MaskingAppender appender = new MaskingAppender(
            context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
        );
        appender.setContext(context);
        appender.setName(APPENDER_NAME);
        appender.start();
        target.addAppender(appender);
        target.setAdditive(false);
    }

    static String mask(String message) {
        if (message == null || message.indexOf('=') < 0) {
            return message;
        }
        return SECRET.matcher(message).replaceAll("$1$2=" + MASK);
    }

    static final class MaskingAppender extends AppenderBase<ILoggingEvent> {

        private final Logger root;

        MaskingAppender(Logger root) {
            this.root = root;
        }

        @Override
        protected void append(ILoggingEvent event) {
            ILoggingEvent output = masked(event);
            Iterator<Appender<ILoggingEvent>> it = root.iteratorForAppenders();
            while (it.hasNext()) {
                it.next().doAppend(output);
            }
        }

        private ILoggingEvent masked(ILoggingEvent event) {
            String message = event.getFormattedMessage();
            String masked = mask(message);
            if (masked == null || masked.equals(message)) {
                return event;
            }
            LoggingEvent copy = new LoggingEvent(
                Logger.class.getName(),
                root.getLoggerContext().getLogger(event.getLoggerName()),
                event.getLevel(),
                masked,
                null,
                null
            );
            copy.setThreadName(event.getThreadName());
            copy.setTimeStamp(event.getTimeStamp());
            return copy;
        }
    }
}
