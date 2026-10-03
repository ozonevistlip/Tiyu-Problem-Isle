import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { listPetsApi, type PetInfo } from '@/api/pets'
import { useUserStore } from './user'

export const usePetCatalogStore = defineStore('petCatalog', () => {
  const pets = ref<PetInfo[]>([])
  const selectedId = ref<number | null>(null)
  const loadedFor = ref<number | null>(null)
  const loading = ref(false)
  const user = useUserStore()
  const selected = computed(() => pets.value.find(pet => pet.id === selectedId.value && pet.owned && pet.hasAnimation) || null)

  async function refresh() {
    const userId = user.userInfo?.id
    if (!userId) { clear(); return }
    loading.value = true
    try {
      const next = await listPetsApi()
      if (user.userInfo?.id !== userId) return
      pets.value = next
      const saved = Number(localStorage.getItem(`cppkid:selected-pet:${userId}`))
      const candidate = loadedFor.value === userId ? selectedId.value : saved
      const usable = next.filter(pet => pet.owned && pet.hasAnimation)
      selectedId.value = usable.find(pet => pet.id === candidate)?.id ?? usable[0]?.id ?? null
      loadedFor.value = userId
    } finally { loading.value = false }
  }

  function select(id: number) {
    if (!pets.value.some(pet => pet.id === id && pet.owned && pet.hasAnimation)) return
    selectedId.value = id
    if (user.userInfo?.id) localStorage.setItem(`cppkid:selected-pet:${user.userInfo.id}`, String(id))
  }

  function clear() { pets.value = []; selectedId.value = null; loadedFor.value = null }
  window.addEventListener('cppkid-auth-cleared', clear)
  return { pets, selectedId, selected, loading, refresh, select, clear }
})
