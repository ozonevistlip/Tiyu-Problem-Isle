import type {
  ArrayAccessNode, AssignmentNode, BaseTraceEvent, BlockNode, DeclarationNode, ExecutionLimits, ExecutionResult, ExecutionSnapshot,
  ExpressionNode, FunctionNode, InitializerNode, LoopContext, PrimitiveType, ProgramNode, RuntimeArray, RuntimeArray2D, RuntimeErrorInfo,
  RuntimeStackFrame, RuntimeValue, RuntimeVariable, StatementNode
} from '../types'
import { executionLimits } from '../types'

type Signal = { type: 'break' | 'continue' } | { type: 'return'; value: RuntimeValue }
type CompactTraceFact = Pick<BaseTraceEvent, 'type' | 'line' | 'description' | 'data'>

const compactableEventTypes = new Set<BaseTraceEvent['type']>([
  'variable_read', 'array_read', 'array_2d_read', 'string_read',
  'binary_operation', 'comparison', 'function_enter', 'function_return'
])

interface RuntimeScope {
  id: string
  name: string
  variables: Map<string, RuntimeVariable>
}

class TeachingRuntimeError extends Error {
  constructor(readonly info: RuntimeErrorInfo) { super(info.message) }
}

const voidValue = (): RuntimeValue => ({ kind: 'void' })
const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value)) as T

export class TeachingInterpreter {
  private readonly functions = new Map<string, FunctionNode>()
  private readonly events: BaseTraceEvent[] = []
  private readonly snapshots: ExecutionSnapshot[] = []
  private readonly scopes: RuntimeScope[] = []
  private readonly callStack: RuntimeStackFrame[] = []
  private readonly consoleOutput: string[] = []
  private readonly loops: LoopContext[] = []
  private scopeCounter = 0
  private eventCounter = 0
  private currentLine = 1
  private loopCounter = 0
  private compactFacts: CompactTraceFact[] | null = null
  private readonly startedAt = Date.now()

  constructor(private readonly input: string[] = [], private readonly limits: ExecutionLimits = executionLimits) {}

  run(program: ProgramNode, treeSummary: string): ExecutionResult {
    program.functions.forEach((item) => this.functions.set(item.name, item))
    if (!this.functions.has('main')) throw new TeachingRuntimeError({ line: 1, message: '没有找到 main()，程序不知道从哪里开始执行。' })
    this.emit('program_start', 1, '程序准备从 main() 开始执行。')
    this.callFunction('main', [], 1)
    this.emit('program_end', this.currentLine, '程序执行完成。')
    return { events: this.events, snapshots: this.snapshots, treeSummary }
  }

  private callFunction(name: string, arguments_: RuntimeValue[], line: number, argumentLabels: string[] = []): RuntimeValue {
    if (name === 'swap') return this.callSwap(arguments_, line, argumentLabels)
    if (name === 'strlen') return this.callStrlen(arguments_, line, argumentLabels)
    const functionNode = this.functions.get(name)
    if (!functionNode) this.fail(line, `没有找到函数 ${name}()。请先确认函数名有没有写错。`)
    if (arguments_.length !== functionNode.parameters.length) this.fail(line, `函数 ${name}() 需要 ${functionNode.parameters.length} 个参数，但现在给了 ${arguments_.length} 个。`)
    if (this.callStack.length >= this.limits.maxCallDepth) this.fail(line, `函数调用层数超过 ${this.limits.maxCallDepth} 层，先检查是不是出现了没有结束的递归。`)

    const frame = { id: `call-${this.callStack.length + 1}-${name}`, name, line, active: true }
    this.callStack.forEach((item) => { item.active = false })
    this.callStack.push(frame)
    this.pushScope(name)
    const expression = `${name}(${argumentLabels.join(', ')})`
    this.emit('function_enter', line, `进入函数 ${name}()。`, { name, expression })
    functionNode.parameters.forEach((parameter, index) => this.declareVariable(parameter.name, parameter.dataType, this.coerce(arguments_[index], parameter.dataType, parameter.line), parameter.line))
    const signal = this.executeBlock(functionNode.body, false)
    const returnValue = signal?.type === 'return' ? signal.value : voidValue()
    this.emit('function_return', line, `函数 ${name}() 返回 ${this.describeValue(returnValue)}。`, { name, value: this.describeValue(returnValue), expression })
    this.popScope(functionNode.body.line)
    this.callStack.pop()
    const active = this.callStack[this.callStack.length - 1]
    if (active) active.active = true
    return returnValue
  }

  private callSwap(arguments_: RuntimeValue[], line: number, argumentLabels: string[] = []): RuntimeValue {
    if (arguments_.length !== 2) this.fail(line, 'swap 需要两个数组元素作为参数。')
    const expression = `swap(${argumentLabels.join(', ')})`
    this.emit('function_enter', line, '调用内置函数 swap，准备交换两个值。', { name: 'swap', expression })
    this.emit('function_return', line, 'swap 调用结束。', { name: 'swap', expression })
    return voidValue()
  }

  private callStrlen(arguments_: RuntimeValue[], line: number, argumentLabels: string[] = []): RuntimeValue {
    if (arguments_.length !== 1) this.fail(line, 'strlen() 需要一个字符串或字符数组参数。')
    const value = arguments_[0]
    let length: number
    if (value.kind === 'string') length = value.value.length
    else if (value.kind === 'array' && value.elementType === 'char') {
      const terminator = value.values.findIndex((item) => item.kind === 'char' && item.value === '\0')
      length = terminator < 0 ? value.length : terminator
    } else this.fail(line, 'strlen() 目前支持 string 和 char 数组。')
    const expression = `strlen(${argumentLabels.join(', ')})`
    this.emit('function_enter', line, `计算 ${expression}。`, { name: 'strlen', expression })
    this.emit('function_return', line, `${expression} = ${length}。`, { name: 'strlen', value: String(length), expression })
    return { kind: 'int', value: length }
  }

