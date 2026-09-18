import { createApp } from 'vue';
import App from './ui/App.vue';
import './ui/styles.css';
import { initializeSettings } from './ui/settings';

void initializeSettings().then(() => {
  createApp(App).mount('#app');
});
