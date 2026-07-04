<template>
  <nav class="module-grid" aria-label="数智服务入口">
    <button v-for="item in modules" :key="item.title" class="module-card" type="button" @click="openModule(item)">
      <span class="module-visual">
        <img :src="item.image" :alt="item.title" />
      </span>
      <span class="module-title">{{ item.title }}</span>
      <span class="module-desc">{{ item.desc }}</span>
      <span class="module-action" aria-hidden="true">
        <IconArrowRight />
      </span>
    </button>
  </nav>
</template>

<script setup lang="ts">
import IconArrowRight from '~icons/material-symbols/arrow-right-alt-rounded';

interface HomeModule {
  title: string;
  desc: string;
  image: string;
  path?: string;
}

defineProps<{ modules: HomeModule[] }>();
const emit = defineEmits<{ (e: 'open', item: HomeModule): void }>();

const openModule = (item: HomeModule) => emit('open', item);
</script>

<style scoped>
.module-grid {
  max-width: 1440px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 20px;
  margin: 248px auto 0;
}

.module-card {
  position: relative;
  height: 340px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 18px 20px 20px;
  border: 1px solid rgba(207, 224, 245, 0.92);
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(248, 252, 255, 0.94) 100%);
  box-shadow:
    0 18px 42px rgba(35, 90, 151, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.96);
  color: var(--portal-blue);
  cursor: pointer;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.module-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: radial-gradient(circle at 50% 18%, rgba(54, 142, 255, 0.16), transparent 46%);
  opacity: 0;
  transition: opacity 0.18s ease;
  pointer-events: none;
}

.module-card:hover {
  transform: translateY(-6px);
  border-color: rgba(122, 179, 244, 0.92);
  box-shadow:
    0 28px 58px rgba(32, 98, 180, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 1);
}

.module-card:hover::before {
  opacity: 1;
}

.module-visual {
  width: 166px;
  height: 166px;
  display: block;
  margin-bottom: 14px;
}

.module-visual img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: contain;
}

.module-title {
  position: relative;
  z-index: 1;
  color: var(--portal-blue);
  font-size: 28px;
  line-height: 1.15;
  font-weight: 800;
}

.module-desc {
  position: relative;
  z-index: 1;
  margin-top: 10px;
  color: var(--portal-muted);
  font-size: 16px;
  line-height: 1.35;
  font-weight: 600;
}

.module-action {
  position: relative;
  z-index: 1;
  width: 36px;
  height: 36px;
  min-width: 36px;
  min-height: 36px;
  flex: 0 0 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 14px;
  box-sizing: border-box;
  border-radius: 50%;
  background: linear-gradient(180deg, #4fb0ff 0%, #2e86f5 52%, #1f6fe5 100%);
  color: #fff;
  line-height: 0;
  box-shadow:
    inset 0 1px 2px rgba(255, 255, 255, 0.72),
    inset 0 -4px 7px rgba(20, 91, 204, 0.36),
    0 8px 13px rgba(36, 116, 236, 0.28);
}

.module-action svg {
  width: 21px;
  height: 21px;
  color: #fff;
  transform: translateX(1px);
  filter: drop-shadow(0 1px 1px rgba(7, 61, 148, 0.2));
}

@media (max-width: 1460px) {
  .module-grid {
    gap: 16px;
    margin-top: 128px;
  }

  .module-card {
    height: 318px;
    padding-top: 16px;
  }

  .module-visual {
    width: 148px;
    height: 148px;
  }

  .module-title {
    font-size: 25px;
  }

  .module-desc {
    font-size: 15px;
  }
}

@media (max-width: 1180px) {
  .module-grid {
    margin-top: 96px;
  }

  .module-card {
    height: 288px;
    padding: 14px 14px 18px;
  }

  .module-visual {
    width: 124px;
    height: 124px;
  }

  .module-title {
    font-size: 22px;
  }

  .module-desc {
    font-size: 14px;
  }

  .module-action {
    width: 34px;
    height: 34px;
    min-width: 34px;
    min-height: 34px;
    flex-basis: 34px;
    margin-top: 12px;
  }
}

@media (max-width: 1023px) {
  .module-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    margin-top: 64px;
  }

  .module-card {
    height: 252px;
  }

  .module-visual {
    width: 112px;
    height: 112px;
    margin-bottom: 10px;
  }

  .module-title {
    font-size: 21px;
  }
}

@media (max-width: 767px) {
  .module-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    margin-top: 48px;
  }
}

@media (max-width: 640px) {
  .module-grid {
    margin-top: 40px;
  }

  .module-card {
    height: 236px;
  }

  .module-visual {
    width: 104px;
    height: 104px;
  }

  .module-title {
    font-size: 20px;
  }
}

@media (max-width: 520px) {
  .module-grid {
    grid-template-columns: 1fr;
  }

  .module-card {
    height: 232px;
  }
}
</style>
