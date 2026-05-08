// 简易的阳历->农历转换工具（适用于 1900-2099 年范围）
// 数据与算法来源为常见开源实现并作了精简，提供基础的农历年月日与中文表示。

const lunarInfo: number[] = [
  0x04bd8,0x04ae0,0x0a570,0x054d5,0x0d260,0x0d950,0x16554,0x056a0,0x09ad0,0x055d2,
  0x04ae0,0x0a5b6,0x0a4d0,0x0d250,0x1d255,0x0b540,0x0d6a0,0x0ada2,0x095b0,0x14977,
  0x04970,0x0a4b0,0x0b4b5,0x06a50,0x06d40,0x1ab54,0x02b60,0x09570,0x052f2,0x04970,
  0x06566,0x0d4a0,0x0ea50,0x06e95,0x05ad0,0x02b60,0x186e3,0x092e0,0x1c8d7,0x0c950,
  0x0d4a0,0x1d8a6,0x0b550,0x056a0,0x1a5b4,0x025d0,0x092d0,0x0d2b2,0x0a950,0x0b557,
  0x06ca0,0x0b550,0x15355,0x04da0,0x0a5b0,0x14573,0x052b0,0x0a9a8,0x0e950,0x06aa0,
  0x0aea6,0x0ab50,0x04b60,0x0aae4,0x0a570,0x05260,0x0f263,0x0d950,0x05b57,0x056a0,
  0x096d0,0x04dd5,0x04ad0,0x0a4d0,0x0d4d4,0x0d250,0x0d558,0x0b540,0x0b5a0,0x195a6,
  0x095b0,0x049b0,0x0a974,0x0a4b0,0x0b27a,0x06a50,0x06d40,0x0af46,0x0ab60,0x09570,
  0x04afb,0x04970,0x064b0,0x074a3,0x0ea50,0x06b58,0x05ac0,0x0ab60,0x096d5,0x092e0,
  0x0c960,0x0d954,0x0d4a0,0x0da50,0x07552,0x056a0,0x0abb7,0x025d0,0x092d0,0x0cab5,
  0x0a950,0x0b4a0,0x0baa4,0x0ad50,0x055d9,0x04ba0,0x0a5b0,0x15176,0x052b0,0x0a930,
  0x07954,0x06aa0,0x0ad50,0x05b52,0x04b60,0x0a6e6,0x0a4e0,0x0d260,0x0ea65,0x0d530,
  0x05aa0,0x076a3,0x096d0,0x04bd7,0x04ad0,0x0a4d0,0x1d0b6,0x0d250,0x0d520,0x0dd45,
  0x0b5a0,0x056d0,0x055b2,0x049b0,0x0a577,0x0a4b0,0x0aa50,0x1b255,0x06d20,0x0ada0,
  0x14b63,0x09370,0x049f8,0x04970,0x064b0,0x168a6,0x0ea50,0x06b20,0x1a6c4,0x0aae0,
  0x0a2e0,0x0d2e3,0x0c960,0x0d557,0x0d4a0,0x0da50,0x07552,0x056a0,0x0abb7,0x025d0,
  0x092d0,0x0cab5,0x0a950,0x0b4a0,0x0baa4,0x0ad50,0x055d9,0x04ba0,0x0a5b0,0x15176,
  0x052b0,0x0a930,0x07954,0x06aa0,0x0ad50,0x05b52,0x04b60,0x0a6e6,0x0a4e0,0x0d260,
  0x0ea65,0x0d530,0x05aa0,0x076a3,0x096d0,0x04bd7,0x04ad0,0x0a4d0,0x1d0b6,0x0d250
]

function lYearDays(y: number): number {
  let sum = 348
  const info = lunarInfo[y - 1900]
  for (let i = 0; i < 12; i++) {
    if ((info & (0x8000 >> i)) !== 0) sum += 1
  }
  return sum + leapDays(y)
}

