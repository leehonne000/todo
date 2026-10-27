<template>
  <!-- 统一「选一个」筛选样式：文字 + 主题色下划线（参考标签查询页） -->
  <div class="tabs">
    <button
      v-for="t in items" :key="String(t.value)"
      class="tab" :class="{ on: modelValue === t.value }"
      @click="$emit('update:modelValue', t.value)"
    >{{ t.label }}</button>
  </div>
</template>

<script setup>
defineProps({
  items: { type: Array, required: true }, // [{ value, label }]
  modelValue: { type: [String, Number, Object], default: null }
})
defineEmits(['update:modelValue'])
</script>

<style scoped>
.tabs {
  width: 100%;
  display: flex;
  gap: 26px;
  padding: 0 4px 2px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}
.tab {
  position: relative;
  padding: 10px 2px 12px;
  font-size: 15px;
  letter-spacing: 1px;
  color: var(--text-dim);
  white-space: nowrap;
  transition: color 0.15s;
}
.tab.on { color: var(--theme-text); }
.tab.on::after {
  content: '';
  position: absolute;
  left: 0; right: 0; bottom: -1px;
  height: 2.5px;
  border-radius: 2px;
  background: var(--theme-text);
}
</style>
