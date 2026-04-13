package com.valadir.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.valadir.common.mdc.MdcKeys;

import java.util.Map;

public class MdcContextConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {

        Map<String, String> mdc = event.getMDCPropertyMap();
        String requestId = mdc.get(MdcKeys.REQUEST_ID);

        if (requestId == null || requestId.isBlank()) {
            return "";
        }

        return "[requestId=%s method=%s path=%s accountId=%s] ".formatted(
            requestId,
            mdc.getOrDefault(MdcKeys.METHOD, MdcKeys.UNKNOWN),
            mdc.getOrDefault(MdcKeys.PATH, MdcKeys.UNKNOWN),
            mdc.getOrDefault(MdcKeys.ACCOUNT_ID, MdcKeys.UNKNOWN)
        );
    }
}
