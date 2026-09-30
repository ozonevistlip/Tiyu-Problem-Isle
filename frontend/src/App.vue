<template>
  <SiteAnnouncementBanner />
  <router-view v-slot="{ Component }">
    <transition name="page-fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<script setup lang="ts">
import SiteAnnouncementBanner from '@/components/SiteAnnouncementBanner.vue'
</script>
