package com.ruoyi.web.interceptor.cost;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.cost.vo.CostOpenAppSession;
import com.ruoyi.system.service.cost.ICostOpenTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.LinkedHashMap;

/**
 * 开放接口 Bearer Token 鉴权拦截器
 */
@Component
public class CostOpenTokenAuthInterceptor implements HandlerInterceptor {
    @Autowired
    private ICostOpenTokenService openTokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader(CostOpenApiConstants.OPEN_AUTHORIZATION_HEADER);
        if (StringUtils.isBlank(authorization) || !StringUtils.startsWithIgnoreCase(authorization, CostOpenApiConstants.OPEN_TOKEN_PREFIX)) {
            renderAuthError(response, HttpStatus.UNAUTHORIZED,
                    "缺少开放接口访问令牌，请先调用 /cost/open/auth/token 获取 Bearer Token");
            return false;
        }
        String accessToken = StringUtils.substringAfter(authorization, CostOpenApiConstants.OPEN_TOKEN_PREFIX).trim();
        try {
            CostOpenAppSession session = openTokenService.getSession(accessToken);
            request.setAttribute(CostOpenApiConstants.OPEN_APP_SESSION_ATTR, session);
            return true;
        } catch (Exception ex) {
            renderAuthError(response, HttpStatus.UNAUTHORIZED, ex.getMessage());
            return false;
        }
    }

    private void renderAuthError(HttpServletResponse response, int code, String message) {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("tokenType", "Bearer");
        data.put("tokenApplyPath", "/cost/open/auth/token");
        AjaxResult body = AjaxResult.error(code, message).put("data", data);
        try {
            response.getWriter().write(JSON.toJSONString(body));
        } catch (Exception ignored) {
        }
    }
}
