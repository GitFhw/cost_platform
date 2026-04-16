import defaultSettings from '@/settings'
import { useDark, useToggle } from '@vueuse/core'
import { useDynamicTitle } from '@/utils/dynamicTitle'

const isDark = useDark()
const toggleDark = useToggle(isDark)

const { sideTheme, showSettings, navType, tagsView, tagsViewPersist, tagsIcon, fixedHeader, sidebarLogo, dynamicTitle, footerVisible, footerContent, costPageMode } = defaultSettings

const storageSetting = JSON.parse(localStorage.getItem('layout-setting')) || ''

function writeLayoutSetting(payload = {}) {
  const current = JSON.parse(localStorage.getItem('layout-setting')) || {}
  localStorage.setItem('layout-setting', JSON.stringify({
    ...current,
    ...payload
  }))
}

const useSettingsStore = defineStore(
  'settings',
  {
    state: () => ({
      title: '',
      theme: storageSetting.theme || '#409EFF',
      sideTheme: storageSetting.sideTheme || sideTheme,
      showSettings: showSettings,
      navType: storageSetting.navType === undefined ? navType : storageSetting.navType,
      tagsView: storageSetting.tagsView === undefined ? tagsView : storageSetting.tagsView,
      tagsViewPersist: storageSetting.tagsViewPersist === undefined ? tagsViewPersist : storageSetting.tagsViewPersist,
      tagsIcon: storageSetting.tagsIcon === undefined ? tagsIcon : storageSetting.tagsIcon,
      fixedHeader: storageSetting.fixedHeader === undefined ? fixedHeader : storageSetting.fixedHeader,
      sidebarLogo: storageSetting.sidebarLogo === undefined ? sidebarLogo : storageSetting.sidebarLogo,
      dynamicTitle: storageSetting.dynamicTitle === undefined ? dynamicTitle : storageSetting.dynamicTitle,
      footerVisible: storageSetting.footerVisible === undefined ? footerVisible : storageSetting.footerVisible,
      costPageMode: storageSetting.costPageMode === undefined ? costPageMode : storageSetting.costPageMode,
      footerContent: footerContent,
      isDark: isDark.value
    }),
    actions: {
      // 修改布局设置
      changeSetting(data) {
        const { key, value } = data
        if (this.hasOwnProperty(key)) {
          this[key] = value
        }
      },
      // 设置网页标题
      setTitle(title) {
        this.title = title
        useDynamicTitle()
      },
      setCostPageMode(mode) {
        const nextMode = mode === 'COMPACT' ? 'COMPACT' : 'STANDARD'
        this.costPageMode = nextMode
        writeLayoutSetting({ costPageMode: nextMode })
      },
      toggleCostPageMode() {
        this.setCostPageMode(this.costPageMode === 'COMPACT' ? 'STANDARD' : 'COMPACT')
      },
      // 切换暗黑模式
      toggleTheme() {
        this.isDark = !this.isDark
        toggleDark()
      }
    }
  })

export default useSettingsStore