function leapMonth(y: number): number {
  return lunarInfo[y - 1900] & 0xf
}

function leapDays(y: number): number {
  if (leapMonth(y)) return (lunarInfo[y - 1900] & 0x10000) ? 30 : 29
  return 0
}

function monthDays(y: number, m: number): number {
  return (lunarInfo[y - 1900] & (0x8000 >> (m - 1))) ? 30 : 29
}

const dayCn = ['', '初一','初二','初三','初四','初五','初六','初七','初八','初九','初十','十一','十二','十三','十四','十五','十六','十七','十八','十九','二十','廿一','廿二','廿三','廿四','廿五','廿六','廿七','廿八','廿九','三十']
const monthCn = ['正','二','三','四','五','六','七','八','九','十','十一','腊']

export function solar2lunar(year: number, month: number, day: number) {
  if (year < 1900 || year > 2099) {
    return null
  }
  const baseDate = new Date(1900, 0, 31)
  const objDate = new Date(year, month - 1, day)
  let offset = Math.floor((objDate.getTime() - baseDate.getTime()) / 86400000)

  let iYear = 1900
  let temp = 0
  while (iYear < 2100 && offset > 0) {
    temp = lYearDays(iYear)
    offset -= temp
    iYear++
  }
  if (offset < 0) {
    offset += temp
    iYear--
  }

  const lunarYear = iYear
  const leap = leapMonth(lunarYear)
  let isLeap = false
  let lunarMonth = 1
  let j = 1
  for (j = 1; j <= 12 && offset > 0; j++) {
    // leap month handling
    if (leap > 0 && j === leap + 1 && !isLeap) {
      j--
      isLeap = true
      temp = leapDays(lunarYear)
    } else {
      temp = monthDays(lunarYear, j)
    }
    if (isLeap && j === leap + 1) isLeap = false
    offset -= temp
  }

  if (offset === 0 && leap > 0 && j === leap + 1) {
    if (isLeap) {
      isLeap = false
    } else {
      isLeap = true
      j--
    }
  }
  if (offset < 0) {
    offset += temp
    j--
  }

  lunarMonth = j
  const lunarDay = offset + 1

  const lunarMonthName = (isLeap ? '闰' : '') + monthCn[lunarMonth - 1] + '月'
  const lunarDayName = dayCn[lunarDay]
  const lunarText = `${lunarMonthName}${lunarDayName}`

  return {
    lYear: lunarYear,
    lMonth: lunarMonth,
    lDay: lunarDay,
    isLeap,
    lunar: lunarText,
    lunarMonthName,
    lunarDayName,
  }
}

export default { solar2lunar }

// 24节气数据（用于 1900-2099 年范围的近似计算）
const sTermInfo = [0,21208,42467,63836,85337,107014,128867,150921,173149,195551,218072,240693,263343,285989,308563,331033,353350,375494,397447,419210,440795,462224,483532,504758]
const solarTermNames = ['小寒','大寒','立春','雨水','惊蛰','春分','清明','谷雨','立夏','小满','芒种','夏至','小暑','大暑','立秋','处暑','白露','秋分','寒露','霜降','立冬','小雪','大雪','冬至']

function sTerm(year: number, n: number): number {
  const base = Date.UTC(1900, 0, 6, 2, 5, 0) // 1900-01-06 02:05 UTC
  const msPerYear = 31556925974.7
  const off = msPerYear * (year - 1900) + sTermInfo[n] * 60000
  const dt = new Date(base + off)
  return dt.getUTCDate()
}

export function getSolarTermName(year: number, month: number, day: number): string | null {
  try {
    for (let i = 0; i < 24; i++) {
      const termMonth = Math.floor(i / 2) + 1
      if (termMonth !== month) continue
      const termDay = sTerm(year, i)
      if (termDay === day) return solarTermNames[i]
    }
  } catch (e) { /* ignore */ }
  return null
}

