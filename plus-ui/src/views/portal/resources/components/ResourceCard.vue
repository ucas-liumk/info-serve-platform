<template>
  <article class="resource-card" @click="emit('preview', resource)">
    <div class="preview-stack" aria-hidden="true">
      <div :class="['preview-sheet', 'sheet-back', typeClass]"></div>
      <div :class="['preview-sheet', 'sheet-mid', typeClass]"></div>
      <div :class="['preview-sheet', 'sheet-front', typeClass]">
        <img v-if="thumbnailUrl" :src="thumbnailUrl" :alt="resource.title" @error="thumbnailBroken = true" />
        <template v-else>
          <span class="cover-ribbon">{{ resource.categoryName || '资源共享' }}</span>
          <strong>{{ typeLabel }}</strong>
          <em>{{ coverTitle }}</em>
        </template>
      </div>
    </div>

    <div class="resource-body">
      <button class="title-button" type="button" @click.stop="emit('preview', resource)">
        {{ resource.title }}
      </button>
      <p>简介：{{ resource.description || resource.originalName || '暂无简介' }}</p>
      <div class="resource-meta">
        <span>大小：{{ formatSize(resource.fileSize) }}</span>
        <i></i>
        <span>浏览：{{ resource.viewCount || 0 }}次</span>
      </div>
      <div class="rating-row">
        <span>星级：</span>
        <span class="stars">
          <el-icon v-for="item in 5" :key="item" :class="{ active: item <= rating }"><StarFilled /></el-icon>
        </span>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { StarFilled } from '@element-plus/icons-vue';
import { resourceThumbnailUrl } from '@/api/infoservice/portal';
import type { InfoResource } from '@/api/infoservice/types';
import { getToken } from '@/utils/auth';

const props = defineProps<{
  resource: InfoResource;
}>();

const emit = defineEmits<{
  (e: 'preview', resource: InfoResource): void;
  (e: 'download', resource: InfoResource): void;
}>();

const thumbnailBroken = ref(false);

const typeClass = computed(() => props.resource.previewType || 'file');

const typeLabel = computed(() => {
  const suffix = props.resource.fileSuffix || props.resource.previewType || 'FILE';
  return suffix.toUpperCase().slice(0, 5);
});

const coverTitle = computed(() => {
  const title = props.resource.title || props.resource.originalName || '资料预览';
  return title.length > 9 ? `${title.slice(0, 9)}...` : title;
});

const thumbnailUrl = computed(() => {
  if (thumbnailBroken.value) {
    return '';
  }
  const target = new URL(resourceThumbnailUrl(props.resource.resourceId), window.location.origin);
  const token = getToken();
  if (token) {
    target.searchParams.set('Authorization', `Bearer ${token}`);
  }
  target.searchParams.set('clientid', import.meta.env.VITE_APP_CLIENT_ID);
  return target.toString();
});

const rating = computed(() => {
  const hotScore = (props.resource.viewCount || 0) + (props.resource.downloadCount || 0) * 3;
  if (hotScore >= 1000) return 5;
  if (hotScore >= 300) return 4;
  if (hotScore >= 80) return 3;
  if (hotScore > 0) return 2;
  return 1;
});

watch(
  () => props.resource.resourceId,
  () => {
    thumbnailBroken.value = false;
  }
);

const formatSize = (size?: number) => {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
};
</script>

