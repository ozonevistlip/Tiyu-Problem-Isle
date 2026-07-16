import type { ParseError, SourceLocation } from '../types'

export type TokenKind = 'identifier' | 'number' | 'string' | 'char' | 'symbol' | 'keyword' | 'eof'
export interface Token extends SourceLocation { kind: TokenKind; value: string }

const keywords = new Set(['int', 'double', 'bool', 'char', 'string', 'void', 'if', 'else', 'for', 'while', 'return', 'break', 'continue', 'true', 'false', 'cout', 'cin'])
const multiSymbols = ['==', '!=', '<=', '>=', '&&', '||', '++', '--', '+=', '-=', '<<', '>>']

export class Tokenizer {
  private index = 0
  private line = 1
  private column = 1
  readonly errors: ParseError[] = []

  constructor(private readonly source: string) {}

  tokenize() {
    const tokens: Token[] = []
    while (!this.finished()) {
      this.skipTrivia()
      if (this.finished()) break
      const line = this.line
      const column = this.column
      const char = this.peek()
      if (this.isLetter(char) || char === '_') tokens.push(this.readIdentifier(line, column))
      else if (this.isDigit(char)) tokens.push(this.readNumber(line, column))
      else if (char === '"') tokens.push(this.readQuoted('string', line, column))
      else if (char === "'") tokens.push(this.readQuoted('char', line, column))
      else tokens.push(this.readSymbol(line, column))
    }
    tokens.push({ kind: 'eof', value: '', line: this.line, column: this.column })
    return tokens
  }

  private skipTrivia() {
    while (!this.finished()) {
      if (/\s/.test(this.peek())) { this.advance(); continue }
      if (this.peek() === '/' && this.peek(1) === '/') { while (!this.finished() && this.peek() !== '\n') this.advance(); continue }
      if (this.peek() === '/' && this.peek(1) === '*') {
        this.advance(); this.advance()
        while (!this.finished() && !(this.peek() === '*' && this.peek(1) === '/')) this.advance()
        if (this.finished()) { this.errors.push({ line: this.line, column: this.column, message: '多行注释没有找到结束符 */。' }); return }
        this.advance(); this.advance(); continue
      }
      if (this.peek() === '#') { while (!this.finished() && this.peek() !== '\n') this.advance(); continue }
      if (this.skipUsingNamespaceDirective()) continue
      break
    }
  }

  private skipUsingNamespaceDirective() {
    const directive = this.source.slice(this.index).match(/^using[ \t]+namespace[ \t]+[A-Za-z_][A-Za-z0-9_]*(?:::[A-Za-z_][A-Za-z0-9_]*)*[ \t]*;/)
    if (!directive) return false
    for (let offset = 0; offset < directive[0].length; offset += 1) this.advance()
    return true
  }

  private readIdentifier(line: number, column: number): Token {
    let value = ''
    while (this.isLetter(this.peek()) || this.isDigit(this.peek()) || this.peek() === '_') value += this.advance()
    return { kind: keywords.has(value) ? 'keyword' : 'identifier', value, line, column }
  }

  private readNumber(line: number, column: number): Token {
    let value = ''
    while (this.isDigit(this.peek())) value += this.advance()
    if (this.peek() === '.' && this.isDigit(this.peek(1))) { value += this.advance(); while (this.isDigit(this.peek())) value += this.advance() }
    return { kind: 'number', value, line, column }
  }

  private readQuoted(kind: 'string' | 'char', line: number, column: number): Token {
    const quote = this.advance()
    let value = ''
    while (!this.finished() && this.peek() !== quote) {
      if (this.peek() === '\\') { this.advance(); const escaped = this.advance(); value += ({ n: '\n', t: '\t', '0': '\0', '"': '"', "'": "'", '\\': '\\' } as Record<string, string>)[escaped] ?? escaped }
      else value += this.advance()
    }
    if (this.finished()) this.errors.push({ line, column, message: '字符串或字符没有找到结束引号。' })
    else this.advance()
    return { kind, value, line, column }
  }

  private readSymbol(line: number, column: number): Token {
    const pair = this.source.slice(this.index, this.index + 2)
    if (multiSymbols.includes(pair)) { this.advance(); this.advance(); return { kind: 'symbol', value: pair, line, column } }
    const value = this.advance()
    if (!'(){}[];,.+-*/%!=<>'.includes(value)) this.errors.push({ line, column, message: `暂时不能识别符号 “${value}”。` })
    return { kind: 'symbol', value, line, column }
  }

  private finished() { return this.index >= this.source.length }
  private peek(offset = 0) { return this.source[this.index + offset] ?? '' }
  private advance() { const char = this.source[this.index++] ?? ''; if (char === '\n') { this.line += 1; this.column = 1 } else this.column += 1; return char }
  private isLetter(char: string) { return /[A-Za-z]/.test(char) }
  private isDigit(char: string) { return /[0-9]/.test(char) }
}
