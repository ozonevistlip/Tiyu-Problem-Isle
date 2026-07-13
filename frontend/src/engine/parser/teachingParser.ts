import { Tokenizer, type Token } from './tokenizer'
import type {
  ArrayAccessNode, AssignmentNode, BlockNode, DeclarationNode, ExpressionNode, FunctionNode, InitializerNode, ParseError,
  PrimitiveType, ProgramNode, StatementNode
} from '../types'

const types = new Set<PrimitiveType>(['int', 'double', 'bool', 'char', 'string', 'void'])

export function parseTeachingCpp(source: string): ProgramNode {
  const tokenizer = new Tokenizer(source)
  const parser = new TeachingParser(tokenizer.tokenize(), tokenizer.errors)
  return parser.parseProgram()
}

export class TeachingParser {
  private index = 0
  constructor(private readonly tokens: Token[], private readonly errors: ParseError[]) {}

  parseProgram(): ProgramNode {
    const functions: FunctionNode[] = []
    while (!this.atEnd()) {
      if (this.isType(this.peek())) functions.push(this.parseFunction())
      else this.error(this.peek(), `这里应该是函数声明，例如 int main() { ... }。`)
    }
    if (this.errors.length) throw this.errors
    if (!functions.some((item) => item.name === 'main')) throw [{ line: 1, column: 1, message: '没有找到 main()，程序需要从 main() 开始。' }] satisfies ParseError[]
    return { kind: 'program', functions }
  }

  private parseFunction(): FunctionNode {
    const returnType = this.consumeType()
    const name = this.consume('identifier', '函数需要有名字，例如 main 或 add。')
    this.consumeValue('(', '函数名后面需要有左括号 (。')
    const parameters = []
    if (!this.checkValue(')')) {
      do {
        const dataType = this.consumeType()
        const parameterName = this.consume('identifier', '参数需要有名字。')
        parameters.push({ name: parameterName.value, dataType, line: parameterName.line, column: parameterName.column })
      } while (this.matchValue(','))
    }
    this.consumeValue(')', '函数参数后面需要有右括号 )。')
    const body = this.parseBlock()
    return { kind: 'function', name: name.value, returnType, parameters, body, line: name.line, column: name.column }
  }

  private parseBlock(): BlockNode {
    const open = this.consumeValue('{', '这里需要用 { 开始一段代码。')
    const statements: StatementNode[] = []
    while (!this.checkValue('}') && !this.atEnd()) statements.push(this.parseStatement())
    this.consumeValue('}', '这段代码缺少右花括号 }。')
    return { kind: 'block', statements, line: open.line, column: open.column }
  }

  private parseStatement(): StatementNode {
    if (this.checkValue('{')) return this.parseBlock()
    if (this.isType(this.peek()) && this.peek().value !== 'void') return this.parseDeclaration(true)
    if (this.matchValue('if')) return this.parseIf(this.previous())
    if (this.matchValue('for')) return this.parseFor(this.previous())
    if (this.matchValue('while')) return this.parseWhile(this.previous())
    if (this.matchValue('return')) return this.parseReturn(this.previous())
    if (this.matchValue('break')) { const token = this.previous(); this.consumeValue(';', 'break 后面需要分号。'); return { kind: 'break', line: token.line, column: token.column } }
    if (this.matchValue('continue')) { const token = this.previous(); this.consumeValue(';', 'continue 后面需要分号。'); return { kind: 'continue', line: token.line, column: token.column } }
    if (this.matchValue('cout')) return this.parseOutput(this.previous())
    if (this.matchValue('cin')) return this.parseInput(this.previous())
    const expression = this.parseExpression()
    this.consumeValue(';', '一条语句结束时需要分号 ;。')
    return { kind: 'expression_statement', expression, line: expression.line, column: expression.column }
  }

  private parseDeclaration(requireSemicolon: boolean): DeclarationNode {
    const typeToken = this.advance()
    const dataType = typeToken.value as PrimitiveType
    const name = this.consume('identifier', '变量需要有名字。')
    const dimensions: ExpressionNode[] = []
    while (this.matchValue('[')) { dimensions.push(this.parseExpression()); this.consumeValue(']', '数组下标后面需要 ]。') }
    let initializer: InitializerNode | ExpressionNode | undefined
    if (this.matchValue('=')) initializer = this.checkValue('{') ? this.parseInitializer() : this.parseExpression()
    if (requireSemicolon) this.consumeValue(';', '变量声明后面需要分号 ;。')
    return { kind: 'declaration', dataType, name: name.value, dimensions, initializer, line: typeToken.line, column: typeToken.column }
  }