  private callSort(arguments_: ExpressionNode[], line: number): RuntimeValue {
    if (arguments_.length !== 2 && arguments_.length !== 3) this.fail(line, 'sort() 需要起始位置、结束位置，以及可选的比较器参数。')
    const start = this.resolveArrayIterator(arguments_[0])
    const end = this.resolveArrayIterator(arguments_[1])
    const descending = arguments_.length === 3 && this.isDescendingSort(arguments_[2])
    if (start.name !== end.name) this.fail(line, 'sort() 的两个位置需要来自同一个一维数组。')

    const variable = this.lookup(start.name, line)
    if (variable.value.kind !== 'array') this.fail(line, 'sort() 目前只支持一维数组。')
    if (start.index < 0 || end.index < 0 || start.index > end.index || end.index > variable.value.length) {
      this.fail(line, `sort() 的排序范围需要在 ${start.name} 的有效下标内，并且结束位置不能小于起始位置。`)
    }

    const previous = clone(variable.value)
    const sorted = variable.value.values
      .slice(start.index, end.index)
      .sort((left, right) => (this.sortValue(left, line) - this.sortValue(right, line)) * (descending ? -1 : 1))
    variable.value.values.splice(start.index, end.index - start.index, ...sorted)
    variable.changed = true
    variable.previousValue = previous

    const range = end.index > start.index ? `${start.name}[${start.index}] 到 ${start.name}[${end.index - 1}]` : '空范围'
    this.emit('array_write', line, `将 ${range} 按从${descending ? '大' : '小'}到${descending ? '小' : '大'}的顺序排序。`, {
      name: start.name,
      indices: `${start.index},${end.index - 1}`,
      value: this.describeValue(variable.value),
      previousValue: this.describeValue(previous),
      animationKind: 'sort',
      animationDurationMs: 3200,
      rangeStart: start.index,
      rangeEnd: end.index,
      direction: descending ? 'descending' : 'ascending',
      previousValues: previous.values.map((item) => this.describeValue(item)),
      sortedValues: variable.value.values.map((item) => this.describeValue(item))
    })
    return voidValue()
  }

  private isDescendingSort(node: ExpressionNode) {
    if (node.kind === 'call' && node.callee === 'greater' && node.arguments.length === 0) return true
    if (node.kind === 'call' && node.callee === 'less' && node.arguments.length === 0) return false
    this.fail(node.line, 'sort() 的第三个参数目前支持 greater<int>()（降序）或 less<int>()（升序）。')
  }

  private resolveArrayIterator(node: ExpressionNode): { name: string; index: number } {
    if (node.kind === 'identifier') return { name: node.name, index: 0 }
    if (node.kind === 'binary' && node.operator === '+') {
      const base = this.resolveArrayIterator(node.left)
      const offset = this.asInteger(this.evaluate(node.right), node.right.line, 'sort() 的数组位置')
      return { name: base.name, index: base.index + offset }
    }
    this.fail(node.line, 'sort() 的参数需要是数组名加位置，例如 arr + 1。')
  }

  private sortValue(value: RuntimeValue, line: number): number {
    if (value.kind === 'int' || value.kind === 'double') return value.value
    if (value.kind === 'char') return value.value.charCodeAt(0)
    this.fail(line, 'sort() 目前支持 int、double 和 char 数组。')
  }

  private executeBlock(block: BlockNode, createScope = true): Signal | undefined {
    if (createScope) this.pushScope('代码块', block.line)
    for (const statement of block.statements) {
      const signal = this.executeStatement(statement)
      if (signal) { if (createScope) this.popScope(statement.line); return signal }
    }
    if (createScope) this.popScope(block.line)
    return undefined
  }

  private executeStatement(statement: StatementNode): Signal | undefined {
    this.currentLine = statement.line
    switch (statement.kind) {
      case 'declaration': this.executeDeclaration(statement); return undefined
      case 'declaration_list': this.executeDeclarationList(statement.declarations); return undefined
      case 'expression_statement': this.evaluate(statement.expression); return undefined
      case 'output': this.executeOutput(statement); return undefined
      case 'input': this.executeInput(statement); return undefined
      case 'if': return this.executeIf(statement)
      case 'for': return this.executeFor(statement)
      case 'while': return this.executeWhile(statement)
      case 'return': return { type: 'return', value: statement.value ? this.evaluate(statement.value) : voidValue() }
      case 'break': return { type: 'break' }
      case 'continue': return { type: 'continue' }
      case 'block': return this.executeBlock(statement)
    }
  }

