package com.ruoyi.system.config.cost;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cost dispatch runtime properties.
 */
@Component
@ConfigurationProperties(prefix = "cost.dispatch")
public class CostDispatchProperties
{
    /**
     * Dispatch coordinator scan interval in seconds.
     */
    private Long dispatchIntervalSeconds = 15L;

    /**
     * Running task stale timeout in seconds.
     */
    private Long staleTimeoutSeconds = 600L;

    /**
     * Explicit node id for task dispatch and partition ownership.
     */
    private String nodeId;

    public Long getDispatchIntervalSeconds()
    {
        return dispatchIntervalSeconds;
    }

    public void setDispatchIntervalSeconds(Long dispatchIntervalSeconds)
    {
        this.dispatchIntervalSeconds = dispatchIntervalSeconds;
    }

    public Long getStaleTimeoutSeconds()
    {
        return staleTimeoutSeconds;
    }

    public void setStaleTimeoutSeconds(Long staleTimeoutSeconds)
    {
        this.staleTimeoutSeconds = staleTimeoutSeconds;
    }

    public String getNodeId()
    {
        return nodeId;
    }

    public void setNodeId(String nodeId)
    {
        this.nodeId = nodeId;
    }
}
