/**
 * 图表统一配色主题
 *
 * 从 CSS 变量实时读取颜色，浅/深色模式自动适配。
 * 替换各图表页硬编码的 #409eff / 彩虹色板 / #bbb 空态灰，
 * 让 ECharts 视觉与品牌（华农绿）和主题保持一致。
 *
 * 用法：
 *   import { getChartTheme } from '@/utils/chartTheme'
 *   const t = getChartTheme()
 *   series: [{ itemStyle: { color: t.primary }, ... }]
 */

function cssVar(name, fallback = '') {
  if (typeof window === 'undefined' || !window.getComputedStyle) return fallback
  const v = window.getComputedStyle(document.documentElement).getPropertyValue(name)
  return v ? v.trim() : fallback
}

export function getChartTheme() {
  const primary = cssVar('--color-primary', '#1a7a4e')
  const accent = cssVar('--color-accent', '#238a5f')
  const gold = cssVar('--color-gold', '#c9a45c')
  const goldLight = cssVar('--color-gold-light', '#d9b877')
  const textSecondary = cssVar('--text-secondary', '#5a5d6e')
  const textTertiary = cssVar('--text-tertiary', '#9ea0ad')
  const borderColor = cssVar('--border-color', '#e4e6ed')

  // 品牌绿系渐进色板 + 琥珀金点缀（替代通用彩虹板）。
  // 末位紫罗兰用于"女"等需要与绿强区分的对比项，对红绿色盲友好。
  const palette = [primary, '#2fb984', '#57c493', '#8fd3b0', gold, '#b37feb']

  return {
    primary,
    accent,
    gold,
    goldLight,
    textSecondary,
    textTertiary,
    borderColor,
    emptyText: textTertiary,   // 图表空态文字
    axisLine: borderColor,     // 坐标轴线
    palette,
    maleColor: primary,
    femaleColor: '#b37feb',
  }
}