  private parseInitializer(): InitializerNode {
    const start = this.consumeValue('{', '数组初始值需要从 { 开始。')
    const values: Array<InitializerNode | ExpressionNode> = []
    if (!this.checkValue('}')) {
      do values.push(this.checkValue('{') ? this.parseInitializer() : this.parseExpression())
      while (this.matchValue(','))
    }
    this.consumeValue('}', '数组初始值缺少右花括号 }。')
    return { kind: 'initializer', values, line: start.line, column: start.column }
  }

  private parseIf(token: Token): StatementNode {
    this.consumeValue('(', 'if 后面需要 ( 条件 )。')
    const condition = this.parseExpression()
    this.consumeValue(')', 'if 条件后面需要 )。')
    const consequent = this.parseStatementAsBlock()
    let alternate: BlockNode | StatementNode | undefined
    if (this.matchValue('else')) alternate = this.matchValue('if') ? this.parseIf(this.previous()) : this.parseStatementAsBlock()
    return { kind: 'if', condition, consequent, alternate: alternate as BlockNode | undefined, line: token.line, column: token.column }
  }

  private parseFor(token: Token): StatementNode {
    this.consumeValue('(', 'for 后面需要 (。')
    let initializer: DeclarationNode | ExpressionNode | undefined
    if (!this.checkValue(';')) initializer = this.isType(this.peek()) ? this.parseDeclaration(false) : this.parseExpression()
    this.consumeValue(';', 'for 的第一部分后面需要 ;。')
    const condition = this.checkValue(';') ? undefined : this.parseExpression()
    this.consumeValue(';', 'for 的条件后面需要 ;。')
    const update = this.checkValue(')') ? undefined : this.parseExpression()
    this.consumeValue(')', 'for 的括号没有正确结束。')
    const body = this.parseStatementAsBlock()
    return { kind: 'for', initializer, condition, update, body, line: token.line, column: token.column }
  }

  private parseWhile(token: Token): StatementNode {
    this.consumeValue('(', 'while 后面需要 ( 条件 )。')
    const condition = this.parseExpression()
    this.consumeValue(')', 'while 条件后面需要 )。')
    return { kind: 'while', condition, body: this.parseStatementAsBlock(), line: token.line, column: token.column }
  }

  private parseReturn(token: Token): StatementNode {
    const value = this.checkValue(';') ? undefined : this.parseExpression()
    this.consumeValue(';', 'return 后面需要分号 ;。')
    return { kind: 'return', value, line: token.line, column: token.column }
  }

  private parseOutput(token: Token): StatementNode {
    const values: ExpressionNode[] = []
    do { this.consumeValue('<<', 'cout 后面需要 << 输出内容。'); values.push(this.parseExpression()) } while (this.checkValue('<<'))
    this.consumeValue(';', 'cout 语句后面需要分号 ;。')
    return { kind: 'output', values, line: token.line, column: token.column }
  }

  private parseInput(token: Token): StatementNode {
    const targets: ExpressionNode[] = []
    do { this.consumeValue('>>', 'cin 后面需要 >> 变量名。'); targets.push(this.parsePrimary()) } while (this.checkValue('>>'))
    this.consumeValue(';', 'cin 语句后面需要分号 ;。')
    return { kind: 'input', targets, line: token.line, column: token.column }
  }

  private parseStatementAsBlock(): BlockNode {
    if (this.checkValue('{')) return this.parseBlock()
    const statement = this.parseStatement()
    return { kind: 'block', statements: [statement], line: statement.line, column: statement.column }
  }

  private parseExpression(): ExpressionNode { return this.parseAssignment() }

  private parseAssignment(): ExpressionNode {
    const expression = this.parseLogicalOr()
    if (this.matchValue('=', '+=', '-=')) {
      const operator = this.previous()
      if (expression.kind !== 'identifier' && expression.kind !== 'array_access') this.error(operator, '赋值号左侧需要是变量或数组元素。')
      const target = expression as AssignmentNode['target']
      return { kind: 'assignment', target, operator: operator.value as AssignmentNode['operator'], value: this.parseAssignment(), line: operator.line, column: operator.column }
    }
    return expression
  }