<style scoped>
.resource-card {
  min-height: 188px;
  display: grid;
  grid-template-columns: 126px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  border: 1px solid #e3e8f0;
  border-radius: 8px;
  padding: 14px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(20, 36, 67, 0.04);
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.resource-card:hover {
  border-color: #c5d5f2;
  box-shadow: 0 14px 30px rgba(20, 36, 67, 0.08);
  transform: translateY(-1px);
}

.preview-stack {
  position: relative;
  width: 126px;
  height: 142px;
}

.preview-sheet {
  position: absolute;
  overflow: hidden;
  border: 1px solid #dfe5ee;
  border-radius: 7px;
  background: #fff;
  box-shadow: 0 10px 24px rgba(20, 36, 67, 0.12);
}

.preview-sheet::before {
  content: '';
  position: absolute;
  inset: 41% 0 auto;
  height: 38px;
  background: linear-gradient(135deg, #f6d34a 0%, #f4a812 46%, #f75f3a 100%);
}

.preview-sheet.office::before {
  background: linear-gradient(135deg, #ffdf56 0%, #ff9d2d 44%, #ff5c57 100%);
}

.preview-sheet.pdf::before,
.preview-sheet.ofd::before {
  background: linear-gradient(135deg, #fff1bd 0%, #ff5a4f 48%, #b51027 100%);
}

.preview-sheet.image::before {
  background: linear-gradient(135deg, #6ad6ff 0%, #2f8df5 48%, #2651d6 100%);
}

.preview-sheet.video::before,
.preview-sheet.audio::before {
  background: linear-gradient(135deg, #9ae6c5 0%, #2fb981 48%, #1680a8 100%);
}

.preview-sheet.text::before {
  background: linear-gradient(135deg, #e6edf7 0%, #aebbd0 48%, #64748b 100%);
}

.sheet-back {
  left: 0;
  top: 34px;
  width: 82px;
  height: 82px;
  opacity: 0.68;
}

.sheet-mid {
  left: 16px;
  top: 20px;
  width: 94px;
  height: 104px;
  opacity: 0.84;
}

.sheet-front {
  left: 32px;
  top: 0;
  width: 94px;
  height: 132px;
  display: grid;
  grid-template-rows: 1fr auto auto 1fr;
  justify-items: center;
  color: #10223f;
}

.sheet-front img {
  position: relative;
  z-index: 2;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-ribbon,
.sheet-front strong,
.sheet-front em {
  position: relative;
  z-index: 1;
}

.cover-ribbon {
  grid-row: 2;
  min-width: 66px;
  max-width: 78px;
  overflow: hidden;
  border-radius: 999px;
  padding: 3px 8px;
  background: rgba(255, 255, 255, 0.82);
  color: #405273;
  font-size: 10px;
  font-weight: 800;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sheet-front strong {
  grid-row: 3;
  margin-top: 6px;
  color: #10223f;
  font-size: 17px;
  font-weight: 950;
  letter-spacing: 0;
}

.sheet-front em {
  max-width: 76px;
  margin-top: 5px;
  overflow: hidden;
  color: #56657e;
  font-size: 10px;
  font-style: normal;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-body {
  min-width: 0;
}

.title-button {
  width: 100%;
  overflow: hidden;
  border: 0;
  padding: 0;
  background: transparent;
  color: #10223f;
  font-size: 16px;
  line-height: 1.34;
  font-weight: 700;
  letter-spacing: 0;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.title-button:hover {
  color: #1260e8;
}

.resource-card p {
  min-height: 40px;
  margin: 8px 0 0;
  display: -webkit-box;
  overflow: hidden;
  color: #344762;
  font-size: 13px;
  line-height: 1.55;
  font-weight: 400;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.resource-meta {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  color: #8a97af;
  font-size: 12px;
  font-weight: 400;
}

.resource-meta i {
  width: 1px;
  height: 13px;
  background: #b6c0cf;
}

.rating-row {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #8a97af;
  font-size: 12px;
}

.stars {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: #dfe3e8;
  font-size: 15px;
}

.stars .active {
  color: #ff9800;
}

@media (max-width: 1540px) {
  .resource-card {
    min-height: 180px;
    grid-template-columns: 112px minmax(0, 1fr);
    gap: 10px;
    padding: 12px;
  }

  .preview-stack {
    transform: scale(0.88);
    transform-origin: left center;
  }

  .title-button {
    font-size: 15px;
  }

  .resource-card p {
    font-size: 12px;
  }
}

@media (max-width: 620px) {
  .resource-card {
    grid-template-columns: 126px minmax(0, 1fr);
    padding: 14px;
  }
}
</style>
