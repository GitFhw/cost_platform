<template>
  <div class="app-container formula-lab">
    <section v-show="!isCompactMode" class="formula-lab__hero">
      <div>
        <div class="formula-lab__eyebrow">公式资产工作台</div>
        <h2 class="formula-lab__title">公式实验室</h2>
        <p class="formula-lab__subtitle">
          面向业务配置人员的公式工作台，可通过变量、条件、函数和模板快速组织公式表达，统一沉淀标准化公式资产。
        </p>
      </div>
      <el-tag type="success">支持按场景组织公式配置，并自动生成中文说明与标准表达式</el-tag>
    </section>

    <section v-show="!isCompactMode" class="formula-lab__metrics">
      <div v-for="item in metricItems" :key="item.label" class="formula-lab__metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </div>
    </section>

    <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="84px" v-show="showSearch">
      <el-form-item label="所属场景" prop="sceneId">
        <el-select v-model="queryParams.sceneId" clearable filterable placeholder="请选择场景" style="width: 240px" @change="handleQuerySceneChange">
          <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
        </el-select>
      </el-form-item>
      <el-form-item label="业务域" prop="businessDomain">
        <el-select v-model="queryParams.businessDomain" clearable placeholder="请选择业务域" style="width: 180px">
          <el-option v-for="item in businessDomainOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="公式编码" prop="formulaCode">
        <el-input v-model="queryParams.formulaCode" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="公式名称" prop="formulaName">
        <el-input v-model="queryParams.formulaName" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="请选择状态" style="width: 160px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="返回类型" prop="returnType">
        <el-select v-model="queryParams.returnType" clearable placeholder="请选择返回类型" style="width: 160px">
          <el-option v-for="item in returnTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="资产类型" prop="assetType">
        <el-select v-model="queryParams.assetType" clearable placeholder="请选择资产类型" style="width: 160px">
          <el-option v-for="item in assetTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <section class="formula-lab__workspace">
      <div class="formula-lab__builder">
        <div class="formula-lab__panel-head">
          <div>
            <h3>公式工作台</h3>
            <p>业务人员优先维护中文公式；结构助手用于条件分支与区间档位，系统统一编译标准表达式。</p>
          </div>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
        </div>

        <div class="formula-lab__toolbar">
          <el-select v-model="form.sceneId" filterable placeholder="请选择场景" style="width: 240px" @change="handleWorkbenchSceneChange">
            <el-option v-for="item in sceneOptions" :key="item.sceneId" :label="`${item.sceneName} / ${item.sceneCode}`" :value="item.sceneId" />
          </el-select>
          <el-segmented v-model="workbench.mode" :options="builderModes" />
          <el-button type="primary" icon="Plus" @click="handleCreate">新建公式</el-button>
          <el-button type="success" icon="Select" @click="handleSave" v-hasPermi="['cost:formula:add']">保存公式</el-button>
          <el-button type="info" icon="Promotion" @click="handleTest" v-hasPermi="['cost:formula:test']">立即试算</el-button>
        </div>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form ref="formulaRef" :model="form" :rules="rules" label-width="96px">
              <el-form-item label="公式编码" prop="formulaCode"><el-input v-model="form.formulaCode" /></el-form-item>
              <el-form-item label="公式名称" prop="formulaName"><el-input v-model="form.formulaName" /></el-form-item>
              <el-form-item label="返回类型" prop="returnType">
                <el-select v-model="form.returnType" style="width: 100%">
                  <el-option v-for="item in returnTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="资产类型" prop="assetType">
                <el-radio-group v-model="form.assetType">
                  <el-radio-button v-for="item in assetTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="命名空间"><el-input v-model="form.namespaceScope" /></el-form-item>
              <el-form-item label="状态" prop="status">
                <el-radio-group v-model="form.status">
                  <el-radio v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="业务说明"><el-input v-model="form.formulaDesc" type="textarea" :rows="2" /></el-form-item>
            </el-form>
          </el-col>
          <el-col :span="12">
            <div class="formula-lab__preview-card">
              <div class="formula-lab__preview-title">业务公式预览</div>
              <div class="formula-lab__preview-text">{{ activeBusinessFormula || '请先在下方业务编排区维护中文公式，系统会自动生成业务表达。' }}</div>
            </div>
            <div class="formula-lab__preview-card formula-lab__preview-card--code">
              <div class="formula-lab__preview-title">标准表达式预览</div>
              <pre class="formula-lab__code">{{ activeFormulaExpression || '系统会把中文公式或结构助手内容编译为标准表达式。' }}</pre>
            </div>
            <el-alert
              v-if="formulaValidationMessages.length"
              title="表达式预校验未通过"
              type="warning"
              :closable="false"
              show-icon
              class="mt12"
            >
              <template #default>
                <div v-for="message in formulaValidationMessages" :key="message" class="formula-lab__validation-item">{{ message }}</div>
              </template>
            </el-alert>
            <el-alert
              v-else-if="activeFormulaExpression"
              title="表达式预校验通过"
              description="括号、命名空间和场景变量引用已通过前端预校验。"
              type="success"
              :closable="false"
              show-icon
              class="mt12"
            />
          </el-col>
        </el-row>

        <div v-if="workbench.mode === 'BUSINESS'" class="formula-lab__business">
          <div class="formula-lab__editor-card">
            <div class="formula-lab__section-title">
              <span>中文公式编辑区</span>
              <el-button link type="primary" @click="scrollToResourceWorkbench">定位资源工作台</el-button>
            </div>
            <el-form label-width="96px">
              <el-form-item label="中文公式">
                <el-input
                  ref="businessFormulaInputRef"
                  v-model="form.businessFormula"
                  type="textarea"
                  :rows="5"
                  placeholder="例如：四舍五入((21700 / 6) × 队女工人数 × (女工实际出勤 / 最大值(女工应出勤, 1)), 2)"
                  @click="captureBusinessCursor"
                  @keyup="captureBusinessCursor"
                  @mouseup="captureBusinessCursor"
                />
              </el-form-item>
            </el-form>
            <div class="formula-lab__note-card">
              <strong>业务侧只维护中文公式</strong>
              <span>系统会自动把中文变量名、中文费用名、中文函数和条件结构编译成标准表达式，并用于保存、试算和后续治理。</span>
            </div>
          </div>

          <div class="formula-lab__draft-card">
            <div class="formula-lab__section-title">
              <span>可视化公式草稿</span>
              <div class="formula-lab__draft-actions">
                <el-button link type="primary" @click="focusBusinessEditor">继续输入</el-button>
                <el-button link type="warning" @click="focusFirstRiskToken" :disabled="!businessRiskTokens.length">定位风险片段</el-button>
                <el-button link type="info" @click="removeLastBusinessToken" :disabled="!businessDraftTokens.length">回退一段</el-button>
                <el-button link type="danger" @click="clearBusinessFormula" :disabled="!activeBusinessFormula">清空草稿</el-button>
              </div>
            </div>
            <div v-if="businessDraftTokens.length" class="formula-lab__draft-list">
              <button
                v-for="token in businessDraftTokens"
                :key="token.key"
                type="button"
                class="formula-lab__draft-token"
                :class="[`is-${token.type}`, { 'is-active': selectedDraftKey === token.key }]"
                @click="focusBusinessToken(token)"
              >
                <span>{{ token.label }}</span>
                <em>{{ token.typeLabel }}</em>
                <i @click.stop="removeBusinessToken(token)">×</i>
              </button>
            </div>
            <el-empty v-else :image-size="72" description="先输入中文公式或点选变量、费用、函数、运算符，这里会自动拆成业务块。" />
          </div>

          <div class="formula-lab__quick-grid">
            <div class="formula-lab__quick-card">
              <div class="formula-lab__tool-title">运算符与数字速插</div>
              <div class="formula-lab__chip-grid">
                <button v-for="item in operatorButtons" :key="`operator-${item.label}`" type="button" class="formula-lab__chip" @click="appendBusinessToken(item.value)">{{ item.label }}</button>
                <button v-for="item in numberButtons" :key="`number-${item.label}`" type="button" class="formula-lab__chip" @click="appendBusinessToken(item.value)">{{ item.label }}</button>
              </div>
            </div>

            <div class="formula-lab__quick-card">
              <div class="formula-lab__tool-title">条件比较速插</div>
              <div class="formula-lab__chip-grid">
                <button v-for="item in keywordButtons" :key="`keyword-${item.label}`" type="button" class="formula-lab__chip" @click="appendBusinessToken(item.value)">{{ item.label }}</button>
              </div>
            </div>

            <div class="formula-lab__quick-card">
              <div class="formula-lab__tool-title">常用函数速插</div>
              <div class="formula-lab__chip-grid">
                <button v-for="item in functionButtons" :key="`function-${item.label}`" type="button" class="formula-lab__chip" @click="appendBusinessToken(item.value)">{{ item.label }}</button>
              </div>
            </div>

            <div class="formula-lab__quick-card">
              <div class="formula-lab__tool-title">场景变量速插</div>
              <div class="formula-lab__chip-grid">
                <button v-for="item in highFrequencyVariableOptions" :key="`variable-${item.variableCode}`" type="button" class="formula-lab__chip" @click="appendBusinessToken(item.variableName || item.variableCode)">{{ item.variableName || item.variableCode }}</button>
              </div>
            </div>

            <div class="formula-lab__quick-card">
              <div class="formula-lab__tool-title">上下文费用速插</div>
              <div class="formula-lab__chip-grid">
                <button v-for="item in highFrequencyFeeOptions" :key="`fee-${item.feeCode}`" type="button" class="formula-lab__chip" @click="appendBusinessToken(item.feeName || item.feeCode)">{{ item.feeName || item.feeCode }}</button>
              </div>
            </div>
          </div>

          <div class="formula-lab__test-card">
            <div class="formula-lab__section-title">
              <span>试算上下文</span>
              <el-button link type="primary" @click="handleGenerateSample">按引用生成示例</el-button>
            </div>
            <JsonEditor v-model="testInputJson" title="试算上下文 JSON" :rows="6" placeholder="请输入测试 JSON，上下文建议按 V/C/I/F/T 命名空间组织。" />
            <div class="formula-lab__test-result">
              <div><strong>试算结果：</strong>{{ testResultDisplay }}</div>
            </div>
          </div>
        </div>

        <div v-else class="formula-lab__guided">
          <div class="formula-lab__pattern-bar">
            <el-radio-group v-model="workbench.pattern">
              <el-radio-button label="IF_ELSE">条件分支</el-radio-button>
              <el-radio-button label="RANGE_LOOKUP">区间档位</el-radio-button>
            </el-radio-group>
          </div>

          <div v-if="workbench.pattern === 'IF_ELSE'">
            <div class="formula-lab__section-title">
              <span>条件配置</span>
              <el-button type="primary" plain icon="Plus" @click="handleAddCondition">新增条件</el-button>
            </div>
            <el-table :data="workbench.conditions" size="small" border>
              <el-table-column label="变量" min-width="220">
                <template #default="scope">
                  <el-select
                    v-model="scope.row.variableCode"
                    filterable
                    :placeholder="resolveVariablePlaceholder()"
                    @change="value => handleConditionVariableChange(scope.row, value)"
                  >
                    <el-option v-for="item in variableOptions" :key="item.variableCode" :label="`${item.variableName} / ${item.variableCode}`" :value="item.variableCode" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="操作符" width="140">
                <template #default="scope">
                  <el-select v-model="scope.row.operatorCode" placeholder="请选择操作符">
                    <el-option v-for="item in conditionOperators" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="比较值" min-width="200">
                <template #default="scope">
                  <el-input v-model="scope.row.compareValue" placeholder="如：煤炭 / 白班 / 100" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90" align="center">
                <template #default="scope">
                  <el-button link type="danger" icon="Delete" @click="handleRemoveCondition(scope.$index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="formula-lab__result-grid">
              <div>
                <div class="formula-lab__field-label">命中结果</div>
                <el-input v-model="workbench.trueResultValue" placeholder="如：2 或 V.煤炭单价" />
              </div>
              <div>
                <div class="formula-lab__field-label">未命中结果</div>
                <el-input v-model="workbench.falseResultValue" placeholder="如：1 或 0" />
              </div>
            </div>
          </div>

          <div v-else>
            <div class="formula-lab__result-grid">
              <div>
                <div class="formula-lab__field-label">区间依据变量</div>
                <el-select v-model="workbench.rangeVariableCode" filterable :placeholder="resolveVariablePlaceholder('请选择区间变量')">
                    <el-option v-for="item in numericVariableOptions" :key="item.variableCode" :label="`${item.variableName} / ${item.variableCode}`" :value="item.variableCode" />
                </el-select>
              </div>
              <div>
                <div class="formula-lab__field-label">兜底结果</div>
                <el-input v-model="workbench.defaultResultValue" placeholder="未命中任一档位时返回" />
              </div>
            </div>
            <div class="formula-lab__section-title">
              <span>区间档位</span>
              <el-button type="primary" plain icon="Plus" @click="handleAddRange">新增档位</el-button>
            </div>
            <el-table :data="workbench.ranges" size="small" border>
              <el-table-column label="起始值" min-width="120"><template #default="scope"><el-input v-model="scope.row.startValue" /></template></el-table-column>
              <el-table-column label="结束值" min-width="120"><template #default="scope"><el-input v-model="scope.row.endValue" /></template></el-table-column>
              <el-table-column label="结果值" min-width="160"><template #default="scope"><el-input v-model="scope.row.resultValue" /></template></el-table-column>
              <el-table-column label="操作" width="90" align="center"><template #default="scope"><el-button link type="danger" icon="Delete" @click="handleRemoveRange(scope.$index)">删除</el-button></template></el-table-column>
            </el-table>
          </div>

          <div class="formula-lab__test-card">
            <div class="formula-lab__section-title"><span>试算上下文</span><el-button link type="primary" @click="handleGenerateSample">按引用生成示例</el-button></div>
            <JsonEditor v-model="testInputJson" title="试算上下文 JSON" :rows="6" placeholder="请输入测试 JSON，上下文建议按 V/C/I/F/T 命名空间组织。" />
            <div class="formula-lab__test-result">
              <div><strong>试算结果：</strong>{{ testResultDisplay }}</div>
            </div>
          </div>
        </div>
      </div>

      <aside ref="resourceWorkbenchRef" class="formula-lab__toolbox">
        <div class="formula-lab__panel-head">
          <div>
            <h3>资源工作台</h3>
            <p>同页完成中文公式排障、变量/费用映射核对和资源点选，不再来回跳转。</p>
          </div>
        </div>
        <div class="formula-lab__tool-section">
          <div class="formula-lab__tool-title">业务编排建议</div>
          <div class="formula-lab__note-list">
            <div class="formula-lab__note-item">
              <strong>先写中文，再由系统翻译</strong>
              <span>优先维护业务人员能读懂的中文公式，复杂分档建议切到结构助手，不建议直接手写长段编码表达式。</span>
            </div>
          </div>
        </div>
        <div class="formula-lab__tool-section">
          <div class="formula-lab__tool-title">定位式校验</div>
          <div v-if="workbench.mode === 'BUSINESS' && businessIssueItems.length" class="formula-lab__issue-list">
            <div v-for="(item, index) in businessIssueItems" :key="item.key" class="formula-lab__issue-item">
              <strong>中文编排问题 {{ index + 1 }}</strong>
              <p>{{ item.message }}</p>
              <span v-if="item.fragment">定位片段：{{ item.fragment }}</span>
              <span v-if="item.suggestion">建议：{{ item.suggestion }}</span>
              <div v-if="getRiskSuggestions(item.fragment).length" class="formula-lab__issue-actions">
                <el-button
                  v-for="suggestion in getRiskSuggestions(item.fragment)"
                  :key="`${item.key}-${suggestion.value}`"
                  link
                  type="primary"
                  @click="replaceRiskFragment(item.fragment, suggestion.value)"
                >
                  替换为 {{ suggestion.label }}
                </el-button>
              </div>
            </div>
          </div>
          <el-alert
            v-else-if="formulaValidationMessages.length"
            title="标准表达式校验未通过"
            type="warning"
            :closable="false"
            show-icon
          >
            <template #default>
              <div v-for="message in formulaValidationMessages" :key="message" class="formula-lab__validation-item">{{ message }}</div>
            </template>
          </el-alert>
          <el-alert
            v-else
            title="当前公式可用于保存与试算"
            description="中文公式编译与标准表达式预校验已通过。"
            type="success"
            :closable="false"
            show-icon
          />
        </div>
        <div class="formula-lab__tool-section">
          <div class="formula-lab__tool-title">引用映射清单</div>
          <div v-if="referenceDetails.length" class="formula-lab__reference-list">
            <div v-for="item in referenceDetails" :key="item.key" class="formula-lab__reference-item">
              <strong>{{ item.label }}</strong>
              <span>{{ item.type === 'variable' ? `V.${item.code}` : `F.${item.code}` }}</span>
              <small>{{ item.description }}</small>
            </div>
          </div>
          <el-empty v-else :image-size="72" description="编辑区里还没有识别到变量或上下文费用引用。" />
        </div>
        <div class="formula-lab__tool-section">
          <div class="formula-lab__section-title">
            <span>资源工作台</span>
            <el-input v-model="resourceKeyword" clearable placeholder="搜索变量、费用、函数" style="width: 220px" />
          </div>
          <ExpressionResourcePanel :sections="businessResourceSections" @append="handleAppendResourceToken" />
        </div>
        <div class="formula-lab__tool-section">
          <div class="formula-lab__tool-title">平台模板</div>
          <div class="formula-lab__list">
            <button v-for="item in platformTemplates" :key="item.code" type="button" class="formula-lab__list-item" @click="applyTemplate(item)">
              <strong>{{ item.name }}</strong>
              <span>{{ item.desc }}</span>
            </button>
          </div>
        </div>
        <div class="formula-lab__tool-section">
          <div class="formula-lab__tool-title">模板库</div>
          <div v-if="templateOptionList.length" class="formula-lab__list">
            <button v-for="item in templateOptionList" :key="item.formulaId" type="button" class="formula-lab__list-item" @click="applyStoredTemplate(item)">
              <strong>{{ item.formulaName }}</strong>
              <span>{{ item.formulaCode }} · V{{ item.currentVersionNo || 1 }}</span>
            </button>
          </div>
          <el-empty v-else :image-size="72" description="当前场景还没有沉淀模板资产，可先把常用公式保存为模板。" />
        </div>
        <div class="formula-lab__tool-section">
          <div class="formula-lab__tool-title">已有公式</div>
          <div class="formula-lab__list">
            <button v-for="item in formulaOptionList" :key="item.formulaCode" type="button" class="formula-lab__list-item" @click="handleLoadFormula(item)">
              <strong>{{ item.formulaName }}</strong>
              <span>{{ item.formulaCode }}</span>
            </button>
          </div>
        </div>
      </aside>
    </section>

    <section class="formula-lab__ledger">
      <div class="formula-lab__panel-head">
        <div>
          <h3>公式资产台账</h3>
          <p>台账只负责资产管理，公式生成、预览和试算全部回到上方工作台完成。</p>
        </div>
      </div>

      <el-table v-loading="loading" :data="formulaList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="场景" min-width="200"><template #default="scope">{{ scope.row.sceneCode }} / {{ scope.row.sceneName }}</template></el-table-column>
        <el-table-column label="公式编码" prop="formulaCode" width="180" />
        <el-table-column label="公式名称" prop="formulaName" min-width="180" />
        <el-table-column label="资产类型" width="110">
          <template #default="scope">
            <el-tag :type="scope.row.assetType === 'TEMPLATE' ? 'warning' : 'primary'">{{ resolveAssetTypeLabel(scope.row.assetType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前版本" width="100" align="center">
          <template #default="scope">V{{ scope.row.currentVersionNo || 1 }}</template>
        </el-table-column>
        <el-table-column label="返回类型" width="120"><template #default="scope"><dict-tag :options="returnTypeOptions" :value="scope.row.returnType" /></template></el-table-column>
        <el-table-column label="状态" width="110"><template #default="scope"><dict-tag :options="statusOptions" :value="scope.row.status" /></template></el-table-column>
        <el-table-column label="变量引用" prop="variableRefCount" width="100" align="center" />
        <el-table-column label="规则引用" prop="ruleRefCount" width="100" align="center" />
        <el-table-column label="操作" width="340" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleEdit(scope.row)">装载编辑</el-button>
            <el-button link type="primary" icon="Collection" @click="handleOpenVersions(scope.row)">版本</el-button>
            <el-button link type="primary" icon="View" @click="handleGovernance(scope.row)">治理</el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['cost:formula:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </section>

    <el-drawer v-model="governanceOpen" title="公式治理检查" size="520px" append-to-body>
      <div v-if="governanceInfo.formulaId">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="公式">{{ governanceInfo.formulaCode }} / {{ governanceInfo.formulaName }}</el-descriptions-item>
          <el-descriptions-item label="变量引用">{{ governanceInfo.variableRefCount }}</el-descriptions-item>
          <el-descriptions-item label="规则引用">{{ governanceInfo.ruleRefCount }}</el-descriptions-item>
          <el-descriptions-item label="发布快照引用">{{ governanceInfo.publishedVersionCount }}</el-descriptions-item>
        </el-descriptions>
        <el-alert class="mt12" :title="governanceInfo.canDelete ? '允许删除' : '当前不允许删除'" :description="governanceInfo.removeBlockingReason || '当前公式未被变量、规则和发布快照引用。'" :type="governanceInfo.canDelete ? 'success' : 'warning'" :closable="false" show-icon />
        <el-alert class="mt12" :title="governanceInfo.canDisable ? '允许停用' : '当前不允许停用'" :description="governanceInfo.disableBlockingReason || '当前公式未进入任何发布快照，可以停用。'" :type="governanceInfo.canDisable ? 'success' : 'warning'" :closable="false" show-icon />
      </div>
    </el-drawer>

    <el-drawer v-model="versionOpen" title="公式版本台账" size="760px" append-to-body>
      <div class="formula-lab__drawer-head">
        <div>
          <strong>{{ versionFormulaTitle || '请选择公式查看版本' }}</strong>
          <p>历史版本用于回看公式演进，也可一键装载到上方工作台继续维护。</p>
        </div>
      </div>
      <el-table :data="versionList" v-loading="versionLoading">
        <el-table-column label="版本号" width="90" align="center">
          <template #default="scope">V{{ scope.row.versionNo }}</template>
        </el-table-column>
        <el-table-column label="变更类型" width="110" align="center">
          <template #default="scope">
            <el-tag :type="resolveVersionChangeType(scope.row.changeType).type">{{ resolveVersionChangeType(scope.row.changeType).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="资产类型" width="110" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.assetType === 'TEMPLATE' ? 'warning' : 'primary'">{{ resolveAssetTypeLabel(scope.row.assetType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="业务公式" prop="businessFormula" min-width="220" show-overflow-tooltip />
        <el-table-column label="保存人" prop="createBy" width="120" />
        <el-table-column label="保存时间" prop="createTime" width="180" />
        <el-table-column label="操作" width="220" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="RefreshRight" @click="handleLoadVersion(scope.row)">装载此版</el-button>
            <el-button link type="warning" icon="RefreshLeft" @click="handleRollbackVersion(scope.row)" v-hasPermi="['cost:formula:edit']">回滚为当前版</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup name="CostFormula">
import { ElMessageBox } from 'element-plus'
import ExpressionResourcePanel from '@/components/cost/ExpressionResourcePanel.vue'
import JsonEditor from '@/components/cost/JsonEditor.vue'
import {
  addFormula,
  delFormula,
  getFormula,
  getFormulaGovernance,
  getFormulaStats,
  getFormulaVersion,
  listFormula,
  listFormulaTemplates,
  listFormulaVersions,
  optionselectFormula,
  rollbackFormulaVersion,
  testFormula,
  updateFormula
} from '@/api/cost/formula'
import { optionselectFee } from '@/api/cost/fee'
import { optionselectScene } from '@/api/cost/scene'
import useSettingsStore from '@/store/modules/settings'
import { compileCostBusinessFormula } from '@/utils/costBusinessFormulaCompiler'
import { validateCostExpression } from '@/utils/costExpressionValidation'
import { optionselectVariable } from '@/api/cost/variable'
import { resolveWorkingCostSceneId } from '@/utils/costSceneContext'
import { getRemoteDictOptionMap } from '@/utils/dictRemote'

const route = useRoute()
const { proxy } = getCurrentInstance()
const settingsStore = useSettingsStore()
const isCompactMode = computed(() => settingsStore.costPageMode === 'COMPACT')

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const sceneOptions = ref([])
const variableOptions = ref([])
const feeOptions = ref([])
const formulaOptionList = ref([])
const templateOptionList = ref([])
const businessDomainOptions = ref([])
const statusOptions = ref([])
const returnTypeOptions = ref([])
const ids = ref([])
const governanceOpen = ref(false)
const governanceInfo = ref({})
const versionOpen = ref(false)
const versionLoading = ref(false)
const versionList = ref([])
const versionFormulaTitle = ref('')
const formulaList = ref([])
const testInputJson = ref('')
const testResult = ref(undefined)
const resourceKeyword = ref('')
const businessFormulaInputRef = ref()
const resourceWorkbenchRef = ref()
const businessCursor = reactive({ start: 0, end: 0 })
const selectedDraftKey = ref('')

const statistics = reactive({
  formulaCount: 0,
  enabledFormulaCount: 0,
  variableRefCount: 0,
  ruleRefCount: 0
})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  sceneId: undefined,
  businessDomain: undefined,
  formulaCode: undefined,
  formulaName: undefined,
  status: undefined,
  returnType: undefined,
  assetType: undefined
})

const form = reactive({
  formulaId: undefined,
  sceneId: undefined,
  formulaCode: undefined,
  formulaName: undefined,
  formulaDesc: undefined,
  businessFormula: undefined,
  formulaExpr: undefined,
  assetType: 'FORMULA',
  workbenchMode: 'BUSINESS',
  workbenchPattern: 'IF_ELSE',
  templateCode: undefined,
  workbenchConfigJson: undefined,
  namespaceScope: 'V,C,I,F,T',
  returnType: 'NUMBER',
  testCaseJson: undefined,
  status: '0',
  sortNo: 10,
  remark: undefined
})

const workbench = reactive({
  mode: 'BUSINESS',
  pattern: 'IF_ELSE',
  templateCode: undefined,
  conditionLogic: 'AND',
  conditions: [],
  trueResultValue: '',
  falseResultValue: '',
  rangeVariableCode: undefined,
  ranges: [],
  defaultResultValue: ''
})

const assetTypeOptions = [
  { label: '公式资产', value: 'FORMULA' },
  { label: '模板资产', value: 'TEMPLATE' }
]

const rules = {
  sceneId: [{ required: true, message: '所属场景不能为空', trigger: 'change' }],
  formulaCode: [{ required: true, message: '公式编码不能为空', trigger: 'blur' }],
  formulaName: [{ required: true, message: '公式名称不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }],
  assetType: [{ required: true, message: '资产类型不能为空', trigger: 'change' }],
  returnType: [{ required: true, message: '返回类型不能为空', trigger: 'change' }]
}

const builderModes = [
  { label: '业务编排', value: 'BUSINESS' },
  { label: '结构助手', value: 'GUIDED' }
]

const conditionOperators = [
  { label: '等于', value: 'EQ' },
  { label: '不等于', value: 'NE' },
  { label: '大于', value: 'GT' },
  { label: '大于等于', value: 'GE' },
  { label: '小于', value: 'LT' },
  { label: '小于等于', value: 'LE' }
]

const operatorButtons = [
  { label: '+', value: ' + ' },
  { label: '-', value: ' - ' },
  { label: '×', value: ' × ' },
  { label: '÷', value: ' ÷ ' },
  { label: '%', value: '%' },
  { label: '（', value: '（' },
  { label: '）', value: '）' },
  { label: '，', value: '，' }
]

const functionButtons = [
  { label: '四舍五入', value: '四舍五入(, 2)', desc: '结果保留指定精度。' },
  { label: '最大值', value: '最大值(, )', desc: '适合保底值与上限比较。' },
  { label: '最小值', value: '最小值(, )', desc: '适合封顶值与下限比较。' },
  { label: '空值兜底', value: '空值兜底(, )', desc: '空值时回退到默认值。' }
]

const keywordButtons = [
  { label: '如果', value: '如果 ' },
  { label: '那么', value: ' 那么 ' },
  { label: '否则', value: ' 否则 ' },
  { label: '且', value: ' 且 ' },
  { label: '或', value: ' 或 ' },
  { label: '等于', value: ' 等于 ' },
  { label: '不等于', value: ' 不等于 ' },
  { label: '大于', value: ' 大于 ' },
  { label: '大于等于', value: ' 大于等于 ' },
  { label: '小于', value: ' 小于 ' },
  { label: '小于等于', value: ' 小于等于 ' }
]

const numberButtons = [
  { label: '7', value: '7' },
  { label: '8', value: '8' },
  { label: '9', value: '9' },
  { label: '4', value: '4' },
  { label: '5', value: '5' },
  { label: '6', value: '6' },
  { label: '1', value: '1' },
  { label: '2', value: '2' },
  { label: '3', value: '3' },
  { label: '0', value: '0' },
  { label: '00', value: '00' },
  { label: '.', value: '.' }
]

const namespaceTokens = [
  { label: 'V.变量', value: 'V.', type: 'warning', desc: '标准变量上下文' },
  { label: 'I.输入', value: 'I.', type: 'warning', desc: '原始输入 JSON' },
  { label: 'C.上下文', value: 'C.', type: 'warning', desc: '场景/版本/账期等运行信息' },
  { label: 'F.费用结果', value: 'F.', type: 'warning', desc: '前序费用结果与试算费用对象' },
  { label: 'T.临时值', value: 'T.', type: 'warning', desc: '预留临时变量空间' }
]

const platformTemplates = [
  {
    code: 'CARGO_SHIFT_PRICE',
    name: '货种 + 班次计价',
    desc: '适合“货种 = 煤炭 且 班次 = 白班时取 2 元，否则取 1 元”这类业务口径。',
    pattern: 'IF_ELSE',
    conditions: [
      { variableCode: '', operatorCode: 'EQ', compareValue: '' },
      { variableCode: '', operatorCode: 'EQ', compareValue: '' }
    ],
    trueResultValue: '2',
    falseResultValue: '1'
  },
  {
    code: 'RANGE_RATE',
    name: '区间档位取价',
    desc: '适合按天数、重量、面积等数值做区间价。',
    pattern: 'RANGE_LOOKUP',
    ranges: [
      { startValue: '0', endValue: '10', resultValue: '1' },
      { startValue: '10', endValue: '20', resultValue: '2' }
    ],
    defaultResultValue: '0'
  },
  {
    code: 'KEEP_AMOUNT',
    name: '面积 × 天数 × 单价',
    desc: '适合仓储保管费类金额公式，直接用中文公式编排即可。',
    pattern: 'BUSINESS',
    businessFormula: '面积 × 天数 × 单价'
  }
]

const metricItems = computed(() => [
  { label: '公式总数', value: statistics.formulaCount, desc: '当前筛选范围内沉淀的公式资产数' },
  { label: '启用公式', value: statistics.enabledFormulaCount, desc: '状态为正常的公式数量' },
  { label: '变量引用', value: statistics.variableRefCount, desc: '已被变量中心引用的次数' },
  { label: '规则引用', value: statistics.ruleRefCount, desc: '已被规则中心引用的次数' }
])

const variableMetaMap = computed(() => variableOptions.value.reduce((acc, item) => {
  acc[item.variableCode] = item
  return acc
}, {}))

const feeMetaMap = computed(() => feeOptions.value.reduce((acc, item) => {
  acc[item.feeCode] = item
  return acc
}, {}))

const highFrequencyVariableOptions = computed(() => variableOptions.value.slice(0, 12))
const highFrequencyFeeOptions = computed(() => feeOptions.value.slice(0, 12))

const businessCompileResult = computed(() => compileCostBusinessFormula({
  businessFormula: form.businessFormula,
  variableOptions: variableOptions.value,
  feeOptions: feeOptions.value
}))

const activeBusinessFormula = computed(() => {
  return workbench.mode === 'GUIDED'
    ? String(derivedFormula.value.businessFormula || '').trim()
    : String(form.businessFormula || '').trim()
})

const activeFormulaExpression = computed(() => {
  return workbench.mode === 'GUIDED'
    ? String(derivedFormula.value.formulaExpr || '').trim()
    : String(businessCompileResult.value.expression || '').trim()
})

const formulaValidationResult = computed(() => {
  if (workbench.mode !== 'GUIDED' && businessCompileResult.value.issues?.length) {
    return {
      valid: false,
      issues: businessCompileResult.value.issues.map(item => ({ message: item.message })),
      namespaces: [],
      variableRefs: businessCompileResult.value.variableRefs || [],
      feeRefs: businessCompileResult.value.feeRefs || []
    }
  }
  return validateCostExpression({
    expression: activeFormulaExpression.value,
    namespaceScope: form.namespaceScope,
    variableCodes: variableOptions.value.map(item => item.variableCode),
    feeCodes: feeOptions.value.map(item => item.feeCode),
    validateVariableRefs: Boolean(form.sceneId),
    validateFeeRefs: Boolean(form.sceneId)
  })
})

const formulaValidationMessages = computed(() => formulaValidationResult.value.issues.map(item => item.message))

const referenceDetails = computed(() => {
  const variableRefs = (formulaValidationResult.value.variableRefs || []).map(code => {
    const meta = variableMetaMap.value[code]
    const readPath = String(meta?.dataPath || '').trim()
    return {
      key: `V-${code}`,
      type: 'variable',
      label: meta?.variableName || code,
      code,
      description: readPath ? `来源路径：${readPath}` : `平铺字段：${meta?.variableCode || code}`
    }
  })
  const feeRefs = (formulaValidationResult.value.feeRefs || []).map(code => {
    const meta = feeMetaMap.value[code]
    return {
      key: `F-${code}`,
      type: 'fee',
      label: meta?.feeName || code,
      code,
      description: `上下文费用：F.${code}`
    }
  })
  return [...variableRefs, ...feeRefs]
})

const businessIssueItems = computed(() => {
  return (businessCompileResult.value.issues || []).map((item, index) => ({
    key: `${item.code || 'ISSUE'}-${index}`,
    message: item.message,
    fragment: item.fragment || '',
    suggestion: item.suggestion || ''
  }))
})

const businessTokenDefinitions = computed(() => {
  return [
    ...variableOptions.value.map(item => ({
      type: 'variable',
      label: item.variableName,
      value: item.variableName,
      code: item.variableCode,
      description: item.dataPath ? `来源路径：${item.dataPath}` : `平铺字段：${item.variableCode}`
    })),
    ...feeOptions.value.map(item => ({
      type: 'fee',
      label: item.feeName,
      value: item.feeName,
      code: item.feeCode,
      description: `上下文费用：F.${item.feeCode}`
    })),
    ...functionButtons.map(item => ({
      type: 'function',
      label: item.label,
      value: item.label,
      description: item.desc
    })),
    ...keywordButtons.map(item => ({
      type: 'keyword',
      label: item.label,
      value: item.label.trim(),
      description: '条件与比较关键字'
    }))
  ].filter(item => item.label).sort((a, b) => String(b.label).length - String(a.label).length)
})

const businessDraftTokens = computed(() => tokenizeBusinessFormula(activeBusinessFormula.value, businessTokenDefinitions.value))
const businessRiskTokens = computed(() => businessDraftTokens.value.filter(item => item.type === 'risk'))

const businessResourceSections = computed(() => {
  const keyword = resourceKeyword.value.trim().toLowerCase()
  const filterItems = items => {
    if (!keyword) return items
    return items.filter(item => `${item.label || ''} ${item.desc || ''}`.toLowerCase().includes(keyword))
  }
  return [
    {
      key: 'function',
      title: '中文函数',
      tip: '业务人员优先写中文函数，系统会自动编译成标准表达式。',
      display: 'tag',
      items: filterItems(functionButtons.map(item => ({ ...item, type: 'success' })))
    },
    {
      key: 'keyword',
      title: '条件关键字',
      display: 'tag',
      items: filterItems(keywordButtons.map(item => ({ ...item, type: 'warning' })))
    },
    {
      key: 'variable',
      title: '场景变量',
      display: 'list',
      emptyText: resolveVariablePlaceholder('当前场景暂无变量，请先到变量中心维护'),
      items: filterItems(variableOptions.value.map(item => ({
        label: item.variableName || item.variableCode,
        value: item.variableName || item.variableCode,
        desc: `${item.variableCode}${item.dataPath ? ` / ${item.dataPath}` : ''}${item.dataType ? ` / ${item.dataType}` : ''}`
      })))
    },
    {
      key: 'fee',
      title: '上下文费用',
      display: 'list',
      emptyText: '当前场景暂无可引用费用，请先维护费用主线。',
      items: filterItems(feeOptions.value.map(item => ({
        label: item.feeName || item.feeCode,
        value: item.feeName || item.feeCode,
        desc: `${item.feeCode} / 前序费用结果或试算 F 对象`
      })))
    },
    {
      key: 'namespace',
      title: '系统命名空间说明',
      display: 'tag',
      items: namespaceTokens
    }
  ]
})

const numericVariableOptions = computed(() => {
  return variableOptions.value.filter(item => ['NUMBER', 'INTEGER', 'DECIMAL', 'LONG'].includes(String(item.dataType || '').toUpperCase()))
})

const derivedFormula = computed(() => {
  return workbench.pattern === 'RANGE_LOOKUP' ? buildRangeFormula() : buildIfElseFormula()
})

const testResultDisplay = computed(() => {
  if (typeof testResult.value === 'undefined') {
    return '尚未试算'
  }
  if (typeof testResult.value === 'string') {
    return testResult.value
  }
  return JSON.stringify(testResult.value, null, 2)
})

async function loadBaseOptions() {
  const [dictMap, sceneResponse] = await Promise.all([
    getRemoteDictOptionMap(['cost_business_domain', 'cost_formula_status', 'cost_formula_return_type']),
    optionselectScene({ status: '0', pageNum: 1, pageSize: 1000 })
  ])
  businessDomainOptions.value = dictMap.cost_business_domain || []
  statusOptions.value = dictMap.cost_formula_status || []
  returnTypeOptions.value = dictMap.cost_formula_return_type || []
  sceneOptions.value = sceneResponse?.data || []
  queryParams.sceneId = resolveWorkingCostSceneId(sceneOptions.value, queryParams.sceneId)
  form.sceneId = resolveWorkingCostSceneId(sceneOptions.value, form.sceneId, queryParams.sceneId)
}

async function loadSceneAssets(sceneId) {
  if (!sceneId) {
    variableOptions.value = []
    feeOptions.value = []
    formulaOptionList.value = []
    templateOptionList.value = []
    return
  }
  const [variableResponse, feeResponse, formulaResponse, templateResponse] = await Promise.all([
    optionselectVariable({ sceneId, status: '0', pageNum: 1, pageSize: 1000 }),
    optionselectFee({ sceneId, status: '0', pageNum: 1, pageSize: 1000 }),
    optionselectFormula({ sceneId, status: '0', pageNum: 1, pageSize: 1000 }),
    listFormulaTemplates({ sceneId, pageNum: 1, pageSize: 1000 })
  ])
  variableOptions.value = variableResponse?.data || []
  feeOptions.value = feeResponse?.data || []
  formulaOptionList.value = (formulaResponse?.data || []).filter(item => item.formulaCode !== form.formulaCode)
  templateOptionList.value = templateResponse?.data || []
}

function normalizeStats(data = {}) {
  statistics.formulaCount = Number(data.formulaCount || 0)
  statistics.enabledFormulaCount = Number(data.enabledFormulaCount || 0)
  statistics.variableRefCount = Number(data.variableRefCount || 0)
  statistics.ruleRefCount = Number(data.ruleRefCount || 0)
}

async function getList() {
  loading.value = true
  try {
    const [listResponse, statsResponse] = await Promise.all([
      listFormula(queryParams),
      getFormulaStats(queryParams)
    ])
    formulaList.value = listResponse?.rows || []
    total.value = listResponse?.total || 0
    normalizeStats(statsResponse?.data || {})
  } finally {
    loading.value = false
  }
}

function resetWorkbench() {
  workbench.mode = 'BUSINESS'
  workbench.pattern = 'IF_ELSE'
  workbench.templateCode = undefined
  workbench.conditionLogic = 'AND'
  workbench.conditions = [{ variableCode: '', operatorCode: 'EQ', compareValue: '' }]
  workbench.trueResultValue = ''
  workbench.falseResultValue = ''
  workbench.rangeVariableCode = undefined
  workbench.ranges = [{ startValue: '', endValue: '', resultValue: '' }]
  workbench.defaultResultValue = ''
}

function resetFormModel() {
  const currentSceneId = resolveWorkingCostSceneId(sceneOptions.value) || form.sceneId || queryParams.sceneId
  form.formulaId = undefined
  form.sceneId = currentSceneId
  form.formulaCode = undefined
  form.formulaName = undefined
  form.formulaDesc = undefined
  form.businessFormula = undefined
  form.formulaExpr = undefined
  form.assetType = 'FORMULA'
  form.workbenchMode = 'BUSINESS'
  form.workbenchPattern = 'IF_ELSE'
  form.templateCode = undefined
  form.workbenchConfigJson = undefined
  form.namespaceScope = 'V,C,I,F,T'
  form.returnType = 'NUMBER'
  form.testCaseJson = undefined
  form.status = '0'
  form.sortNo = 10
  form.remark = undefined
  testInputJson.value = ''
  testResult.value = undefined
  resourceKeyword.value = ''
  selectedDraftKey.value = ''
  resetWorkbench()
  proxy.resetForm('formulaRef')
}

function buildIfElseFormula() {
  const rows = (workbench.conditions || []).filter(item => item.variableCode && item.operatorCode)
  if (!rows.length) {
    return { businessFormula: '', formulaExpr: '' }
  }
  const logicText = workbench.conditionLogic === 'OR' ? '或' : '且'
  const logicExpr = workbench.conditionLogic === 'OR' ? ' or ' : ' and '
  const conditionTexts = rows.map(item => `${resolveVariableLabel(item.variableCode)} ${resolveOperatorLabel(item.operatorCode)} ${formatDisplayValue(item.compareValue)}`)
  const conditionExprs = rows.map(item => buildConditionExpression(item))
  const trueText = formatDisplayValue(workbench.trueResultValue)
  const falseText = formatDisplayValue(workbench.falseResultValue || '0')
  return {
    businessFormula: `当 ${conditionTexts.join(` ${logicText} `)} 时取 ${trueText}，否则取 ${falseText}`,
    formulaExpr: `if(${conditionExprs.join(logicExpr)}, ${normalizeResultToken(workbench.trueResultValue)}, ${normalizeResultToken(workbench.falseResultValue || '0')})`
  }
}

function buildRangeFormula() {
  const variableCode = workbench.rangeVariableCode
  const ranges = (workbench.ranges || []).filter(item => item.startValue !== '' && item.endValue !== '' && item.resultValue !== '')
  if (!variableCode || !ranges.length) {
    return { businessFormula: '', formulaExpr: '' }
  }
  const variableName = resolveVariableLabel(variableCode)
  const businessParts = ranges.map(item => `${item.startValue} - ${item.endValue} 取 ${formatDisplayValue(item.resultValue)}`)
  let expression = normalizeResultToken(workbench.defaultResultValue || '0')
  ;[...ranges].reverse().forEach(item => {
    expression = `if(between(V.${variableCode}, ${item.startValue}, ${item.endValue}), ${normalizeResultToken(item.resultValue)}, ${expression})`
  })
  return {
    businessFormula: `按 ${variableName} 分档：${businessParts.join('；')}；其他取 ${formatDisplayValue(workbench.defaultResultValue || '0')}`,
    formulaExpr: expression
  }
}

function buildConditionExpression(condition) {
  const left = `V.${condition.variableCode}`
  const right = formatExpressionValue(condition.compareValue, condition.variableCode)
  const map = {
    EQ: `${left} == ${right}`,
    NE: `${left} != ${right}`,
    GT: `${left} > ${right}`,
    GE: `${left} >= ${right}`,
    LT: `${left} < ${right}`,
    LE: `${left} <= ${right}`
  }
  return map[condition.operatorCode] || `${left} == ${right}`
}

function formatExpressionValue(value, variableCode) {
  const variableMeta = variableMetaMap.value[variableCode] || {}
  const dataType = String(variableMeta.dataType || '').toUpperCase()
  if (['NUMBER', 'INTEGER', 'DECIMAL', 'LONG'].includes(dataType) && String(value).trim() !== '') {
    return String(value).trim()
  }
  return `'${String(value ?? '').replace(/'/g, "\\'")}'`
}

function normalizeResultToken(value) {
  const text = String(value ?? '').trim()
  if (!text) {
    return '0'
  }
  if (/^(V|C|I|F|T)\./.test(text) || /^if\(/.test(text) || /^between\(/.test(text) || /^round\(/.test(text) || /^coalesce\(/.test(text) || /^max\(/.test(text) || /^min\(/.test(text)) {
    return text
  }
  if (/^-?\d+(\.\d+)?$/.test(text)) {
    return text
  }
  return `'${text.replace(/'/g, "\\'")}'`
}

function formatDisplayValue(value) {
  return String(value ?? '').trim() || '空值'
}

function resolveVariableLabel(variableCode) {
  return variableMetaMap.value[variableCode]?.variableName || variableCode || '未选择变量'
}

function resolveOperatorLabel(operatorCode) {
  return conditionOperators.find(item => item.value === operatorCode)?.label || operatorCode
}

function resolveVariablePlaceholder(emptyText = '当前场景暂无变量，请先到变量中心维护') {
  if (!form.sceneId) {
    return '请先选择场景'
  }
  return variableOptions.value.length ? '请选择变量' : emptyText
}

function resolveAssetTypeLabel(assetType) {
  return assetTypeOptions.find(item => item.value === assetType)?.label || '公式资产'
}

function resolveVersionChangeType(changeType) {
  if (changeType === 'CREATE') {
    return { label: '创建', type: 'success' }
  }
  if (changeType === 'ROLLBACK') {
    return { label: '回滚', type: 'warning' }
  }
  return { label: '更新', type: 'info' }
}

function normalizeWorkbenchMode(mode) {
  return mode === 'GUIDED' ? 'GUIDED' : 'BUSINESS'
}

function getBusinessTextarea() {
  return businessFormulaInputRef.value?.textarea || businessFormulaInputRef.value?.$el?.querySelector('textarea')
}

function captureBusinessCursor() {
  const textarea = getBusinessTextarea()
  if (!textarea) {
    return
  }
  businessCursor.start = textarea.selectionStart ?? 0
  businessCursor.end = textarea.selectionEnd ?? businessCursor.start
}

function focusBusinessEditor(start = businessCursor.start, end = businessCursor.end) {
  nextTick(() => {
    const textarea = getBusinessTextarea()
    if (!textarea) {
      return
    }
    textarea.focus()
    textarea.setSelectionRange(start, end)
    businessCursor.start = start
    businessCursor.end = end
  })
}

function setBusinessFormulaText(text, start = text.length, end = start) {
  form.businessFormula = text
  focusBusinessEditor(start, end)
}

function appendBusinessToken(token) {
  workbench.mode = 'BUSINESS'
  workbench.templateCode = undefined
  captureBusinessCursor()
  const source = String(form.businessFormula || '')
  const start = businessCursor.start ?? source.length
  const end = businessCursor.end ?? start
  const nextText = `${source.slice(0, start)}${token}${source.slice(end)}`
  const cursor = start + token.length
  selectedDraftKey.value = ''
  setBusinessFormulaText(nextText, cursor, cursor)
}

function removeBusinessToken(token) {
  const source = String(form.businessFormula || '')
  const nextText = `${source.slice(0, token.start)}${source.slice(token.end)}`
  selectedDraftKey.value = ''
  setBusinessFormulaText(nextText, token.start, token.start)
}

function removeLastBusinessToken() {
  const last = businessDraftTokens.value.at(-1)
  if (last) {
    removeBusinessToken(last)
  }
}

function clearBusinessFormula() {
  selectedDraftKey.value = ''
  setBusinessFormulaText('', 0, 0)
}

function focusBusinessToken(token) {
  selectedDraftKey.value = token.key
  setBusinessFormulaText(String(form.businessFormula || ''), token.start, token.end)
}

function focusFirstRiskToken() {
  if (businessRiskTokens.value.length) {
    focusBusinessToken(businessRiskTokens.value[0])
  }
}

function scrollToResourceWorkbench() {
  const target = resourceWorkbenchRef.value?.$el || resourceWorkbenchRef.value
  target?.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
}

function resolveDraftTokenTypeLabel(type) {
  const labelMap = {
    variable: '变量',
    fee: '费用',
    function: '函数',
    keyword: '关键字',
    number: '数值',
    operator: '运算符',
    risk: '待识别'
  }
  return labelMap[type] || '片段'
}

function tokenizeBusinessFormula(text, definitions = []) {
  const source = String(text || '')
  if (!source.trim()) {
    return []
  }
  const tokens = []
  let cursor = 0
  let sequence = 0
  while (cursor < source.length) {
    const rest = source.slice(cursor)
    if (/^\s+/.test(rest)) {
      cursor += rest.match(/^\s+/)[0].length
      continue
    }
    const definition = definitions.find(item => item?.value && source.startsWith(item.value, cursor))
    if (definition) {
      const value = definition.value
      tokens.push({
        key: `${definition.type}-${sequence++}-${cursor}`,
        type: definition.type,
        typeLabel: resolveDraftTokenTypeLabel(definition.type),
        label: value,
        code: definition.code,
        description: definition.description,
        start: cursor,
        end: cursor + value.length
      })
      cursor += value.length
      continue
    }
    const numberMatch = rest.match(/^\d+(?:\.\d+)?(?:%|％)?/)
    if (numberMatch) {
      const value = numberMatch[0]
      tokens.push({
        key: `number-${sequence++}-${cursor}`,
        type: 'number',
        typeLabel: resolveDraftTokenTypeLabel('number'),
        label: value,
        start: cursor,
        end: cursor + value.length
      })
      cursor += value.length
      continue
    }
    if (/^[+\-×÷*/%(),，（）]/.test(rest)) {
      const value = rest[0]
      tokens.push({
        key: `operator-${sequence++}-${cursor}`,
        type: 'operator',
        typeLabel: resolveDraftTokenTypeLabel('operator'),
        label: value,
        start: cursor,
        end: cursor + 1
      })
      cursor += 1
      continue
    }
    let end = cursor + 1
    while (end < source.length) {
      const segment = source.slice(end)
      if (/^\s/.test(segment) || /^[+\-×÷*/%(),，（）]/.test(segment) || /^\d/.test(segment)) {
        break
      }
      if (definitions.some(item => item?.value && source.startsWith(item.value, end))) {
        break
      }
      end += 1
    }
    const label = source.slice(cursor, end)
    tokens.push({
      key: `risk-${sequence++}-${cursor}`,
      type: 'risk',
      typeLabel: resolveDraftTokenTypeLabel('risk'),
      label,
      start: cursor,
      end
    })
    cursor = end
  }
  return tokens
}

function calculateSuggestionScore(keyword, item) {
  const normalizedKeyword = String(keyword || '').trim()
  const label = String(item?.label || '')
  if (!normalizedKeyword || !label) {
    return 0
  }
  if (label === normalizedKeyword) {
    return 100
  }
  if (label.includes(normalizedKeyword) || normalizedKeyword.includes(label)) {
    return 80
  }
  const sharedChars = [...new Set(normalizedKeyword.split(''))].filter(char => label.includes(char)).length
  return sharedChars >= Math.min(2, normalizedKeyword.length) ? sharedChars * 10 : 0
}

function getRiskSuggestions(fragment) {
  const keyword = String(fragment || '').trim()
  if (!keyword) {
    return []
  }
  return businessTokenDefinitions.value
    .map(item => ({
      label: item.label,
      value: item.value,
      score: calculateSuggestionScore(keyword, item)
    }))
    .filter(item => item.score >= 20)
    .sort((a, b) => b.score - a.score || a.label.length - b.label.length)
    .slice(0, 3)
}

function replaceRiskFragment(fragment, replacement) {
  const target = businessRiskTokens.value.find(item => item.label === fragment)
  if (!target) {
    return
  }
  const source = String(form.businessFormula || '')
  const nextText = `${source.slice(0, target.start)}${replacement}${source.slice(target.end)}`
  const cursor = target.start + replacement.length
  selectedDraftKey.value = ''
  setBusinessFormulaText(nextText, cursor, cursor)
}

function assignNestedValue(target, path, value) {
  const segments = String(path || '').split('.').map(item => item.trim()).filter(Boolean)
  if (!segments.length) {
    return
  }
  let current = target
  segments.forEach((segment, index) => {
    if (index === segments.length - 1) {
      current[segment] = value
      return
    }
    if (!current[segment] || typeof current[segment] !== 'object') {
      current[segment] = {}
    }
    current = current[segment]
  })
}

function handleAppendResourceToken(payload) {
  appendBusinessToken(payload.value)
}

function applyTemplate(template) {
  workbench.templateCode = template.code
  if (template.pattern === 'BUSINESS') {
    workbench.mode = 'BUSINESS'
    form.businessFormula = template.businessFormula
    form.formulaExpr = businessCompileResult.value.expression
    return
  }
  workbench.mode = 'GUIDED'
  workbench.pattern = template.pattern
  if (template.pattern === 'IF_ELSE') {
    workbench.conditions = (template.conditions || []).map(item => ({ ...item }))
    workbench.trueResultValue = template.trueResultValue || ''
    workbench.falseResultValue = template.falseResultValue || ''
  } else {
    workbench.ranges = (template.ranges || []).map(item => ({ ...item }))
    workbench.defaultResultValue = template.defaultResultValue || ''
  }
}

function applyStoredTemplate(template) {
  form.assetType = 'FORMULA'
  restoreWorkbench({ ...template, templateCode: template.templateCode || template.formulaCode })
  if (workbench.mode === 'BUSINESS') {
    form.businessFormula = template.businessFormula
    form.formulaExpr = template.formulaExpr
  }
}

function handleAddCondition() {
  workbench.conditions.push({ variableCode: '', operatorCode: 'EQ', compareValue: '' })
}

function handleRemoveCondition(index) {
  workbench.conditions.splice(index, 1)
  if (!workbench.conditions.length) {
    handleAddCondition()
  }
}

function handleConditionVariableChange(row, value) {
  row.variableCode = value
}

function handleAddRange() {
  workbench.ranges.push({ startValue: '', endValue: '', resultValue: '' })
}

function handleRemoveRange(index) {
  workbench.ranges.splice(index, 1)
  if (!workbench.ranges.length) {
    handleAddRange()
  }
}

function buildPayload() {
  const payload = {
    ...form,
    businessFormula: activeBusinessFormula.value || form.businessFormula,
    formulaExpr: activeFormulaExpression.value || form.formulaExpr,
    workbenchMode: workbench.mode,
    workbenchPattern: workbench.pattern,
    templateCode: workbench.templateCode,
    workbenchConfigJson: JSON.stringify(buildWorkbenchPayload()),
    testCaseJson: testInputJson.value || undefined
  }
  return payload
}

async function handleSave() {
  await proxy.$refs.formulaRef.validate()
  const payload = buildPayload()
  if (!payload.formulaExpr) {
    proxy.$modal.msgError('请先通过业务编排或结构助手生成标准表达式')
    return
  }
  if (!formulaValidationResult.value.valid) {
    proxy.$modal.msgError(formulaValidationResult.value.issues[0].message)
    return
  }
  if (payload.formulaId) {
    await updateFormula(payload)
  } else {
    await addFormula(payload)
  }
  proxy.$modal.msgSuccess(payload.formulaId ? '公式修改成功' : '公式新增成功')
  await getList()
  await loadSceneAssets(payload.sceneId)
}

async function handleTest() {
  const payload = buildPayload()
  if (!payload.formulaExpr) {
    proxy.$modal.msgError('当前没有可测试的公式表达式')
    return
  }
  if (!formulaValidationResult.value.valid) {
    proxy.$modal.msgError(formulaValidationResult.value.issues[0].message)
    return
  }
  const response = await testFormula({
    sceneId: payload.sceneId,
    formulaExpr: payload.formulaExpr,
    formulaCode: payload.formulaCode,
    inputJson: testInputJson.value
  })
  testResult.value = response?.data?.result
}

async function handleCreate() {
  resetFormModel()
  if (form.sceneId) {
    queryParams.sceneId = form.sceneId
    await loadSceneAssets(form.sceneId)
  }
}

async function handleEdit(row) {
  const response = await getFormula(row.formulaId)
  Object.assign(form, response.data || {})
  queryParams.sceneId = form.sceneId
  testInputJson.value = form.testCaseJson || ''
  testResult.value = response.data?.sampleResultJson ? safeJsonParse(response.data.sampleResultJson) : undefined
  resourceKeyword.value = ''
  selectedDraftKey.value = ''
  await loadSceneAssets(form.sceneId)
  restoreWorkbench(response.data)
}

async function handleLoadFormula(row) {
  if (!row?.formulaId) {
    return
  }
  await handleEdit(row)
}

async function handleGovernance(row) {
  const response = await getFormulaGovernance(row.formulaId)
  governanceInfo.value = response?.data || {}
  governanceOpen.value = true
}

async function handleOpenVersions(row) {
  versionLoading.value = true
  versionFormulaTitle.value = `${row.formulaCode} / ${row.formulaName}`
  versionOpen.value = true
  try {
    const response = await listFormulaVersions(row.formulaId)
    versionList.value = response?.data || []
  } finally {
    versionLoading.value = false
  }
}

async function handleLoadVersion(row) {
  const response = await getFormulaVersion(row.versionId)
  Object.assign(form, response.data || {})
  queryParams.sceneId = form.sceneId
  resourceKeyword.value = ''
  selectedDraftKey.value = ''
  await loadSceneAssets(form.sceneId)
  restoreWorkbench(response.data)
  testInputJson.value = form.testCaseJson || ''
  testResult.value = undefined
  versionOpen.value = false
  proxy.$modal.msgSuccess(`已装载版本 V${row.versionNo}`)
}

async function handleRollbackVersion(row) {
  await ElMessageBox.confirm(`确认将当前公式回滚为版本 V${row.versionNo} 吗？`, '版本回滚确认', { type: 'warning' })
  await rollbackFormulaVersion(row.versionId)
  const [formulaResponse, versionResponse] = await Promise.all([
    getFormula(row.formulaId),
    listFormulaVersions(row.formulaId),
    getList()
  ])
  Object.assign(form, formulaResponse.data || {})
  queryParams.sceneId = form.sceneId
  await loadSceneAssets(form.sceneId)
  restoreWorkbench(formulaResponse.data)
  testInputJson.value = form.testCaseJson || ''
  testResult.value = undefined
  versionList.value = versionResponse?.data || []
  proxy.$modal.msgSuccess(`已回滚为版本 V${row.versionNo}`)
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除公式 ${row.formulaCode} 吗？`, '删除确认', { type: 'warning' })
  await delFormula(row.formulaId)
  proxy.$modal.msgSuccess('删除成功')
  await getList()
  await loadSceneAssets(form.sceneId)
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.formulaId)
}

async function handleGenerateSample() {
  if (!form.sceneId) {
    proxy.$modal.msgWarning('请先选择场景，再按引用生成示例')
    return
  }
  const V = {}
  const I = {}
  const F = {}
  const variableRefCodes = formulaValidationResult.value.variableRefs?.length
    ? formulaValidationResult.value.variableRefs
    : variableOptions.value.map(item => item.variableCode)
  variableRefCodes.forEach(code => {
    const item = variableMetaMap.value[code]
    if (!item) {
      return
    }
    const type = String(item.dataType || '').toUpperCase()
    let sampleValue
    if (['NUMBER', 'INTEGER', 'DECIMAL', 'LONG'].includes(type)) {
      sampleValue = 1
    } else if (type === 'BOOLEAN') {
      sampleValue = true
    } else {
      sampleValue = item.variableName || item.variableCode
    }
    V[item.variableCode] = sampleValue
    if (item.dataPath) {
      assignNestedValue(I, item.dataPath, sampleValue)
    } else {
      I[item.variableCode] = sampleValue
    }
  })
  ;(formulaValidationResult.value.feeRefs || []).forEach(code => {
    F[code] = 100
  })
  testInputJson.value = JSON.stringify({ V, C: {}, I, F, T: {} }, null, 2)
}

async function handleWorkbenchSceneChange(sceneId) {
  const workingSceneId = sceneId || resolveWorkingCostSceneId(sceneOptions.value)
  form.sceneId = workingSceneId
  queryParams.sceneId = workingSceneId
  resourceKeyword.value = ''
  selectedDraftKey.value = ''
  await loadSceneAssets(workingSceneId)
}

async function handleQuerySceneChange(sceneId) {
  const workingSceneId = sceneId || resolveWorkingCostSceneId(sceneOptions.value)
  queryParams.sceneId = workingSceneId
  form.sceneId = workingSceneId
  resourceKeyword.value = ''
  selectedDraftKey.value = ''
  await loadSceneAssets(workingSceneId)
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.pageNum = 1
  queryParams.pageSize = 10
  handleQuery()
}

function resolveRouteQueryValue(key) {
  const value = route.query?.[key]
  return Array.isArray(value) ? value[0] : value
}

function safeJsonParse(text) {
  if (!text) {
    return undefined
  }
  try {
    return typeof text === 'string' ? JSON.parse(text) : text
  } catch (error) {
    return text
  }
}

function buildWorkbenchPayload() {
  return {
    mode: workbench.mode,
    pattern: workbench.pattern,
    templateCode: workbench.templateCode,
    conditionLogic: workbench.conditionLogic,
    conditions: (workbench.conditions || []).map(item => ({
      variableCode: item.variableCode || '',
      operatorCode: item.operatorCode || 'EQ',
      compareValue: item.compareValue || ''
    })),
    trueResultValue: workbench.trueResultValue || '',
    falseResultValue: workbench.falseResultValue || '',
    rangeVariableCode: workbench.rangeVariableCode,
    ranges: (workbench.ranges || []).map(item => ({
      startValue: item.startValue || '',
      endValue: item.endValue || '',
      resultValue: item.resultValue || ''
    })),
    defaultResultValue: workbench.defaultResultValue || '',
    businessFormula: activeBusinessFormula.value || form.businessFormula || '',
    formulaExpr: activeFormulaExpression.value || form.formulaExpr || ''
  }
}

function restoreWorkbench(data = {}) {
  resetWorkbench()
  const config = safeJsonParse(data.workbenchConfigJson)
  if (!config || typeof config !== 'object') {
    workbench.mode = normalizeWorkbenchMode(data.workbenchMode)
    workbench.pattern = data.workbenchPattern || 'IF_ELSE'
    workbench.templateCode = data.templateCode
    if (workbench.mode !== 'GUIDED') {
      workbench.mode = 'BUSINESS'
    }
    return
  }
  workbench.mode = normalizeWorkbenchMode(config.mode || data.workbenchMode)
  workbench.pattern = config.pattern || data.workbenchPattern || 'IF_ELSE'
  workbench.templateCode = config.templateCode || data.templateCode
  workbench.conditionLogic = config.conditionLogic || 'AND'
  workbench.conditions = Array.isArray(config.conditions) && config.conditions.length
    ? config.conditions.map(item => ({
      variableCode: item.variableCode || '',
      operatorCode: item.operatorCode || 'EQ',
      compareValue: item.compareValue || ''
    }))
    : [{ variableCode: '', operatorCode: 'EQ', compareValue: '' }]
  workbench.trueResultValue = config.trueResultValue || ''
  workbench.falseResultValue = config.falseResultValue || ''
  workbench.rangeVariableCode = config.rangeVariableCode
  workbench.ranges = Array.isArray(config.ranges) && config.ranges.length
    ? config.ranges.map(item => ({
      startValue: item.startValue || '',
      endValue: item.endValue || '',
      resultValue: item.resultValue || ''
    }))
    : [{ startValue: '', endValue: '', resultValue: '' }]
  workbench.defaultResultValue = config.defaultResultValue || ''
  if (workbench.mode === 'BUSINESS') {
    form.businessFormula = config.businessFormula || data.businessFormula
    form.formulaExpr = config.formulaExpr || data.formulaExpr
  }
}

async function initializePageState() {
  await loadBaseOptions()
  resetFormModel()
  const routeSceneId = resolveRouteQueryValue('sceneId')
  const routeFormulaCode = resolveRouteQueryValue('formulaCode')
  if (routeSceneId) {
    queryParams.sceneId = routeSceneId
  }
  if (routeFormulaCode) {
    queryParams.formulaCode = routeFormulaCode
  }
  if (queryParams.sceneId) {
    form.sceneId = queryParams.sceneId
    await loadSceneAssets(queryParams.sceneId)
    const matchedFormula = routeFormulaCode
      ? formulaOptionList.value.find(item => item.formulaCode === routeFormulaCode)
      : undefined
    if (matchedFormula?.formulaId) {
      await handleLoadFormula(matchedFormula)
    }
  }
  await getList()
}

onMounted(async () => {
  await initializePageState()
})

onActivated(async () => {
  await initializePageState()
})
</script>

<style lang="scss" scoped>
.formula-lab {
  display: flex;
  flex-direction: column;
  gap: 20px;
  color: var(--el-text-color-primary);
}

.formula-lab__hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 28px 32px;
  border-radius: 24px;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
}

.formula-lab__eyebrow {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-color-warning);
  margin-bottom: 8px;
}

.formula-lab__title {
  margin: 0 0 10px;
  font-size: 40px;
  line-height: 1.1;
  color: var(--el-text-color-primary);
}

.formula-lab__subtitle {
  margin: 0;
  max-width: 880px;
  color: var(--el-text-color-regular);
  line-height: 1.8;
}

.formula-lab__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.formula-lab__metric-card,
.formula-lab__builder,
.formula-lab__toolbox,
.formula-lab__ledger {
  border-radius: 24px;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
  box-shadow: var(--el-box-shadow-light);
}

.formula-lab__metric-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 20px 22px;
}

.formula-lab__metric-card span,
.formula-lab__metric-card small {
  color: var(--el-text-color-secondary);
}

.formula-lab__metric-card strong {
  font-size: 36px;
  line-height: 1;
  color: var(--el-color-primary);
}

.formula-lab__workspace {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(320px, 0.9fr);
  gap: 20px;
}

.formula-lab__builder,
.formula-lab__toolbox,
.formula-lab__ledger {
  padding: 24px;
}

.formula-lab__panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.formula-lab__panel-head h3 {
  margin: 0 0 6px;
  font-size: 24px;
  color: var(--el-text-color-primary);
}

.formula-lab__panel-head p {
  margin: 0;
  color: var(--el-text-color-regular);
}

.formula-lab__drawer-head {
  margin-bottom: 16px;
}

.formula-lab__drawer-head strong {
  display: block;
  margin-bottom: 6px;
  color: var(--el-text-color-primary);
}

.formula-lab__drawer-head p {
  margin: 0;
  color: var(--el-text-color-regular);
}

.formula-lab__toolbar,
.formula-lab__pattern-bar,
.formula-lab__result-grid,
.formula-lab__test-result {
  display: flex;
  gap: 12px;
}

.formula-lab__toolbar {
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.formula-lab__preview-card,
.formula-lab__test-card {
  margin-bottom: 16px;
  padding: 18px 20px;
  border-radius: 18px;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
}

.formula-lab__preview-card--code {
  background: var(--el-fill-color-dark);
  border-color: var(--el-fill-color-darker);
}

.formula-lab__preview-title {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.formula-lab__preview-card--code .formula-lab__preview-title {
  color: var(--el-color-white);
}

.formula-lab__preview-text {
  min-height: 54px;
  color: var(--el-text-color-primary);
  line-height: 1.8;
}

.formula-lab__code {
  margin: 0;
  min-height: 132px;
  color: var(--el-color-white);
  font-family: 'JetBrains Mono', 'Consolas', monospace;
  white-space: pre-wrap;
  word-break: break-word;
}

.formula-lab__validation-item {
  line-height: 1.7;
}

.formula-lab__business,
.formula-lab__guided {
  margin-bottom: 18px;
}

.formula-lab__editor-card,
.formula-lab__draft-card,
.formula-lab__quick-card {
  margin-bottom: 16px;
  padding: 18px 20px;
  border-radius: 18px;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
}

.formula-lab__note-card {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 14px;
  background: var(--el-bg-color-overlay);
  border: 1px dashed var(--el-border-color);
}

.formula-lab__note-card strong {
  color: var(--el-text-color-primary);
}

.formula-lab__note-card span {
  color: var(--el-text-color-regular);
  line-height: 1.7;
}

.formula-lab__draft-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.formula-lab__draft-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.formula-lab__draft-token {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid var(--el-border-color);
  background: var(--el-bg-color-overlay);
  color: var(--el-text-color-regular);
  cursor: pointer;
  transition: all 0.2s ease;
}

.formula-lab__draft-token:hover,
.formula-lab__draft-token.is-active {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px rgb(64 158 255 / 12%);
}

.formula-lab__draft-token em {
  color: var(--el-text-color-secondary);
  font-style: normal;
  font-size: 12px;
}

.formula-lab__draft-token i {
  color: var(--el-text-color-placeholder);
  font-style: normal;
}

.formula-lab__draft-token.is-variable {
  border-color: var(--el-color-success-light-5);
}

.formula-lab__draft-token.is-fee {
  border-color: var(--el-color-warning-light-5);
}

.formula-lab__draft-token.is-function {
  border-color: var(--el-color-primary-light-5);
}

.formula-lab__draft-token.is-risk {
  border-style: dashed;
  border-color: var(--el-color-danger-light-5);
  background: var(--el-color-danger-light-9);
}

.formula-lab__quick-grid {
  display: grid;
  gap: 16px;
}

.formula-lab__note-list,
.formula-lab__issue-list,
.formula-lab__reference-list {
  display: grid;
  gap: 12px;
}

.formula-lab__note-item,
.formula-lab__issue-item,
.formula-lab__reference-item {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 16px;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color);
}

.formula-lab__note-item strong,
.formula-lab__issue-item strong,
.formula-lab__reference-item strong {
  color: var(--el-text-color-primary);
}

.formula-lab__note-item span,
.formula-lab__issue-item p,
.formula-lab__issue-item span,
.formula-lab__reference-item span,
.formula-lab__reference-item small {
  margin: 0;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}

.formula-lab__reference-item small {
  color: var(--el-text-color-secondary);
}

.formula-lab__issue-item {
  border-color: var(--el-color-warning-light-5);
}

.formula-lab__issue-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
}

.formula-lab__section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.formula-lab__result-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 14px;
}

.formula-lab__field-label,
.formula-lab__tool-title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.formula-lab__test-card {
  margin-bottom: 0;
}

.formula-lab__test-result {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 14px;
  background: var(--el-bg-color-overlay);
  border: 1px dashed var(--el-border-color);
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
}

.formula-lab__toolbox {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.formula-lab__tool-section {
  padding: 16px 18px;
  border-radius: 18px;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
}

.formula-lab__chip-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.formula-lab__chip,
.formula-lab__list-item {
  appearance: none;
  border: 1px solid var(--el-border-color);
  background: var(--el-bg-color-overlay);
  cursor: pointer;
  transition: all 0.2s ease;
}

.formula-lab__chip {
  padding: 8px 12px;
  border-radius: 999px;
  color: var(--el-text-color-regular);
}

.formula-lab__chip:hover,
.formula-lab__list-item:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
  transform: translateY(-1px);
}

.formula-lab__list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 220px;
  overflow: auto;
}

.formula-lab__list-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  width: 100%;
  padding: 12px 14px;
  border-radius: 16px;
  text-align: left;
}

.formula-lab__list-item strong {
  color: var(--el-text-color-primary);
}

.formula-lab__list-item span {
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.formula-lab :deep(.el-empty__description p) {
  color: var(--el-text-color-secondary);
}

.formula-lab :deep(.el-alert) {
  border-radius: 16px;
}

.mt12 {
  margin-top: 12px;
}

@media (max-width: 1280px) {
  .formula-lab__metrics,
  .formula-lab__workspace {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .formula-lab__hero {
    flex-direction: column;
    padding: 22px 20px;
  }

  .formula-lab__title {
    font-size: 32px;
  }

  .formula-lab__builder,
  .formula-lab__toolbox,
  .formula-lab__ledger {
    padding: 18px;
  }

  .formula-lab__result-grid {
    grid-template-columns: 1fr;
  }

  .formula-lab__draft-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