  private executeDeclaration(node: DeclarationNode) {
    if (node.dimensions.length === 0) {
      const initial = node.initializer && node.initializer.kind !== 'initializer' ? this.evaluateCompacted(node.initializer).value : this.defaultValue(node.dataType)
      this.declareVariable(node.name, node.dataType, this.coerce(initial, node.dataType, node.line), node.line)
      return
    }
    const dimensions = node.dimensions.map((item) => this.asInteger(this.evaluateCompacted(item).value, item.line, '数组长度'))
    if (dimensions.some((item) => item <= 0)) this.fail(node.line, '数组长度需要是大于 0 的整数。')
    if (dimensions.length === 1) {
      const length = dimensions[0]
      if (length > this.limits.maxArrayLength) this.fail(node.line, `数组长度不能超过 ${this.limits.maxArrayLength}。`)
      const values = this.buildArrayValues(node.initializer, node.dataType, length, node.line)
      this.putVariable(node.name, node.dataType, { kind: 'array', elementType: node.dataType, length, values }, node.line)
      this.emit('array_declare', node.line, `创建数组 ${node.name}，共有 ${length} 个位置。`, { name: node.name, length })
      return
    }
    if (dimensions.length === 2) {
      const [rows, columns] = dimensions
      if (rows * columns > this.limits.maxMatrixCells) this.fail(node.line, `二维数组总格子数不能超过 ${this.limits.maxMatrixCells}。`)
      const values = this.buildMatrixValues(node.initializer, node.dataType, rows, columns, node.line)
      this.putVariable(node.name, node.dataType, { kind: 'array2d', elementType: node.dataType, rows, columns, values }, node.line)
      this.emit('array_2d_declare', node.line, `创建二维数组 ${node.name}，大小是 ${rows} × ${columns}。`, { name: node.name, rows, columns })
      return
    }
    this.fail(node.line, '第一版最多支持二维数组，例如 int matrix[2][3]。')
  }

  private executeDeclarationList(declarations: DeclarationNode[]) {
    declarations.forEach((declaration) => this.executeDeclaration(declaration))
  }

  private executeIf(node: Extract<StatementNode, { kind: 'if' }>): Signal | undefined {
    const evaluation = this.evaluateCompacted(node.condition)
    const condition = this.asBoolean(evaluation.value)
    const expression = this.expressionText(node.condition)
    const values = this.readFacts(evaluation.facts)
    const action = condition ? '继续向下执行 if 分支' : node.alternate ? '不执行 if 分支，转入 else 分支' : '不执行 if 分支'
    this.emit('branch', node.line, this.decisionDescription(expression, condition, values, action), { expression, result: condition, values, nextAction: action, selectedBranch: condition ? 'if' : node.alternate ? 'else' : 'none' })
    if (condition) return this.executeBlock(node.consequent)
    if (node.alternate) return node.alternate.kind === 'if' ? this.executeIf(node.alternate) : this.executeBlock(node.alternate)
    return undefined
  }

  private executeFor(node: Extract<StatementNode, { kind: 'for' }>): Signal | undefined {
    const loopId = `for-${++this.loopCounter}`
    this.pushScope('for 循环', node.line)
    if (node.initializer) {
      if (node.initializer.kind === 'declaration') this.executeDeclaration(node.initializer)
      else if (node.initializer.kind === 'declaration_list') this.executeDeclarationList(node.initializer.declarations)
      else this.evaluate(node.initializer)
    }
    const context: LoopContext = { id: loopId, type: 'for', iteration: 0, line: node.line, condition: this.expressionText(node.condition), result: true }
    this.loops.push(context)
    this.emit('loop_enter', node.line, '进入 for 循环。', { loopId, loopType: 'for' })
    let signal: Signal | undefined
    while (true) {
      const evaluation = node.condition ? this.evaluateCompacted(node.condition) : { value: { kind: 'bool', value: true } as RuntimeValue, facts: [] as CompactTraceFact[] }
      const condition = this.asBoolean(evaluation.value)
      const values = this.readFacts(evaluation.facts)
      context.result = condition
      const action = condition ? '继续向下执行循环体' : '不再执行循环体，结束循环'
      if (condition) { this.guardLoop(node.line, context); context.iteration += 1 }
      this.emit('loop_condition', node.line, this.decisionDescription(context.condition, condition, values, action), { loopId, result: condition, iteration: context.iteration, values, nextAction: action, expression: context.condition })
      if (!condition) break
      signal = this.executeBlock(node.body)
      if (signal?.type === 'return') break
      if (signal?.type === 'break') { this.emit('loop_exit', node.line, '遇到 break，直接结束循环。', { loopId, nextAction: '结束循环' }); signal = undefined; break }
      if (node.update) this.evaluateCompacted(node.update)
    }
    this.loops.pop()
    this.popScope(node.line)
    return signal?.type === 'continue' ? undefined : signal
  }

  private executeWhile(node: Extract<StatementNode, { kind: 'while' }>): Signal | undefined {
    const loopId = `while-${++this.loopCounter}`
    const context: LoopContext = { id: loopId, type: 'while', iteration: 0, line: node.line, condition: this.expressionText(node.condition), result: true }
    this.loops.push(context)
    this.emit('loop_enter', node.line, '进入 while 循环。', { loopId, loopType: 'while' })
    let signal: Signal | undefined
    while (true) {
      const evaluation = this.evaluateCompacted(node.condition)
      const condition = this.asBoolean(evaluation.value)
      const values = this.readFacts(evaluation.facts)
      context.result = condition
      const action = condition ? '继续向下执行循环体' : '不再执行循环体，结束循环'
      if (condition) { this.guardLoop(node.line, context); context.iteration += 1 }
      this.emit('loop_condition', node.line, this.decisionDescription(context.condition, condition, values, action), { loopId, result: condition, iteration: context.iteration, values, nextAction: action, expression: context.condition })
      if (!condition) break
      signal = this.executeBlock(node.body)
      if (signal?.type === 'return') break
      if (signal?.type === 'break') { this.emit('loop_exit', node.line, '遇到 break，直接结束循环。', { loopId, nextAction: '结束循环' }); signal = undefined; break }
    }
    this.loops.pop()
    return signal?.type === 'continue' ? undefined : signal
  }

