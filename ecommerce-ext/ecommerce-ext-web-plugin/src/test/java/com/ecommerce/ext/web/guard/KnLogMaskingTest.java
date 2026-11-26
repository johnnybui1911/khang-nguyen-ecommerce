package com.ecommerce.ext.web.guard;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.testng.annotations.Test;

public class KnLogMaskingTest {

    @Test
    public void masksOrderKeyAndTokensOnly() {
        assertEquals(
            KnLogMasking.mask("uri: /api/v1/ecm/orders/96, method: GET, parameters: key=AbC-12_x"),
            "uri: /api/v1/ecm/orders/96, method: GET, parameters: key=***"
        );
        assertEquals(
            KnLogMasking.mask("parameters: v=3&token=t0k&accessToken=a1&KEY=b2&monkey=ok"),
            "parameters: v=3&token=***&accessToken=***&KEY=***&monkey=ok"
        );
        assertEquals(KnLogMasking.mask("parameters: v=1700000"), "parameters: v=1700000");
        assertEquals(KnLogMasking.mask(null), null);
    }

    @Test
    public void requestLoggerReachesRootAppendersMasked() {
        LoggerContext context = new LoggerContext();
        ListAppender<ILoggingEvent> sink = new ListAppender<>();
        sink.setContext(context);
        sink.start();
        context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).addAppender(sink);

        KnLogMasking.install(context);
        KnLogMasking.install(context);

        Logger requestLogger = context.getLogger(KnLogMasking.REQUEST_LOGGER);
        requestLogger.info(
            "pre handle request uri: {}, method: {}, parameters: {}",
            "/orders/96", "GET", "key=secretValue&v=1"
        );
        requestLogger.info("post handle request uri: {}, method: {}, code: {}", "/", "GET", 200);
        context.getLogger("other").info("parameters: key=untouched");

        assertEquals(sink.list.size(), 3, String.valueOf(context.getStatusManager().getCopyOfStatusList()));
        String first = sink.list.get(0).getFormattedMessage();
        assertEquals(first, "pre handle request uri: /orders/96, method: GET, parameters: key=***&v=1");
        assertFalse(first.contains("secretValue"));
        assertEquals(sink.list.get(0).getLoggerName(), KnLogMasking.REQUEST_LOGGER);
        assertEquals(sink.list.get(1).getFormattedMessage(),
            "post handle request uri: /, method: GET, code: 200");
        assertTrue(sink.list.get(2).getFormattedMessage().endsWith("key=untouched"));
    }
}
