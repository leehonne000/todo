<template>
  <div class="page page-narrow">
    <TopBar title="文章" back-to="/info" />

    <article v-if="article" class="article glass">
      <h1 class="title">{{ article.title }}</h1>
      <p class="date dim num">{{ (article.publishedAt || '').replace('T', ' ').slice(0, 10) }}<span v-if="article.authorName" class="author"> · {{ article.authorName }}</span></p>
      <img v-if="article.coverUrl" class="cover" :src="article.coverUrl" alt="" />
      <div class="content">{{ article.content }}</div>
    </article>
    <p v-else class="empty dim">加载中…</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import TopBar from '@/components/TopBar.vue'
import { getArticle } from '@/api'
import { toast } from '@/utils/toast'

const route = useRoute()
const article = ref(null)

onMounted(async () => {
  try {
    article.value = await getArticle(route.params.id)
  } catch (e) {
    toast(e.message)
  }
})
</script>

<style scoped>
.article { width: 100%; padding: 28px 30px; }
.title { font-size: 22px; font-weight: 500; line-height: 1.5; letter-spacing: 1px; }
.date { margin: 10px 0 18px; font-size: 12.5px; }
.author { color: var(--theme-text); }
.cover {
  width: 100%;
  max-height: 280px;
  object-fit: cover;
  border-radius: 12px;
  margin-bottom: 20px;
}
.content {
  font-size: 15.5px;
  line-height: 2;
  white-space: pre-wrap;
  color: rgba(255, 255, 255, 0.92);
}
.empty { text-align: center; padding: 40px 0; }
</style>
