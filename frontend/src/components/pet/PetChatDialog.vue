<template>
  <el-dialog :model-value="modelValue" :title="`与${petName}聊天`" width="min(560px, 94vw)" @update:model-value="emit('update:modelValue', $event)">
    <div ref="listElement" class="chat-list" aria-live="polite">
      <p v-if="!messages.length" class="chat-empty">打个招呼吧。当前会话每只宠物只保留最近 20 条消息。</p>
      <div v-for="(message, index) in messages" :key="index" class="chat-message" :class="message.role">
        <strong>{{ message.role === 'user' ? '我' : petName }}</strong><p>{{ message.content }}</p>
      </div>
    </div>
    <div class="chat-compose">
      <el-input v-model="input" type="textarea" :rows="2" maxlength="2000" show-word-limit placeholder="想对宠物说什么？" @keydown.ctrl.enter.prevent="send" />
      <el-button type="primary" :loading="sending" :disabled="!input.trim()" @click="send">发送</el-button>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { petChatApi, type ChatMessage } from '@/api/pets'
import { useUserStore } from '@/stores/user'
import { usePet } from '@/composables/usePet'

const sessions = new Map<string, ChatMessage[]>()
let sessionEpoch = 0
function clearSessions() { sessionEpoch++; sessions.clear() }
window.addEventListener('cppkid-auth-expired', clearSessions)
window.addEventListener('cppkid-auth-cleared', clearSessions)

const props = defineProps<{ modelValue: boolean; petId: number; petName: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
const user = useUserStore()
const pet = usePet()
const input = ref('')
const sending = ref(false)
const listElement = ref<HTMLElement>()
const key = computed(() => `${user.userInfo?.id || 0}:${props.petId}`)
const messages = ref<ChatMessage[]>([])
watch(key, () => { messages.value = sessions.get(key.value) || []; input.value = '' }, { immediate: true })
watch(() => props.modelValue, async open => {
  if (open) { messages.value = sessions.get(key.value) || []; await nextTick(); scrollBottom() }
})

function scrollBottom() { if (listElement.value) listElement.value.scrollTop = listElement.value.scrollHeight }
async function send() {
  const content = input.value.trim()
  if (!content || sending.value) return
  const sessionKey = key.value
  const requestEpoch = sessionEpoch
  const next: ChatMessage[] = [...messages.value, { role: 'user' as const, content }].slice(-20)
  messages.value = next; sessions.set(sessionKey, next); input.value = ''; sending.value = true
  pet.thinking()
  await nextTick(); scrollBottom()
  try {
    const result = await petChatApi(props.petId, next)
    if (requestEpoch !== sessionEpoch) return
    const updated = [...next, { role: 'assistant' as const, content: result.reply }].slice(-20)
    sessions.set(sessionKey, updated)
    if (key.value === sessionKey) messages.value = updated
    pet.showMessage(result.reply.slice(0, 80), { bubbleType: 'tip', duration: 5000 })
    await nextTick(); scrollBottom()
  } catch {
    if (requestEpoch !== sessionEpoch) return
    sessions.set(sessionKey, next.slice(0, -1))
    if (key.value === sessionKey) messages.value = sessions.get(sessionKey) || []
    ElMessage.warning('消息发送失败，请重试')
  } finally { sending.value = false }
}
</script>

<style scoped>
.chat-list { height: min(48vh, 420px); overflow: auto; display: flex; flex-direction: column; gap: 12px; padding: 12px; background: var(--surface-soft); border-radius: 12px; }
.chat-empty { text-align: center; color: var(--text-muted); margin: auto; }
.chat-message { max-width: 82%; padding: 10px 12px; border-radius: 14px; background: var(--surface-card); white-space: pre-wrap; }
.chat-message.user { align-self: flex-end; background: color-mix(in srgb, var(--color-primary), white 84%); }
.chat-message strong { font-size: 12px; color: var(--text-muted); }
.chat-message p { margin: 4px 0 0; line-height: 1.5; overflow-wrap: anywhere; }
.chat-compose { display: flex; align-items: flex-end; gap: 10px; margin-top: 14px; }
</style>
