export interface ParsedArray {
  arrayName: string
  length: number
  values: number[]
  initializedCount: number
}

const intArrayPattern = /\bint\s+([A-Za-z_]\w*)\s*\[\s*([1-9]\d*)\s*\]\s*=\s*\{([^}]*)\}\s*;/m

export function parseFirstIntArray(code: string): ParsedArray | null {
  const match = code.match(intArrayPattern)
  if (!match) return null

  const [, arrayName, rawLength, rawValues] = match
  const length = Number(rawLength)
  if (!Number.isSafeInteger(length) || length <= 0) return null

  const tokens = rawValues
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)

  if (tokens.some((token) => !/^-?\d+$/.test(token))) return null

  const initializedValues = tokens.map(Number)
  const values = initializedValues.slice(0, length)
  while (values.length < length) values.push(0)

  return {
    arrayName,
    length,
    values,
    initializedCount: Math.min(initializedValues.length, length)
  }
}