  private executeOutput(node: Extract<StatementNode, { kind: 'output' }>) {
    const evaluations = node.values.map((item) => this.evaluateCompacted(item))
    const text = evaluations.map((item) => this.describeValue(item.value)).join('')
    const values = this.readFacts(evaluations.flatMap((item) => item.facts))
    this.consoleOutput.push(text)
    this.emit('console_output', node.line, `${values.length ? `${values.join('，')}；` : ''}输出：${text}`, { text, values })
  }

  private executeInput(node: Extract<StatementNode, { kind: 'input' }>) {
    node.targets.forEach((target) => {
      const input = this.input.shift()
      if (input === undefined) this.fail(node.line, '程序正在等待输入，但输入框里的内容不够。')
      this.writeInputTarget(target, input)
    })
  }

  private writeInputTarget(target: ExpressionNode, input: string) {
    if (target.kind === 'identifier') {
      const variable = this.lookup(target.name, target.line)
      if (variable.value.kind === 'array' && variable.value.elementType === 'char') {
        const characters = Array.from(input)
        if (characters.length >= variable.value.length) this.fail(target.line, `输入的字符长度不能超过 ${variable.value.length - 1}，还需要保留一个位置存放结束字符 \\0。`)
        const previous = clone(variable.value)
        variable.value.values = Array.from({ length: variable.value.length }, (_, index) => ({ kind: 'char' as const, value: characters[index] ?? '\0' }))
        variable.changed = true
        variable.previousValue = previous
        this.emit('console_input', target.line, `读取输入 ${input}，放进字符数组 ${variable.name}，并自动补上结束字符 \\0。`, { name: variable.name, text: input, value: this.describeValue(variable.value) })
        return
      }
      if (variable.value.kind === 'array' || variable.value.kind === 'array2d') {
        this.fail(target.line, `不能把输入直接写进整个数组 ${variable.name}，请指定下标，例如 ${variable.name}[i]。`)
      }
      const value = this.parseInput(input, variable.dataType, target.line)
      const previous = clone(variable.value)
      variable.value = value
      variable.changed = true
      variable.previousValue = previous
      this.emit('console_input', target.line, `读取输入 ${input}，放进变量 ${variable.name}。`, { name: variable.name, text: input })
      return
    }

    if (target.kind === 'array_access') {
      const variable = this.lookup(target.target.name, target.line)
      const indexEvaluations = target.indices.map((item) => this.evaluateCompacted(item))
      const indices = indexEvaluations.map((item, index) => this.asInteger(item.value, target.indices[index].line, '数组下标'))
      const dataType = this.elementType(variable.value)
      const value = this.parseInput(input, dataType, target.line)
      const previousContainer = clone(variable.value)
      const previous = this.readContainerValue(variable.value, indices, target.line, variable.name)
      this.writeContainerValue(variable.value, indices, this.coerce(value, dataType, target.line), target.line, variable.name)
      variable.changed = true
      variable.previousValue = previousContainer

      const targetName = `${variable.name}[${indices.join('][')}]`
      const indexValues = this.readFacts(indexEvaluations.flatMap((item) => item.facts))
      this.emit('console_input', target.line, `${indexValues.length ? `${indexValues.join('，')}；` : ''}读取输入 ${input}，将 ${targetName} 从 ${this.describeValue(previous)} 更新为 ${this.describeValue(value)}。`, { name: variable.name, indices: indices.join(','), text: input, value: this.describeValue(value), previousValue: this.describeValue(previous), values: indexValues })
      return
    }

    this.fail(target.line, 'cin 的输入目标需要是变量或带下标的数组元素，例如 n 或 arr[i]。')
  }

  private evaluateCompacted(node: ExpressionNode) {
    const parentFacts = this.compactFacts
    const facts: CompactTraceFact[] = []
    this.compactFacts = facts
    try {
      return { value: this.evaluate(node), facts }
    } finally {
      this.compactFacts = parentFacts
      if (parentFacts) parentFacts.push(...facts)
    }
  }

  private evaluate(node: ExpressionNode): RuntimeValue {
    this.checkTimeout(node.line)
    switch (node.kind) {
      case 'literal': return this.literalValue(node)
      case 'identifier': return node.name === 'endl' ? { kind: 'string', value: '\n' } : this.readVariable(node.name, node.line)
      case 'array_access': return this.readArray(node)
      case 'assignment': return this.assign(node)
      case 'unary': return this.evaluateUnary(node)
      case 'binary': return this.evaluateBinary(node)
      case 'call':
        if (node.callee === 'sort') return this.callSort(node.arguments, node.line)
        return this.callFunction(
          node.callee,
          node.arguments.map((item) => this.evaluate(item)),
          node.line,
          node.arguments.map((item) => this.expressionText(item))
        )
      case 'member_call': return this.evaluateMemberCall(node)
    }
  }

  private evaluateUnary(node: Extract<ExpressionNode, { kind: 'unary' }>): RuntimeValue {
    if (node.operator === '!') return { kind: 'bool', value: !this.asBoolean(this.evaluate(node.argument)) }
    if (node.operator === '-') return this.numberValue(-this.asNumber(this.evaluate(node.argument), node.line), node.line)
    if (node.operator === '++' || node.operator === '--') {
      const target = this.resolveTarget(node.argument)
      const previous = clone(target.value)
      const delta = node.operator === '++' ? 1 : -1
      const next = this.numberValue(this.asNumber(target.value, node.line) + delta, node.line)
      target.value = this.coerce(next, target.dataType, node.line)
      target.changed = true
      target.previousValue = previous
      this.emit('variable_assign', node.line, `${target.name} 从 ${this.describeValue(previous)} 变成 ${this.describeValue(target.value)}。`, { name: target.name, value: this.describeValue(target.value) })
      return node.postfix ? previous : target.value
    }
    this.fail(node.line, `暂时不支持一元运算符 ${node.operator}。`)
  }

