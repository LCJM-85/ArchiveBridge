<template>
  <Login v-if="!token" />
  <AppLayout v-else />
</template>

<script setup>
import { storeToRefs } from 'pinia'
import Login from '@/views/login/Login.vue'
import AppLayout from '@/layouts/AppLayout.vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const { token } = storeToRefs(userStore)
</script>
