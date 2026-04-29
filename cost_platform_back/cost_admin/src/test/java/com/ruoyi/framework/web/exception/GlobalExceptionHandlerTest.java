package com.ruoyi.framework.web.exception;

import com.ruoyi.common.core.domain.AjaxResult;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockHttpServletRequest;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    @Test
    void shouldTranslateDuplicateKeyToBusinessMessage() {
        DuplicateKeyException exception = new DuplicateKeyException("duplicate",
                new SQLIntegrityConstraintViolationException("Duplicate entry 'FEE001' for key 'uk_cost_fee_code'"));

        String message = GlobalExceptionHandler.resolveDataIntegrityMessage(exception);

        assertThat(message).contains("已存在");
        assertThat(message).doesNotContain("Duplicate entry");
        assertThat(message).doesNotContain("uk_cost_fee_code");
    }

    @Test
    void shouldTranslateForeignKeyViolationToBusinessMessage() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException("fk",
                new SQLIntegrityConstraintViolationException("Cannot delete or update a parent row: a foreign key constraint fails"));

        String message = GlobalExceptionHandler.resolveDataIntegrityMessage(exception);

        assertThat(message).contains("被其他业务记录引用");
        assertThat(message).doesNotContain("foreign key");
    }

    @Test
    void shouldTranslateRequiredColumnViolationToBusinessMessage() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException("not null",
                new SQLIntegrityConstraintViolationException("Column 'scene_code' cannot be null"));

        String message = GlobalExceptionHandler.resolveDataIntegrityMessage(exception);

        assertThat(message).contains("必填字段为空");
        assertThat(message).doesNotContain("scene_code");
    }

    @Test
    void shouldHideRuntimeExceptionDetailAndReturnTraceId() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/cost/mock");

        AjaxResult result = handler.handleRuntimeException(new RuntimeException("raw sql stack detail"), request);

        assertThat(result.get("errorType")).isEqualTo("SYSTEM");
        assertThat(String.valueOf(result.get("traceId"))).startsWith("ERR-");
        assertThat(String.valueOf(result.get("msg"))).contains("追踪编号");
        assertThat(String.valueOf(result.get("msg"))).doesNotContain("raw sql stack detail");
    }

    @Test
    void shouldBuildTraceIdWithStablePrefix() {
        assertThat(GlobalExceptionHandler.buildTraceId()).startsWith("ERR-");
    }
}
