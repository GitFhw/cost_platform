package com.ruoyi.system.service.cost;

import com.ruoyi.system.domain.cost.CostOpenApp;
import com.ruoyi.system.domain.cost.vo.CostOpenAppSession;

import java.util.Set;

/**
 * 开放应用服务接口
 */
public interface ICostOpenAppService {
    CostOpenApp authenticateApp(String appCode, String appSecret);

    CostOpenAppSession buildSession(CostOpenApp app);

    boolean canAccessScene(CostOpenAppSession session, Long sceneId);

    void assertCanAccessScene(CostOpenAppSession session, Long sceneId);

    boolean allowDraftSnapshot(CostOpenAppSession session);

    Set<Long> resolveAuthorizedSceneIds(CostOpenAppSession session);
}
