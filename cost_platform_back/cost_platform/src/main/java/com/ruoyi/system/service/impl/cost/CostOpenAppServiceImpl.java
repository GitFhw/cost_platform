package com.ruoyi.system.service.impl.cost;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.cost.CostOpenApp;
import com.ruoyi.system.domain.cost.vo.CostOpenAppSession;
import com.ruoyi.system.mapper.cost.CostOpenAppMapper;
import com.ruoyi.system.service.cost.ICostOpenAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.ruoyi.system.service.cost.constant.CostDomainConstants.STATUS_ENABLED;

/**
 * 开放应用服务实现
 */
@Service
public class CostOpenAppServiceImpl implements ICostOpenAppService {
    public static final String SCENE_SCOPE_ALL = "ALL";
    public static final String SCENE_SCOPE_LIST = "LIST";

    @Autowired
    private CostOpenAppMapper openAppMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public CostOpenApp authenticateApp(String appCode, String appSecret) {
        if (StringUtils.isBlank(appCode) || StringUtils.isBlank(appSecret)) {
            throw new ServiceException("开放应用编码和密钥不能为空", HttpStatus.UNAUTHORIZED);
        }
        CostOpenApp app = openAppMapper.selectOne(Wrappers.<CostOpenApp>lambdaQuery()
                .eq(CostOpenApp::getAppCode, StringUtils.trim(appCode)));
        if (app == null) {
            throw new ServiceException("开放应用身份无效，请检查 appCode 或 appSecret", HttpStatus.UNAUTHORIZED);
        }
        validateAppStatus(app);
        String requestHash = sha256Hex(StringUtils.trim(appSecret));
        if (!MessageDigest.isEqual(
                StringUtils.defaultString(requestHash).getBytes(StandardCharsets.UTF_8),
                StringUtils.defaultString(app.getAppSecretHash()).getBytes(StandardCharsets.UTF_8))) {
            throw new ServiceException("开放应用身份无效，请检查 appCode 或 appSecret", HttpStatus.UNAUTHORIZED);
        }
        return app;
    }

    @Override
    public CostOpenAppSession buildSession(CostOpenApp app) {
        CostOpenAppSession session = new CostOpenAppSession();
        session.setAppId(app.getAppId());
        session.setAppCode(app.getAppCode());
        session.setAppName(app.getAppName());
        session.setSceneScopeType(normalizeSceneScopeType(app.getSceneScopeType()));
        session.setAuthorizedSceneIds(parseSceneIds(app.getSceneIdsJson()));
        session.setAllowDraftSnapshot(Boolean.TRUE.equals(app.getAllowDraftSnapshot()));
        session.setTokenTtlSeconds(resolveTokenTtlSeconds(app.getTokenTtlSeconds()));
        return session;
    }

    @Override
    public boolean canAccessScene(CostOpenAppSession session, Long sceneId) {
        if (session == null || sceneId == null) {
            return false;
        }
        if (SCENE_SCOPE_ALL.equalsIgnoreCase(normalizeSceneScopeType(session.getSceneScopeType()))) {
            return true;
        }
        return resolveAuthorizedSceneIds(session).contains(sceneId);
    }

    @Override
    public void assertCanAccessScene(CostOpenAppSession session, Long sceneId) {
        if (!canAccessScene(session, sceneId)) {
            throw new ServiceException("当前开放应用未被授权访问该场景，请联系平台管理员开通场景权限", HttpStatus.FORBIDDEN);
        }
    }

    @Override
    public boolean allowDraftSnapshot(CostOpenAppSession session) {
        return session != null && Boolean.TRUE.equals(session.getAllowDraftSnapshot());
    }

    @Override
    public Set<Long> resolveAuthorizedSceneIds(CostOpenAppSession session) {
        if (session == null || session.getAuthorizedSceneIds() == null) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(session.getAuthorizedSceneIds());
    }

    private void validateAppStatus(CostOpenApp app) {
        if (!STATUS_ENABLED.equals(app.getStatus())) {
            throw new ServiceException("开放应用已停用，请联系平台管理员确认", HttpStatus.FORBIDDEN);
        }
        Date now = new Date();
        if (app.getEffectiveStartTime() != null && now.before(app.getEffectiveStartTime())) {
            throw new ServiceException("开放应用尚未生效，请在生效时间后重新申请访问令牌", HttpStatus.FORBIDDEN);
        }
        if (app.getEffectiveEndTime() != null && now.after(app.getEffectiveEndTime())) {
            throw new ServiceException("开放应用已过期，请联系平台管理员续期后重新申请访问令牌", HttpStatus.FORBIDDEN);
        }
    }

    private String normalizeSceneScopeType(String sceneScopeType) {
        return SCENE_SCOPE_LIST.equalsIgnoreCase(sceneScopeType) ? SCENE_SCOPE_LIST : SCENE_SCOPE_ALL;
    }

    private Integer resolveTokenTtlSeconds(Integer tokenTtlSeconds) {
        if (tokenTtlSeconds == null || tokenTtlSeconds <= 0) {
            return 7200;
        }
        return Math.min(tokenTtlSeconds, 86400);
    }

    private List<Long> parseSceneIds(String sceneIdsJson) {
        if (StringUtils.isBlank(sceneIdsJson)) {
            return Collections.emptyList();
        }
        try {
            List<Long> sceneIds = objectMapper.readValue(sceneIdsJson, new TypeReference<List<Long>>() {
            });
            return sceneIds == null ? Collections.emptyList() : sceneIds.stream()
                    .filter(id -> id != null && id > 0)
                    .distinct()
                    .toList();
        } catch (Exception e) {
            throw new ServiceException("开放应用场景权限配置格式不正确，请联系平台管理员修复", HttpStatus.FORBIDDEN);
        }
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(StringUtils.defaultString(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceException("系统不支持 SHA-256 摘要算法");
        }
    }
}
