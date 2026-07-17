export const petDialogues = {
  welcome: [
    '欢迎回来，今天也一起学习吧！',
    '准备好开始今天的代码冒险了吗？',
    '我已经在这里等你啦！'
  ],
  encourage: [
    '写得不错，继续加油！',
    '已经完成很多啦！',
    '别着急，我们慢慢检查。'
  ],
  success: [
    '太棒了，代码运行成功！',
    '这一关完成啦！',
    '你的思路完全正确！'
  ],
  error: [
    '这里好像有一点小问题。',
    '再检查一下括号和分号吧。',
    '别担心，错误也是学习的一部分。',
    '可以看看变量是否已经定义。'
  ],
  idle: [
    '点击我看看会发生什么。',
    '我正在认真看你写代码。',
    '学习一会儿也要记得休息哦。'
  ]
} as const

export function pickPetDialogue(items: readonly string[], previous = '') {
  const candidates = items.filter((item) => item !== previous)
  const pool = candidates.length ? candidates : items
  return pool[Math.floor(Math.random() * pool.length)] || ''
}
