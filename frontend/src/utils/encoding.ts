// 尝试修复后端返回的可能的 mojibake 编码（例如 UTF-8 被当作 Latin1 显示）
export function decodeMaybeMojibake(s?: string | null): string | null {
  if (s == null) return s
  try {
    // 常见浏览器端修复方法：decodeURIComponent(escape(...))
    // 只在转换后出现非 ASCII 字符时才采用
    // eslint-disable-next-line no-undef
    const try1 = decodeURIComponent(escape(s))
    if (try1 && /[^\x00-\x7F]/.test(try1) && try1 !== s) return try1
  } catch (e) {
    // ignore
  }
  // 仅在字符串很可能是 mojibake（例如包含常见 Latin1/ISO-8859-1 伪装字符）时，才尝试按字节重解码，避免把正常 UTF-8 字符串误转码。
  const likelyMojibake = /Ã|Â|�/.test(s) || /[\u00C0-\u00FF]{2,}/.test(s)
  if (likelyMojibake) {
    try {
      // 备用：用 TextDecoder 将每个字符的 code 点当作字节再解为 utf-8
      const bytes = new Uint8Array(Array.from(s).map((c) => c.charCodeAt(0) & 0xff))
      // @ts-ignore
      if (typeof TextDecoder !== 'undefined') {
        // @ts-ignore
        const converted = new TextDecoder('utf-8').decode(bytes)
        if (converted && /[^\x00-\x7F]/.test(converted) && converted !== s) return converted
      }
    } catch (e) {
      // ignore
    }
  }

  return s
}

export function safeTaskTitle(title?: string | null): string {
  if (!title) return '（无标题任务）'
  const fixed = decodeMaybeMojibake(title)
  return fixed || title
}
