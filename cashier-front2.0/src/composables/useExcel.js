import * as XLSX from 'xlsx'

/**
 * 通用表格导出 Excel
 * @param {Array} data - 数据数组
 * @param {Array} columns - [{ label: '表头', key: '字段名' }]
 * @param {string} filename - 文件名（不含扩展名）
 */
export function exportExcel(data, columns, filename = 'export') {
  const rows = data.map(row => {
    const obj = {}
    columns.forEach(col => {
      obj[col.label] = row[col.key] ?? ''
    })
    return obj
  })

  const ws = XLSX.utils.json_to_sheet(rows)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, 'Sheet1')
  XLSX.writeFile(wb, `${filename}.xlsx`)
}

export function useExcel() {
  return { exportExcel }
}
