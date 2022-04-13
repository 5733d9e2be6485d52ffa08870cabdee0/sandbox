package com.redhat.service.smartevents.processor.sources.slack;

import com.redhat.service.smartevents.processor.sources.SourceBean;

public interface SlackSource extends SourceBean {

    String TYPE = "Slack";
    String CHANNEL_PARAM = "channel";
    String TOKEN_PARAM = "token";

    @Override
    default String getType() {
        return TYPE;
    }
}
