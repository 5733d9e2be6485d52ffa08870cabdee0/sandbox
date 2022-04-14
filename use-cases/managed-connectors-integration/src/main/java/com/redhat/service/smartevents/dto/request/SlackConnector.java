
package com.redhat.service.smartevents.dto.request;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Slack Sink
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "channel",
    "webhookUrl",
    "iconEmoji",
    "iconUrl",
    "username"
})
@Generated("jsonschema2pojo")
public class SlackConnector {

    /**
     * Channel
     * <p>
     * The Slack channel to send messages to.
     * (Required)
     * 
     */
    @JsonProperty("channel")
    @JsonPropertyDescription("The Slack channel to send messages to.")
    private String channel;
    /**
     * Webhook URL
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("webhookUrl")
    private Object webhookUrl;
    /**
     * Icon Emoji
     * <p>
     * Use a Slack emoji as an avatar.
     * 
     */
    @JsonProperty("iconEmoji")
    @JsonPropertyDescription("Use a Slack emoji as an avatar.")
    private String iconEmoji;
    /**
     * Icon URL
     * <p>
     * The avatar that the component will use when sending message to a channel or user.
     * 
     */
    @JsonProperty("iconUrl")
    @JsonPropertyDescription("The avatar that the component will use when sending message to a channel or user.")
    private String iconUrl;
    /**
     * Username
     * <p>
     * This is the username that the bot will have when sending messages to a channel or user.
     * 
     */
    @JsonProperty("username")
    @JsonPropertyDescription("This is the username that the bot will have when sending messages to a channel or user.")
    private String username;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * Channel
     * <p>
     * The Slack channel to send messages to.
     * (Required)
     * 
     */
    @JsonProperty("channel")
    public String getChannel() {
        return channel;
    }

    /**
     * Channel
     * <p>
     * The Slack channel to send messages to.
     * (Required)
     * 
     */
    @JsonProperty("channel")
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * Webhook URL
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("webhookUrl")
    public Object getWebhookUrl() {
        return webhookUrl;
    }

    /**
     * Webhook URL
     * <p>
     * 
     * (Required)
     * 
     */
    @JsonProperty("webhookUrl")
    public void setWebhookUrl(Object webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    /**
     * Icon Emoji
     * <p>
     * Use a Slack emoji as an avatar.
     * 
     */
    @JsonProperty("iconEmoji")
    public String getIconEmoji() {
        return iconEmoji;
    }

    /**
     * Icon Emoji
     * <p>
     * Use a Slack emoji as an avatar.
     * 
     */
    @JsonProperty("iconEmoji")
    public void setIconEmoji(String iconEmoji) {
        this.iconEmoji = iconEmoji;
    }

    /**
     * Icon URL
     * <p>
     * The avatar that the component will use when sending message to a channel or user.
     * 
     */
    @JsonProperty("iconUrl")
    public String getIconUrl() {
        return iconUrl;
    }

    /**
     * Icon URL
     * <p>
     * The avatar that the component will use when sending message to a channel or user.
     * 
     */
    @JsonProperty("iconUrl")
    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    /**
     * Username
     * <p>
     * This is the username that the bot will have when sending messages to a channel or user.
     * 
     */
    @JsonProperty("username")
    public String getUsername() {
        return username;
    }

    /**
     * Username
     * <p>
     * This is the username that the bot will have when sending messages to a channel or user.
     * 
     */
    @JsonProperty("username")
    public void setUsername(String username) {
        this.username = username;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
