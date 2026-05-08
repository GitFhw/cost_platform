package com.ruoyi.web.controller.cost;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.cost.CostVariable;
import com.ruoyi.system.domain.vo.CostVariableCopyRequest;
import com.ruoyi.system.domain.vo.CostVariableImportIssueVo;
import com.ruoyi.system.domain.vo.CostVariableImportPreviewVo;
import com.ruoyi.system.domain.vo.CostVariableImportRow;
import com.ruoyi.system.domain.vo.CostVariableTemplateApplyRequest;
import com.ruoyi.system.service.cost.ICostVariableService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 变量中心控制器。
 *
 * @author HwFan
 */
@RestController
@RequestMapping("/cost/variable")
public class CostVariableController extends BaseController {
    @Autowired
    private ICostVariableService variableService;

    /**
     * 查询变量列表。
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:list')")
    @GetMapping("/list")
    public TableDataInfo list(CostVariable variable) {
        startPage();
        List<CostVariable> list = variableService.selectVariableList(variable);
        return getDataTable(list);
    }

    /**
     * 查询变量统计卡片。
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:list')")
    @GetMapping("/stats")
    public AjaxResult stats(CostVariable variable) {
        return success(variableService.selectVariableStats(variable));
    }

    /**
     * 查询变量治理预检查。
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:list')")
    @GetMapping("/governance/{variableId}")
    public AjaxResult governance(@PathVariable Long variableId) {
        return success(variableService.selectVariableGovernanceCheck(variableId));
    }

    /**
     * 查询变量选择框。
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:list')")
    @GetMapping("/optionselect")
    public AjaxResult optionselect(CostVariable variable) {
        return success(variableService.selectVariableOptions(variable));
    }

    /**
     * 导出变量列表。
     */
    @Log(title = "变量中心", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('cost:variable:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, CostVariable variable) {
        List<CostVariable> list = variableService.selectVariableList(variable);
        ExcelUtil<CostVariable> util = new ExcelUtil<>(CostVariable.class);
        util.exportExcel(response, list, "变量中心");
    }

    /**
     * 下载导入模板。
     */
    @PostMapping("/importTemplate")
    @PreAuthorize("@ss.hasPermi('cost:variable:export')")
    public void importTemplate(HttpServletResponse response) {
        ExcelUtil<CostVariableImportRow> util = new ExcelUtil<>(CostVariableImportRow.class);
        util.importTemplateExcel(response, "变量导入模板", "变量导入模板");
    }

    /**
     * 导入预览与校验报告。
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:add')")
    @PostMapping("/importPreview")
    public AjaxResult importPreview(MultipartFile file, boolean updateSupport) throws Exception {
        return success(variableService.previewImport(file, updateSupport));
    }

    /**
     * 瀵煎嚭瀵煎叆澶辫触鎶ュ憡銆?
     */
    @Log(title = "鍙橀噺涓績", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('cost:variable:add')")
    @PostMapping("/importIssueExport")
    public void importIssueExport(HttpServletResponse response, MultipartFile file, boolean updateSupport) throws Exception {
        CostVariableImportPreviewVo preview = variableService.previewImport(file, updateSupport);
        ExcelUtil<CostVariableImportIssueVo> util = new ExcelUtil<>(CostVariableImportIssueVo.class);
        util.exportExcel(response, preview.getIssues(), "变量导入错误报告");
    }

    /**
     * 执行变量导入。
     */
    @Log(title = "变量中心", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('cost:variable:add')")
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        return success(variableService.importVariables(file, updateSupport, getUsername()));
    }

    /**
     * 查询变量详情。
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:query')")
    @GetMapping("/{variableId}")
    public AjaxResult getInfo(@PathVariable Long variableId) {
        return success(variableService.selectVariableById(variableId));
    }

    /**
     * 新增变量。
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:add')")
    @Log(title = "变量中心", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CostVariable variable) {
        if (!variableService.checkVariableCodeUnique(variable)) {
            return error("新增变量'" + variable.getVariableName() + "'失败，同场景下变量编码已存在");
        }
        variable.setCreateBy(getUsername());
        return toAjax(variableService.insertVariable(variable));
    }

    /**
     * 修改变量。
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:edit')")
    @Log(title = "变量中心", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CostVariable variable) {
        if (!variableService.checkVariableCodeUnique(variable)) {
            return error("修改变量'" + variable.getVariableName() + "'失败，同场景下变量编码已存在");
        }
        variable.setUpdateBy(getUsername());
        return toAjax(variableService.updateVariable(variable));
    }

    /**
     * 复制变量。
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:add')")
    @Log(title = "变量中心", businessType = BusinessType.INSERT)
    @PostMapping("/copy")
    public AjaxResult copy(@RequestBody CostVariableCopyRequest request) {
        return success(variableService.copyVariable(request));
    }

    /**
     * 查询共享影响因素模板。
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:list')")
    @GetMapping("/sharedTemplates")
    public AjaxResult sharedTemplates() {
        return success(variableService.selectSharedTemplates());
    }

    /**
     * 应用共享影响因素模板。
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:add')")
    @Log(title = "变量中心", businessType = BusinessType.INSERT)
    @PostMapping("/sharedTemplates/apply")
    public AjaxResult applySharedTemplate(@RequestBody CostVariableTemplateApplyRequest request) {
        return success(variableService.applySharedTemplate(request, getUsername()));
    }

    /**
     * 删除变量。
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:remove')")
    @Log(title = "变量中心", businessType = BusinessType.DELETE)
    @DeleteMapping("/{variableIds}")
    public AjaxResult remove(@PathVariable Long[] variableIds) {
        return toAjax(variableService.deleteVariableByIds(variableIds));
    }

    /**
     * 测试第三方接口。
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:edit')")
    @PostMapping("/remote/test")
    public AjaxResult testRemote(@RequestBody Map<String, Object> request) {
        return success(variableService.testRemoteConnection(request));
    }

    /**
     * 预览第三方接口数据。
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:edit')")
    @PostMapping("/remote/preview")
    public AjaxResult previewRemote(@RequestBody Map<String, Object> request) {
        return success(variableService.previewRemoteData(request));
    }

    /**
     * 刷新第三方变量缓存。
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:edit')")
    @PostMapping("/remote/refresh")
    public AjaxResult refreshRemote(@RequestBody(required = false) Map<String, Object> request) {
        Long sceneId = null;
        if (request != null && request.get("sceneId") != null) {
            sceneId = Long.valueOf(String.valueOf(request.get("sceneId")));
        }
        return success(variableService.refreshRemoteCache(sceneId));
    }
}
