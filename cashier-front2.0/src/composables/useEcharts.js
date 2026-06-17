/**
 * ECharts 动态导入组合式函数
 *
 * 仅在用户访问图表页面时按需加载 ~1MB 的 echarts 模块，
 * 减少首屏 JS 体积。
 *
 * 用法：
 *   const { getEcharts } = useEcharts()
 *   const echarts = await getEcharts()
 *   const chart = echarts.init(dom)
 */
let echartsModule = null

export function useEcharts() {
  const getEcharts = async () => {
    if (!echartsModule) echartsModule = await import('echarts')
    return echartsModule
  }

  return { getEcharts }
}
