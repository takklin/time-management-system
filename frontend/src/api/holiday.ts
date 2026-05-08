import axios from 'axios'
import { solar2lunar, getSolarTermName } from '@/utils/lunar'

export interface HolidayInfo {
  date: string
  isHoliday?: boolean
  holidayName?: string | null
  lunarText?: string | null
}

const fallbackSolarMap: Record<string, string> = {
  '01-01': '元旦',
  '05-01': '劳动节',
  '10-01': '国庆节',
  '12-25': '圣诞节',
  '02-14': '情人节'
}

// 仅提供常用公历节假日的本地回退映射
const fallbackLunarMap: Record<string, string> = {
  '1-1': '春节',
  '5-5': '端午节',
  '8-15': '中秋节',
  '7-7': '七夕'
}

/**
 * 获取指定日期的节假日与农历信息。
 * 优先尝试调用第三方 API（示例：apihubs.cn），失败后使用内置回退映射并尝试本地农历计算。
 */
export async function getHolidayForDate(date: string): Promise<HolidayInfo> {
  // date 格式: YYYY-MM-DD
  try {
    const resp = await axios.get('https://api.apihubs.cn/holiday/get', { params: { date }, timeout: 5000 })
    const data = resp.data

    let holidayName: string | null = null
    let isHoliday = false
    let lunarText: string | null = null

    if (data) {
      // 如果返回了 code 字段且不是成功码（0），当作错误处理，走 catch 分支
      if (typeof (data as any).code !== 'undefined' && (data as any).code !== 0) {
        throw new Error((data as any).msg || 'API error')
      }
      // 兼容常见响应结构
      const d = data.data || data
      if (d) {
        if (d.holiday) {
          holidayName = d.holiday.name || d.holiday || null
          isHoliday = Boolean(d.holiday.isHoliday || d.holiday.is_holiday) || !!holidayName
        }
        if (!holidayName && d.name) {
          holidayName = d.name
          isHoliday = true
        }
        // 有些接口会返回 lunar 字段或节气（jieqi）等
        if (d.lunar) lunarText = d.lunar
        if (!lunarText && d.lunarText) lunarText = d.lunarText
        if (!lunarText && (d.jieqi || d.jieqiName || d.solarTerm || d.term)) lunarText = d.jieqi || d.jieqiName || d.solarTerm || d.term
      }
    }

    // 本地回退（公历常见节日）
    if (!holidayName) {
      const mmdd = date.slice(5)
      if (fallbackSolarMap[mmdd]) {
        holidayName = fallbackSolarMap[mmdd]
        isHoliday = true
      }
    }

    // 补充农历文本与农历节日（本地计算），节气优先显示
    try {
      const [y, m, d] = date.split('-').map(Number)
      const lunar = solar2lunar(y, m, d)
      const term = getSolarTermName(y, m, d)
      if (!lunarText) lunarText = term || (lunar && lunar.lunar) || null
      if (!holidayName && lunar) {
        const lmKey = `${lunar.lMonth}-${lunar.lDay}`
        if (fallbackLunarMap[lmKey]) {
          holidayName = fallbackLunarMap[lmKey]
          isHoliday = true
        }
      }
    } catch (e) { /* ignore */ }

    return { date, isHoliday, holidayName, lunarText }
  } catch (err) {
    // 第三方 API 不可用时，仅使用内置回退并本地计算农历
    const mmdd = date.slice(5)
    const holidayName = fallbackSolarMap[mmdd] || null
    let lunarTextLocal: string | null = null
    try {
      const [y, m, d] = date.split('-').map(Number)
      const lunar = solar2lunar(y, m, d)
      if (lunar) lunarTextLocal = lunar.lunar
    } catch (e) {}
    return { date, isHoliday: !!holidayName, holidayName, lunarText: lunarTextLocal }
  }
}

function sleep(ms: number) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/**
 * 按月批量获取节假日信息并缓存到 localStorage，避免客户端短时间内并发请求导致限流。
 * 返回一个以 YYYY-MM-DD 为键的对象。
 */
