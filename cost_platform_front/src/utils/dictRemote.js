import { getDicts } from '@/api/system/dict/data'

export function normalizeDictOptions(rows = []) {
  return rows.map(item => ({
    label: item.dictLabel,
    value: item.dictValue,
    elTagType: item.listClass,
    elTagClass: item.cssClass
  }))
}

export async function getRemoteDictOptions(dictType) {
  try {
    const response = await getDicts(dictType)
    return normalizeDictOptions(response?.data || [])
  } catch (error) {
    return []
  }
}

export async function getRemoteDictOptionMap(dictTypes = []) {
  const uniqueTypes = [...new Set(dictTypes.filter(Boolean))]
  const responses = await Promise.allSettled(uniqueTypes.map(dictType => getDicts(dictType)))
  return uniqueTypes.reduce((result, dictType, index) => {
    if (responses[index]?.status === 'fulfilled') {
      result[dictType] = normalizeDictOptions(responses[index].value?.data || [])
    } else {
      result[dictType] = []
    }
    return result
  }, {})
}
