package com.ruoyi.framework.config;

import java.util.Date;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * MyBatis-Plus 公共字段自动填充。
 *
 * <p>统一处理核算模块基础审计字段，避免每个 Service 重复维护时间戳逻辑。</p>
 */
@Component
public class MyBatisPlusMetaObjectHandler implements MetaObjectHandler
{
    @Override
    public void insertFill(MetaObject metaObject)
    {
        Date now = DateUtils.getNowDate();
        String username = resolveUsername();
        strictInsertFill(metaObject, "createTime", Date.class, now);
        strictInsertFill(metaObject, "updateTime", Date.class, now);
        if (StringUtils.isNotEmpty(username))
        {
            strictInsertFill(metaObject, "createBy", String.class, username);
            strictInsertFill(metaObject, "updateBy", String.class, username);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject)
    {
        strictUpdateFill(metaObject, "updateTime", Date.class, DateUtils.getNowDate());
        String username = resolveUsername();
        if (StringUtils.isNotEmpty(username))
        {
            strictUpdateFill(metaObject, "updateBy", String.class, username);
        }
    }

    /**
     * 安全获取当前登录人。
     */
    private String resolveUsername()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception ignored)
        {
            return null;
        }
    }
}