  private parseLogicalOr(): ExpressionNode { return this.parseBinary(() => this.parseLogicalAnd(), ['||']) }
  private parseLogicalAnd(): ExpressionNode { return this.parseBinary(() => this.parseEquality(), ['&&']) }
  private parseEquality(): ExpressionNode { return this.parseBinary(() => this.parseComparison(), ['==', '!=']) }
  private parseComparison(): ExpressionNode { return this.parseBinary(() => this.parseTerm(), ['<', '<=', '>', '>=']) }
  private parseTerm(): ExpressionNode { return this.parseBinary(() => this.parseFactor(), ['+', '-']) }
  private parseFactor(): ExpressionNode { return this.parseBinary(() => this.parseUnary(), ['*', '/', '%']) }

  private parseBinary(next: () => ExpressionNode, operators: string[]): ExpressionNode {
    let expression = next()
    while (this.matchValue(...operators)) {
      const operator = this.previous()
      expression = { kind: 'binary', operator: operator.value, left: expression, right: next(), line: operator.line, column: operator.column }
    }
    return expression
  }

  private parseUnary(): ExpressionNode {
    if (this.matchValue('!', '-', '++', '--')) {
      const operator = this.previous()
      return { kind: 'unary', operator: operator.value, argument: this.parseUnary(), postfix: false, line: operator.line, column: operator.column }
    }
    let expression = this.parsePrimary()
    if (this.matchValue('++', '--')) {
      const operator = this.previous()
      expression = { kind: 'unary', operator: operator.value, argument: expression, postfix: true, line: operator.line, column: operator.column }
    }
    return expression
  }

  private parsePrimary(): ExpressionNode {
    const token = this.advance()
    if (token.kind === 'number') return { kind: 'literal', value: Number(token.value), literalType: token.value.includes('.') ? 'double' : 'int', line: token.line, column: token.column }
    if (token.kind === 'string') return { kind: 'literal', value: token.value, literalType: 'string', line: token.line, column: token.column }
    if (token.kind === 'char') return { kind: 'literal', value: token.value, literalType: 'char', line: token.line, column: token.column }
    if (token.value === 'true' || token.value === 'false') return { kind: 'literal', value: token.value === 'true', literalType: 'bool', line: token.line, column: token.column }
    if (token.value === '(') { const expression = this.parseExpression(); this.consumeValue(')', '表达式缺少右括号 )。'); return expression }
    if (token.kind !== 'identifier') this.error(token, `暂时不能把 “${token.value}” 当作表达式。`)
    const identifier = { kind: 'identifier' as const, name: token.value, line: token.line, column: token.column }
    if (this.matchValue('(')) {
      const arguments_ = this.parseArguments()
      return { kind: 'call', callee: identifier.name, arguments: arguments_, line: token.line, column: token.column }
    }
    if (this.matchValue('.')) {
      const member = this.consume('identifier', '点号后面需要方法名，例如 length。')
      this.consumeValue('(', '方法名后面需要 (。')
      return { kind: 'member_call', target: identifier, member: member.value, arguments: this.parseArguments(), line: token.line, column: token.column }
    }
    const indices: ExpressionNode[] = []
    while (this.matchValue('[')) { indices.push(this.parseExpression()); this.consumeValue(']', '数组下标缺少 ]。') }
    return indices.length ? { kind: 'array_access', target: identifier, indices, line: token.line, column: token.column } as ArrayAccessNode : identifier
  }

  private parseArguments() {
    const arguments_: ExpressionNode[] = []
    if (!this.checkValue(')')) { do arguments_.push(this.parseExpression()); while (this.matchValue(',')) }
    this.consumeValue(')', '函数调用缺少右括号 )。')
    return arguments_
  }

  private consumeType() { const token = this.peek(); if (!this.isType(token)) this.error(token, '这里需要 int、double、bool、char、string 或 void。'); return this.advance().value as PrimitiveType }
  private isType(token: Token) { return types.has(token.value as PrimitiveType) }
  private consume(kind: Token['kind'], message: string) { if (this.peek().kind === kind) return this.advance(); this.error(this.peek(), message) }
  private consumeValue(value: string, message: string) { if (this.checkValue(value)) return this.advance(); this.error(this.peek(), message) }
  private matchValue(...values: string[]) { if (!values.includes(this.peek().value)) return false; this.advance(); return true }
  private checkValue(value: string) { return this.peek().value === value }
  private peek() { return this.tokens[this.index] }
  private previous() { return this.tokens[this.index - 1] }
  private advance() { if (!this.atEnd()) this.index += 1; return this.tokens[this.index - 1] }
  private atEnd() { return this.peek().kind === 'eof' }
  private error(token: Token, message: string): never { this.errors.push({ line: token.line, column: token.column, message }); throw this.errors }
}
