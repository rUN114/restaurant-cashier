/**
 * 通用的 ID→名称映射工厂函数
 *
 * 替代在多页面重复的 (id) => list.value.find(item => item.id === id)?.name || '-'
 *
 * @param {import('vue').Ref<Array>} listRef — 数据列表的 ref
 * @param {string} key — 匹配的键名，默认 'id'
 * @param {string} labelKey — 取值的键名，默认 'name'
 * @param {string} fallback — 未匹配时的占位文本，默认 '-'
 * @returns {(id: any) => string}
 */
export function createLookup(listRef, key = 'id', labelKey = 'name', fallback = '-') {
  return (id) => {
    if (id == null) return fallback
    const item = listRef.value.find((i) => i[key] === id)
    return item ? (item[labelKey] ?? fallback) : fallback
  }
}