export async function getHolidaysForMonth(year: number, month: number): Promise<Record<string, HolidayInfo>> {
  const key = `holiday_month_${year}-${String(month).padStart(2,'0')}`
  console.debug('[Holiday] getHolidaysForMonth start', key)
  const cached = localStorage.getItem(key)
  if (cached) {
    try {
      const parsed = JSON.parse(cached)
      const age = Date.now() - (parsed.ts || 0)
      const TTL = 1000 * 60 * 60 * 24 // 24h
      const existingMap = parsed.map || parsed
      if (existingMap && age < TTL) {
        // 补充可能缺失的 lunarText 与基于农历的节日回退（向后兼容旧缓存格式）
        try {
          Object.keys(existingMap).forEach(k => {
            const rec = existingMap[k]
            if (!rec.lunarText) {
              try {
                const [y, m, d] = k.split('-').map(Number)
                const lunar = solar2lunar(y, m, d)
                if (lunar) rec.lunarText = lunar.lunar
              } catch (e) {}
            }
            if (!rec.holidayName) {
              try {
                const [y, m, d] = k.split('-').map(Number)
                const lunar = solar2lunar(y, m, d)
                if (lunar) {
                  const lmKey = `${lunar.lMonth}-${lunar.lDay}`
                  if (fallbackLunarMap[lmKey]) {
                    rec.holidayName = fallbackLunarMap[lmKey]
                    rec.isHoliday = true
                  }
                }
              } catch (e) {}
            }
          })
          try { localStorage.setItem(key, JSON.stringify({ ts: parsed.ts || Date.now(), map: existingMap })) } catch (e) { /* ignore */ }
        } catch (e) { /* ignore */ }
        return existingMap
      }
    } catch (e) { /* ignore */ }
  }

  const days = new Date(year, month, 0).getDate()
  const result: Record<string, HolidayInfo> = {}

  for (let d = 1; d <= days; d++) {
    const date = `${year}-${String(month).padStart(2,'0')}-${String(d).padStart(2,'0')}`
    try {
      const resp = await axios.get('https://api.apihubs.cn/holiday/get', { params: { date }, timeout: 5000 })
      const data = resp.data
      console.debug('[Holiday] api response', date, data)

      // 如果接口返回 code 并非 0，可能是限流或错误，停止批量调用以保护自己，使用本地回退填充剩余日期
      if (typeof (data as any).code !== 'undefined' && (data as any).code !== 0) {
        console.warn('Holiday API returned non-zero code for', date, (data as any).code, (data as any).msg)
        // 标记为限流并跳出，下面会尝试备用源
        result.__limited__ = true as any
        break
      }

      const dObj = data.data || data
      let holidayName: string | null = null
      let isHoliday = false
      let lunarText: string | null = null
      if (dObj) {
        if (dObj.holiday) {
          holidayName = dObj.holiday.name || dObj.holiday || null
          isHoliday = Boolean(dObj.holiday.isHoliday || dObj.holiday.is_holiday) || !!holidayName
        }
        if (!holidayName && dObj.name) {
          holidayName = dObj.name
          isHoliday = true
        }
        if (dObj.lunar) lunarText = dObj.lunar
        if (!lunarText && dObj.lunarText) lunarText = dObj.lunarText
        if (!lunarText && (dObj.jieqi || dObj.jieqiName || dObj.solarTerm || dObj.term)) lunarText = dObj.jieqi || dObj.jieqiName || dObj.solarTerm || dObj.term
      }

      // 本地回退（公历）
      if (!holidayName) {
        const mmdd = date.slice(5)
        if (fallbackSolarMap[mmdd]) {
          holidayName = fallbackSolarMap[mmdd]
          isHoliday = true
        }
      }

      result[date] = { date, isHoliday, holidayName, lunarText }
    } catch (err) {
      console.warn('Failed fetch holiday for', date, err)
      // 在请求失败时继续，但不要太快以减低限流风险
    }

    // 小延迟，避免短时间内并发请求触发限流
    await sleep(700)
  }

  // 如果批量调用中断（可能因为限流），先尝试备用年级接口，再对尚未填充的日期使用内置回退映射
  if ((result as any).__limited__) {
    try {
      console.debug('[Holiday] limited, trying fallback year API', year)
      const fallbackYearResp = await axios.get(`https://timor.tech/api/holiday/year/${year}`, { timeout: 5000 })
      const fy = fallbackYearResp.data
      console.debug('[Holiday] fallback year response', fy)
      const holidaysMap = (fy && (fy.holiday || fy.data || fy)) as Record<string, string> | undefined
      if (holidaysMap) {
        Object.keys(holidaysMap).forEach(k => {
          if (!result[k]) {
            result[k] = { date: k, isHoliday: !!holidaysMap[k], holidayName: holidaysMap[k] || null, lunarText: null }
          }
        })
      }
      // timor.tech 及类似接口可能提供节气映射
      if (fy && fy.jieqi) {
        const jieqiMap = fy.jieqi as Record<string, string>
        Object.keys(jieqiMap).forEach(k => {
          if (!result[k]) result[k] = { date: k, isHoliday: false, holidayName: null, lunarText: jieqiMap[k] }
          else if (!result[k].lunarText) result[k].lunarText = jieqiMap[k]
        })
      }
    } catch (e) {
      console.warn('Fallback year API failed', e)
    }
    delete (result as any).__limited__
  }

  // 对仍未填充的日期使用内置回退映射
  for (let d = 1; d <= days; d++) {
    const date = `${year}-${String(month).padStart(2,'0')}-${String(d).padStart(2,'0')}`
    if (!result[date]) {
      const mmdd = date.slice(5)
      const holidayName = fallbackSolarMap[mmdd] || null
      result[date] = { date, isHoliday: !!holidayName, holidayName, lunarText: null }
    }
  }

  // 补充本地农历文本以及基于农历的节日回退
  try {
    Object.keys(result).forEach(k => {
      const rec = result[k]
      if (!rec.lunarText) {
        try {
          const [y, m, d] = k.split('-').map(Number)
          const lunar = solar2lunar(y, m, d)
          const term = getSolarTermName(y, m, d)
          if (term) {
            rec.lunarText = term
          } else if (lunar) rec.lunarText = lunar.lunar
        } catch (e) {}
      }
      if (!rec.holidayName) {
        try {
          const [y, m, d] = k.split('-').map(Number)
          const lunar = solar2lunar(y, m, d)
          if (lunar) {
            const lmKey = `${lunar.lMonth}-${lunar.lDay}`
            if (fallbackLunarMap[lmKey]) {
              rec.holidayName = fallbackLunarMap[lmKey]
              rec.isHoliday = true
            }
          }
        } catch (e) {}
      }
    })
  } catch (e) { /* ignore */ }

  try { localStorage.setItem(key, JSON.stringify({ ts: Date.now(), map: result })) } catch (e) { /* ignore */ }
  return result
}
