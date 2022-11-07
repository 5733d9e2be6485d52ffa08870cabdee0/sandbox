package com.redhat.service.smartevents.shard.operator.resources;

import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * From the Kubernetes API Conventions:
 * <p/>
 * <em>
 * Conditions provide a standard mechanism for higher-level status reporting from a controller.
 * They are an extension mechanism which allows tools and other controllers to collect summary information about resources without needing
 * to understand resource-specific status details.
 * </em>
 * <p/>
 * <em>
 * Conditions should complement more detailed information about the observed status of an object written by a controller, rather than replace it.
 * For example, the "Available" condition of a Deployment can be determined by examining readyReplicas, replicas, and other properties of the Deployment.
 * However, the "Available" condition allows other components to avoid duplicating the availability logic in the Deployment controller.
 * </em>
 * <p/>
 * <em>
 * Objects may report multiple conditions, and new types of conditions may be added in the future or by 3rd party controllers. Therefore,
 * conditions are represented using a list/slice of objects, where each condition has a similar structure.
 * This collection should be treated as a map with a key of type.
 * </em>
 *
 * @see <a href="https://github.com/kubernetes/community/blob/master/contributors/devel/sig-architecture/api-conventions.md#typical-status-properties">Kubernetes API Conventions - Typical Status
 *      Properties</a>
 */
public class Condition {

    /**
     * The time zone using UTC is sometimes denoted UTC±00:00 or
     * by the letter Z—a reference to the equivalent nautical time zone (GMT), which has been denoted by a Z since about 1950.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Coordinated_Universal_Time">Coordinated Universal Time</a>
     */
    private static final String KUBERNETES_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private final String type;
    private ConditionStatus status;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = KUBERNETES_DATE_FORMAT, timezone = "UTC")
    private Date lastTransitionTime;
    private String reason;
    private String message;
    private String errorCode;

    @JsonCreator
    public Condition(@JsonProperty("type") final String type, @JsonProperty("status") final ConditionStatus status) {
        this.type = type;
        this.status = status;
    }

    public Condition(final String type) {
        this(type, ConditionStatus.UNKNOWN);
    }

    public String getType() {
        return type;
    }

    public ConditionStatus getStatus() {
        return status;
    }

    public void setStatus(ConditionStatus status) {
        this.status = status;
    }

    public Date getLastTransitionTime() {
        return lastTransitionTime;
    }

    public void setLastTransitionTime(Date lastTransitionTime) {
        this.lastTransitionTime = lastTransitionTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    // type is our hash key for this object.
    // in a Set we will identify its uniqueness by this hash
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Condition condition = (Condition) o;
        return type == condition.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }
}