  private evaluateBinary(node: Extract<ExpressionNode, { kind: 'binary' }>): RuntimeValue {
    const left = this.evaluate(node.left)
    const right = this.evaluate(node.right)
    const leftText = this.describeValue(left)
    const rightText = this.describeValue(right)
    let result: RuntimeValue
    if (node.operator === '+' && (left.kind === 'string' || right.kind === 'string')) result = { kind: 'string', value: `${leftText}${rightText}` }
    else if (['+', '-', '*', '/', '%'].includes(node.operator)) {
      const a = this.asNumber(left, node.line)
      const b = this.asNumber(right, node.line)
      if ((node.operator === '/' || node.operator === '%') && b === 0) this.fail(node.line, '不能除以 0。请先检查分母或取模右边的数。')
      const value = node.operator === '+' ? a + b : node.operator === '-' ? a - b : node.operator === '*' ? a * b : node.operator === '/' ? a / b : a % b
      result = this.numberValue(value, node.line)
      this.emit('binary_operation', node.line, `计算 ${leftText} ${node.operator} ${rightText} = ${this.describeValue(result)}。`, { expression: `${leftText} ${node.operator} ${rightText}`, result: this.describeValue(result) })
      return result
    } else if (['==', '!=', '<', '<=', '>', '>=', '&&', '||'].includes(node.operator)) {
      const a = ['&&', '||'].includes(node.operator) ? this.asBoolean(left) : this.primitive(left)
      const b = ['&&', '||'].includes(node.operator) ? this.asBoolean(right) : this.primitive(right)
      const orderedA = this.orderedPrimitive(left)
      const orderedB = this.orderedPrimitive(right)
      const value = node.operator === '==' ? a === b : node.operator === '!=' ? a !== b : node.operator === '<' ? orderedA < orderedB : node.operator === '<=' ? orderedA <= orderedB : node.operator === '>' ? orderedA > orderedB : node.operator === '>=' ? orderedA >= orderedB : node.operator === '&&' ? Boolean(a) && Boolean(b) : Boolean(a) || Boolean(b)
      result = { kind: 'bool', value }
      this.emit('comparison', node.line, `判断 ${leftText} ${node.operator} ${rightText}，结果是 ${value ? 'true' : 'false'}。`, { expression: `${leftText} ${node.operator} ${rightText}`, result: value })
      return result
    } else this.fail(node.line, `暂时不支持运算符 ${node.operator}。`)
    return result
  }

  private evaluateMemberCall(node: Extract<ExpressionNode, { kind: 'member_call' }>): RuntimeValue {
    const value = this.readVariable(node.target.name, node.line)
    if (value.kind === 'string' && ['length', 'size'].includes(node.member) && node.arguments.length === 0) {
      const expression = `${node.target.name}.${node.member}()`
      this.emit('string_read', node.line, `${expression} = ${value.value.length}。`, { name: node.target.name, length: value.value.length, value: value.value.length, expression })
      return { kind: 'int', value: value.value.length }
    }
    this.fail(node.line, `暂时只支持字符串的 length() 或 size()，不能执行 ${node.target.name}.${node.member}()。`)
  }

  private assign(node: AssignmentNode): RuntimeValue {
    const evaluation = this.evaluateCompacted(node.value)
    const value = evaluation.value
    if (node.target.kind === 'identifier') {
      const variable = this.lookup(node.target.name, node.line)
      const previous = clone(variable.value)
      const next = node.operator === '=' ? value : this.applyCompound(previous, value, node.operator, node.line)
      variable.value = this.coerce(next, variable.dataType, node.line)
      variable.changed = true
      variable.previousValue = previous
      const reads = this.readFacts(evaluation.facts)
      const expression = this.expressionText(node.value)
      const operation = node.operator === '=' ? (node.value.kind === 'binary' ? `计算 ${expression}，` : '') : `执行 ${variable.name} ${node.operator} ${expression}，`
      this.emit('variable_assign', node.line, `${reads.length ? `${reads.join('，')}；` : ''}${operation}将 ${variable.name} 从 ${this.describeValue(previous)} 更新为 ${this.describeValue(variable.value)}。`, { name: variable.name, value: this.describeValue(variable.value), previousValue: this.describeValue(previous), expression, reads })
      return variable.value
    }
    const target = node.target
    const variable = this.lookup(target.target.name, node.line)
    const indexEvaluations = target.indices.map((item) => this.evaluateCompacted(item))
    const indices = indexEvaluations.map((item, index) => this.asInteger(item.value, target.indices[index].line, '数组下标'))
    const previousContainer = clone(variable.value)
    const previous = this.readContainerValue(variable.value, indices, node.line, variable.name)
    const next = node.operator === '=' ? value : this.applyCompound(previous, value, node.operator, node.line)
    const stored = this.coerce(next, this.elementType(variable.value), node.line)
    this.writeContainerValue(variable.value, indices, stored, node.line, variable.name)
    variable.changed = true
    variable.previousValue = previousContainer
    const type = variable.value.kind === 'array2d' ? 'array_2d_write' : variable.value.kind === 'string' ? 'string_write' : 'array_write'
    const reads = this.readFacts([...evaluation.facts, ...indexEvaluations.flatMap((item) => item.facts)])
    const targetName = `${variable.name}[${indices.join('][')}]`
    const expression = this.expressionText(node.value)
    const operation = node.operator === '=' ? '' : `执行 ${targetName} ${node.operator} ${expression}，`
    this.emit(type, node.line, `${reads.length ? `${reads.join('，')}；` : ''}${operation}将 ${targetName} 从 ${this.describeValue(previous)} 更新为 ${this.describeValue(stored)}。`, { name: variable.name, indices: indices.join(','), value: this.describeValue(stored), previousValue: this.describeValue(previous), expression, reads })
    return stored
  }

