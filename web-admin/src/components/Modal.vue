<template>
  <!-- 玻璃拟态对话框（删除确认 / 硬节点表单共用） -->
  <transition name="fade">
    <div v-if="modelValue" class="modal-mask" @click.self="$emit('update:modelValue', false)">
      <div class="modal glass-strong">
        <h3 class="modal-title">{{ title }}</h3>
        <div class="modal-body"><slot /></div>
        <div class="modal-actions">
          <button class="btn-glass" type="button" @click="$emit('update:modelValue', false)">{{ cancelText }}</button>
          <button class="btn-amber" type="button" :disabled="confirmDisabled" @click="$emit('confirm')">{{ confirmText }}</button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '' },
  confirmText: { type: String, default: '确定' },
  cancelText: { type: String, default: '取消' },
  confirmDisabled: { type: Boolean, default: false }
})
defineEmits(['update:modelValue', 'confirm'])
</script>

<style scoped>
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(8, 10, 22, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.modal {
  width: 100%;
  max-width: 460px;
  padding: 26px 26px 20px;
}
.modal-title {
  font-size: 18px;
  font-weight: 500;
  letter-spacing: 1px;
  margin-bottom: 16px;
}
.modal-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.modal-actions {
  margin-top: 22px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.modal-actions .btn-glass,
.modal-actions .btn-amber {
  padding: 10px 24px;
  font-size: 14px;
}
</style>
