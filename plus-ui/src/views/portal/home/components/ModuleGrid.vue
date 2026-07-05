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
  max-width: var(--portal-max);
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin: 190px auto 0;
}

.module-card {
  position: relative;
  min-height: 292px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 18px 18px 20px;
  border: 1px solid var(--ip-neutral-200);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(248, 252, 255, 0.94) 100%);
  box-shadow: var(--ip-shadow-md);
  color: var(--portal-blue);
  cursor: pointer;
  transition:
    transform var(--ip-motion-base) var(--ip-motion-ease),
    box-shadow var(--ip-motion-base) var(--ip-motion-ease),
    border-color var(--ip-motion-base) var(--ip-motion-ease);
}

.module-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: radial-gradient(circle at 50% 18%, rgba(54, 142, 255, 0.12), transparent 46%);
  opacity: 0;
  transition: opacity 0.18s ease;
  pointer-events: none;
}

.module-card:hover {
  transform: translateY(-3px);
  border-color: var(--ip-primary-200);
  box-shadow: var(--ip-shadow-lg);
}

.module-card:hover::before {
  opacity: 1;
}

.module-visual {
  width: 148px;
  height: 148px;
  display: block;
  margin-bottom: 12px;
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
  color: var(--ip-primary-900);
  font-size: 24px;
  line-height: 1.15;
  font-weight: 700;
}

.module-desc {
  position: relative;
  z-index: 1;
  margin-top: 10px;
  color: var(--ip-neutral-600);
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
  border-radius: 999px;
  background: var(--ip-primary-600);
  color: var(--ip-neutral-0);
  line-height: 0;
  box-shadow:
    inset 0 1px 2px rgba(255, 255, 255, 0.72),
    inset 0 -4px 7px rgba(20, 91, 204, 0.36),
    var(--ip-shadow-md);
}

.module-action svg {
  width: 21px;
  height: 21px;
  color: var(--ip-neutral-0);
  transform: translateX(1px);
  filter: drop-shadow(0 1px 1px rgba(7, 61, 148, 0.2));
}

@media (max-width: 1460px) {
  .module-grid {
    gap: 16px;
    margin-top: 112px;
  }

  .module-card {
    min-height: 276px;
    padding-top: 16px;
  }

  .module-visual {
    width: 148px;
    height: 148px;
  }

  .module-title {
    font-size: 20px;
  }

  .module-desc {
    font-size: 14px;
  }
}

@media (max-width: 1180px) {
  .module-grid {
    margin-top: 96px;
  }

  .module-card {
    min-height: 260px;
    padding: 14px 14px 18px;
  }

  .module-visual {
    width: 124px;
    height: 124px;
  }

  .module-title {
    font-size: 20px;
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
    grid-template-columns: repeat(3, minmax(180px, 1fr));
    margin-top: 64px;
  }

  .module-card {
    min-height: 236px;
  }

  .module-visual {
    width: 112px;
    height: 112px;
    margin-bottom: 10px;
  }

  .module-title {
    font-size: 20px;
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
    min-height: 232px;
  }
}
</style>