  private readArray(node: ArrayAccessNode): RuntimeValue {
    const variable = this.lookup(node.target.name, node.line)
    const indices = node.indices.map((item) => this.asInteger(this.evaluate(item), item.line, '数组下标'))
    const value = this.readContainerValue(variable.value, indices, node.line, variable.name)
    const type = variable.value.kind === 'array2d' ? 'array_2d_read' : variable.value.kind === 'string' ? 'string_read' : 'array_read'
    this.emit(type, node.line, `读取 ${variable.name}[${indices.join('][')}]，得到 ${this.describeValue(value)}。`, { name: variable.name, indices: indices.join(','), value: this.describeValue(value) })
    return value
  }

  private resolveTarget(node: ExpressionNode): RuntimeVariable {
    if (node.kind !== 'identifier') this.fail(node.line, '这里需要一个普通变量。')
    return this.lookup(node.name, node.line)
  }

  private readVariable(name: string, line: number): RuntimeValue {
    const variable = this.lookup(name, line)
    variable.read = true
    this.emit('variable_read', line, `读取变量 ${name}，它的值是 ${this.describeValue(variable.value)}。`, { name, value: this.describeValue(variable.value) })
    return clone(variable.value)
  }

  private declareVariable(name: string, dataType: PrimitiveType, value: RuntimeValue, line: number) {
    this.putVariable(name, dataType, value, line)
    this.emit(dataType === 'string' ? 'string_declare' : 'variable_declare', line, `创建变量 ${name}，初始值是 ${this.describeValue(value)}。`, { name, dataType, value: this.describeValue(value) })
  }

  private putVariable(name: string, dataType: PrimitiveType, value: RuntimeValue, line: number) {
    const scope = this.currentScope()
    if (scope.variables.has(name)) this.fail(line, `变量 ${name} 在当前作用域已经创建过了，换一个名字或直接给它赋值。`)
    scope.variables.set(name, { name, dataType, value, scopeId: scope.id, scopeName: scope.name, changed: true })
  }

  private lookup(name: string, line: number) {
    for (let index = this.scopes.length - 1; index >= 0; index -= 1) {
      const variable = this.scopes[index].variables.get(name)
      if (variable) return variable
    }
    this.fail(line, `第 ${line} 行使用了变量 ${name}，但程序之前没有创建它。`)
  }

  private buildArrayValues(initializer: InitializerNode | ExpressionNode | undefined, type: PrimitiveType, length: number, line: number) {
    const values = Array.from({ length }, () => this.defaultValue(type))
    if (!initializer) return values
    if (type === 'char' && initializer.kind === 'literal' && initializer.literalType === 'string') {
      const characters = Array.from(String(initializer.value))
      if (characters.length >= length) this.fail(line, `字符数组长度至少需要是 ${characters.length + 1}，才能保存内容和结尾的空字符。`)
      characters.forEach((character, index) => { values[index] = { kind: 'char', value: character } })
      return values
    }
    if (initializer.kind !== 'initializer') this.fail(line, '数组初始化需要使用 { }，例如 {1, 2, 3}。')
    initializer.values.slice(0, length).forEach((item, index) => {
      if (item.kind === 'initializer') this.fail(item.line, '一维数组的每个位置需要是一个普通值。')
      values[index] = this.coerce(this.evaluateCompacted(item).value, type, item.line)
    })
    return values
  }

  private buildMatrixValues(initializer: InitializerNode | ExpressionNode | undefined, type: PrimitiveType, rows: number, columns: number, line: number) {
    const values = Array.from({ length: rows }, () => Array.from({ length: columns }, () => this.defaultValue(type)))
    if (!initializer) return values
    if (initializer.kind !== 'initializer') this.fail(line, '二维数组初始化需要使用嵌套的 { }。')
    initializer.values.slice(0, rows).forEach((row, rowIndex) => {
      if (row.kind !== 'initializer') this.fail(row.line, '二维数组的每一行都需要放在 { } 里。')
      row.values.slice(0, columns).forEach((item, columnIndex) => {
        if (item.kind === 'initializer') this.fail(item.line, '二维数组单元格需要是普通值。')
        values[rowIndex][columnIndex] = this.coerce(this.evaluateCompacted(item).value, type, item.line)
      })
    })
    return values
  }

  private readContainerValue(container: RuntimeValue, indices: number[], line: number, name: string): RuntimeValue {
    if (container.kind === 'array') {
      if (indices.length !== 1) this.fail(line, `数组 ${name} 需要一个下标。`)
      this.checkIndex(indices[0], container.length, line, name)
      return clone(container.values[indices[0]])
    }
    if (container.kind === 'array2d') {
      if (indices.length !== 2) this.fail(line, `二维数组 ${name} 需要两个下标，例如 ${name}[i][j]。`)
      this.checkIndex(indices[0], container.rows, line, name)
      this.checkIndex(indices[1], container.columns, line, name)
      return clone(container.values[indices[0]][indices[1]])
    }
    if (container.kind === 'string') {
      if (indices.length !== 1) this.fail(line, `字符串 ${name} 需要一个下标。`)
      this.checkIndex(indices[0], container.value.length, line, name)
      return { kind: 'char', value: container.value[indices[0]] }
    }
    this.fail(line, `${name} 不是数组或字符串，不能用 [ ] 访问。`)
  }

