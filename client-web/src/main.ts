import { createApp } from 'vue';
import App from './ui/App.vue';
import './ui/styles.css';
import { initializeSettings } from './ui/settings';
import { initializeModAssets } from './core/services/ModAssetStore';

void Promise.all([initializeSettings(), initializeModAssets().catch((error: unknown) => {
  console.warn('Could not initialize browser mod storage.', error);
})]).then(() => {
  createApp(App).mount('#app');
});