  private writeContainerValue(container: RuntimeValue, indices: number[], value: RuntimeValue, line: number, name: string) {
    if (container.kind === 'array') { this.checkIndex(indices[0], container.length, line, name); container.values[indices[0]] = value; return }
    if (container.kind === 'array2d') { this.checkIndex(indices[0], container.rows, line, name); this.checkIndex(indices[1], container.columns, line, name); container.values[indices[0]][indices[1]] = value; return }
    if (container.kind === 'string') {
      this.checkIndex(indices[0], container.value.length, line, name)
      const character = this.describeValue(value)
      if (character.length !== 1) this.fail(line, '给字符串单个位置赋值时，需要放入一个字符。')
      container.value = `${container.value.slice(0, indices[0])}${character}${container.value.slice(indices[0] + 1)}`
      return
    }
    this.fail(line, `${name} 不能通过下标写入。`)
  }

  private applyCompound(previous: RuntimeValue, value: RuntimeValue, operator: '+=' | '-=', line: number) {
    const result = this.asNumber(previous, line) + (operator === '+=' ? 1 : -1) * this.asNumber(value, line)
    return this.numberValue(result, line)
  }

  private readFacts(facts: CompactTraceFact[]) {
    const values = new Map<string, string>()
    facts.forEach((fact) => {
      if (fact.type === 'function_return') {
        const expression = typeof fact.data?.expression === 'string' ? fact.data.expression : ''
        const value = fact.data?.value
        if (expression && (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean')) {
          values.set(expression, `${expression} = ${String(value)}`)
        }
        return
      }
      if (!['variable_read', 'array_read', 'array_2d_read', 'string_read'].includes(fact.type)) return
      const expression = typeof fact.data?.expression === 'string' ? fact.data.expression : ''
      const name = typeof fact.data?.name === 'string' ? fact.data.name : ''
      const value = fact.data?.value
      if ((!name && !expression) || (typeof value !== 'string' && typeof value !== 'number' && typeof value !== 'boolean')) return
      if (expression) {
        values.set(expression, `${expression} = ${String(value)}`)
        return
      }
      const indices = typeof fact.data?.indices === 'string' && fact.data.indices ? `[${fact.data.indices.split(',').join('][')}]` : ''
      values.set(`${name}${indices}`, `${name}${indices} = ${String(value)}`)
    })
    return [...values.values()]
  }

  private decisionDescription(expression: string, result: boolean, values: string[], action: string) {
    return `${values.length ? `${values.join('，')}；` : ''}判断 ${expression}，结果为${result ? '真' : '假'}，${action}。`
  }

  private pushScope(name: string, line = this.currentLine) {
    const scope = { id: `scope-${++this.scopeCounter}`, name, variables: new Map<string, RuntimeVariable>() }
    this.scopes.push(scope)
    this.currentLine = line
  }

  private popScope(line: number) {
    this.scopes.pop()
    this.currentLine = line
  }

  private currentScope() {
    const scope = this.scopes[this.scopes.length - 1]
    if (!scope) this.fail(this.currentLine, '运行环境还没有准备好。')
    return scope
  }

  private emit(type: BaseTraceEvent['type'], line: number, description: string, data: BaseTraceEvent['data'] = {}) {
    this.currentLine = line
    if (this.compactFacts && compactableEventTypes.has(type)) {
      this.compactFacts.push({ type, line, description, data })
      return
    }
    if (++this.eventCounter > this.limits.maxSteps) this.fail(line, `程序执行步骤超过 ${this.limits.maxSteps}，可能存在无法结束的循环。`)
    this.events.push({ id: this.eventCounter, type, line, description, scopeId: this.scopes[this.scopes.length - 1]?.id ?? 'program', data })
    this.snapshots.push(this.snapshot())
    this.scopes.forEach((scope) => scope.variables.forEach((variable) => { variable.changed = false; variable.read = false; variable.previousValue = undefined }))
  }

  private snapshot(): ExecutionSnapshot {
    const variables = this.scopes.flatMap((scope) => Array.from(scope.variables.values()).map((item) => clone(item)))
    return { variables, callStack: clone(this.callStack), scopes: this.scopes.map((item) => ({ id: item.id, name: item.name })), consoleOutput: [...this.consoleOutput], currentLine: this.currentLine, loops: clone(this.loops) }
  }

  private defaultValue(type: PrimitiveType): RuntimeValue {
    if (type === 'int') return { kind: 'int', value: 0 }
    if (type === 'double') return { kind: 'double', value: 0 }
    if (type === 'bool') return { kind: 'bool', value: false }
    if (type === 'char') return { kind: 'char', value: '\0' }
    if (type === 'string') return { kind: 'string', value: '' }
    return voidValue()
  }

  private literalValue(node: Extract<ExpressionNode, { kind: 'literal' }>): RuntimeValue { return node.literalType === 'int' ? { kind: 'int', value: Number(node.value) } : node.literalType === 'double' ? { kind: 'double', value: Number(node.value) } : node.literalType === 'bool' ? { kind: 'bool', value: Boolean(node.value) } : node.literalType === 'char' ? { kind: 'char', value: String(node.value) } : { kind: 'string', value: String(node.value) } }
  private coerce(value: RuntimeValue, type: PrimitiveType, line: number): RuntimeValue {
    if (type === 'int') return { kind: 'int', value: Math.trunc(this.asNumber(value, line)) }
    if (type === 'double') return { kind: 'double', value: this.asNumber(value, line) }
    if (type === 'bool') return { kind: 'bool', value: this.asBoolean(value) }
    if (type === 'char') {
      if (value.kind === 'int' || value.kind === 'double') return { kind: 'char', value: String.fromCharCode(Math.trunc(value.value)) }
      const character = this.describeValue(value)
      if (character.length !== 1) this.fail(line, 'char 变量只能保存一个字符。')
      return { kind: 'char', value: character }
    }
    if (type === 'string') return { kind: 'string', value: this.describeValue(value) }
    return voidValue()
  }
  private numberValue(value: number, line: number): RuntimeValue { if (!Number.isFinite(value)) this.fail(line, '计算结果不是一个有效数字。'); return Number.isInteger(value) ? { kind: 'int', value } : { kind: 'double', value } }
  private asNumber(value: RuntimeValue, line: number) { if (value.kind === 'int' || value.kind === 'double') return value.value; if (value.kind === 'char') return value.value.charCodeAt(0); this.fail(line, `这里需要数字，但拿到了 ${value.kind}。`) }
  private asInteger(value: RuntimeValue, line: number, label: string) { const result = this.asNumber(value, line); if (!Number.isInteger(result)) this.fail(line, `${label}需要是整数。`); return result }
  private asBoolean(value: RuntimeValue) { return value.kind === 'bool' ? value.value : value.kind === 'int' || value.kind === 'double' ? value.value !== 0 : value.kind === 'string' ? value.value.length > 0 : false }
  private primitive(value: RuntimeValue): string | number | boolean { return value.kind === 'int' || value.kind === 'double' || value.kind === 'bool' || value.kind === 'char' || value.kind === 'string' ? value.value : value.kind }
  private orderedPrimitive(value: RuntimeValue): number | string { if (value.kind === 'char') return value.value.charCodeAt(0); if (value.kind === 'int' || value.kind === 'double') return value.value; if (value.kind === 'bool') return value.value ? 1 : 0; if (value.kind === 'string') return value.value; return value.kind }
  private describeValue(value: RuntimeValue): string { return value.kind === 'void' ? '无返回值' : value.kind === 'array' ? value.elementType === 'char' ? value.values.slice(0, value.values.findIndex((item) => item.kind === 'char' && item.value === '\0') < 0 ? value.length : value.values.findIndex((item) => item.kind === 'char' && item.value === '\0')).map((item) => item.kind === 'char' ? item.value : '').join('') : `[${value.values.map((item) => this.describeValue(item)).join(', ')}]` : value.kind === 'array2d' ? '二维数组' : String(value.value) }
  private elementType(value: RuntimeValue): PrimitiveType { return value.kind === 'array' || value.kind === 'array2d' ? value.elementType : value.kind === 'string' ? 'char' : this.fail(this.currentLine, '目标不是容器。') }
  private checkIndex(index: number, length: number, line: number, name: string) { if (index < 0 || index >= length) this.fail(line, `第 ${line} 行访问了 ${name}[${index}]。${name} 的有效下标是 0 到 ${length - 1}，这个下标已经越界。`) }
  private guardLoop(line: number, context: LoopContext) { if (context.iteration >= this.limits.maxLoopIterations) this.fail(line, `循环执行次数超过 ${this.limits.maxLoopIterations}，可能无法结束。`) }
  private checkTimeout(line: number) { if (Date.now() - this.startedAt > this.limits.timeoutMs) this.fail(line, `程序执行超过 ${this.limits.timeoutMs / 1000} 秒，已自动停止。`) }
  private expressionText(node: ExpressionNode | undefined): string { if (!node) return 'true'; if (node.kind === 'identifier') return node.name; if (node.kind === 'literal') return String(node.value); if (node.kind === 'binary') return `${this.expressionText(node.left)} ${node.operator} ${this.expressionText(node.right)}`; if (node.kind === 'unary') return node.postfix ? `${this.expressionText(node.argument)}${node.operator}` : `${node.operator}${this.expressionText(node.argument)}`; if (node.kind === 'array_access') return `${node.target.name}${node.indices.map((item) => `[${this.expressionText(item)}]`).join('')}`; if (node.kind === 'assignment') return `${this.expressionText(node.target)} ${node.operator} ${this.expressionText(node.value)}`; if (node.kind === 'call') return `${node.callee}(...)`; return `${node.target.name}.${node.member}()` }
  private parseInput(text: string, type: PrimitiveType, line: number): RuntimeValue { if (type === 'string') return { kind: 'string', value: text }; if (type === 'char') return this.coerce({ kind: 'char', value: text }, 'char', line); if (type === 'bool') return { kind: 'bool', value: text === 'true' || text === '1' }; const value = Number(text); if (!Number.isFinite(value)) this.fail(line, `输入 “${text}” 不是有效数字。`); return type === 'int' ? { kind: 'int', value: Math.trunc(value) } : { kind: 'double', value } }
  private fail(line: number, message: string): never { throw new TeachingRuntimeError({ line, message }) }
}

export function asRuntimeError(error: unknown): RuntimeErrorInfo {
  if (error instanceof TeachingRuntimeError) return error.info
  return { line: 1, message: error instanceof Error ? `执行时遇到问题：${error.message}` : '执行时遇到未知问题。' }
}
