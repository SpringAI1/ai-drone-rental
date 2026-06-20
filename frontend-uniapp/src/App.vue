<script setup lang="ts">
import { onLaunch, onAppShow, onAppHide } from '@dcloudio/uni-app'

	onLaunch(() => {
		console.log('无人机租赁系统启动')
	})

	onAppShow(() => {
		console.log('App Show')
	})

	onAppHide(() => {
		console.log('App Hide')
	})
</script>

<style lang="scss">
	@import './uni.scss';

	page {
		background-color: #f1f5f9;
		font-family: $font-family;
		font-size: $font-size-base;
		color: $text-primary;
	}

	.uni-row {
		flex-direction: row;
	}

	.uni-column {
		flex-direction: column;
	}

	// ================= 全局工具类 =================

	// 文字颜色
	.text-primary { color: $text-primary !important; }
	.text-secondary { color: $text-secondary !important; }
	.text-muted { color: $text-muted !important; }
	.text-brand { color: $electric-blue !important; }
	.text-success { color: $neon-success !important; }
	.text-warning { color: $neon-warning !important; }
	.text-error { color: $neon-error !important; }

	// 背景色
	.bg-white { background-color: #ffffff !important; }
	.bg-slate-700 { background-color: #f1f5f9 !important; }
	.bg-slate-800 { background-color: #ffffff !important; }
	.bg-glass {
		background: rgba(255, 255, 255, 0.9) !important;
	}

	// 文字大小
	.text-xs { font-size: $font-size-xs !important; }
	.text-sm { font-size: $font-size-sm !important; }
	.text-base { font-size: $font-size-base !important; }
	.text-lg { font-size: $font-size-lg !important; }
	.text-xl { font-size: $font-size-xl !important; }
	.text-2xl { font-size: $font-size-2xl !important; }
	.text-3xl { font-size: $font-size-3xl !important; }
	.text-4xl { font-size: $font-size-4xl !important; }

	// 文字权重
	.font-normal { font-weight: $font-weight-normal !important; }
	.font-medium { font-weight: $font-weight-medium !important; }
	.font-semibold { font-weight: $font-weight-semibold !important; }
	.font-bold { font-weight: $font-weight-bold !important; }

	// 间距
	.p-0 { padding: 0 !important; }
	.p-sm { padding: $spacing-sm !important; }
	.p-md { padding: $spacing-md !important; }
	.p-lg { padding: $spacing-lg !important; }
	.p-xl { padding: $spacing-xl !important; }

	.m-0 { margin: 0 !important; }
	.m-sm { margin: $spacing-sm !important; }
	.m-md { margin: $spacing-md !important; }
	.m-lg { margin: $spacing-lg !important; }
	.m-xl { margin: $spacing-xl !important; }

	.mb-sm { margin-bottom: $spacing-sm !important; }
	.mb-md { margin-bottom: $spacing-md !important; }
	.mb-lg { margin-bottom: $spacing-lg !important; }
	.mb-xl { margin-bottom: $spacing-xl !important; }

	.mt-sm { margin-top: $spacing-sm !important; }
	.mt-md { margin-top: $spacing-md !important; }
	.mt-lg { margin-top: $spacing-lg !important; }
	.mt-xl { margin-top: $spacing-xl !important; }

	// Flexbox
	.flex { display: flex !important; }
	.flex-col { flex-direction: column !important; }
	.flex-wrap { flex-wrap: wrap !important; }
	.items-center { align-items: center !important; }
	.items-start { align-items: flex-start !important; }
	.items-end { align-items: flex-end !important; }
	.justify-center { justify-content: center !important; }
	.justify-between { justify-content: space-between !important; }
	.justify-end { justify-content: flex-end !important; }
	.gap-sm { gap: $spacing-sm !important; }
	.gap-md { gap: $spacing-md !important; }
	.gap-lg { gap: $spacing-lg !important; }

	// ================= 卡片基础样式 =================
	.glass-panel {
		background: #ffffff;
		border: 1rpx solid #e2e8f0;
		border-radius: $radius-3xl;
		box-shadow: $shadow-md;
	}

	// ================= 品牌效果 =================
	.glow-blue {
		box-shadow: $shadow-glow-blue;
	}

	.glow-success {
		box-shadow: 0 8rpx 28rpx $neon-success-glow;
	}

	.glow-warning {
		box-shadow: 0 8rpx 28rpx $neon-warning-glow;
	}

	.glow-error {
		box-shadow: 0 8rpx 28rpx $neon-error-glow;
	}

	// ================= 页面容器 =================
	.page-container {
		padding: $spacing-lg;
		min-height: 100vh;
	}

	.page-title {
		font-size: $font-size-2xl;
		font-weight: $font-weight-bold;
		color: $text-primary;
		margin-bottom: $spacing-lg;
	}

	.page-subtitle {
		font-size: $font-size-sm;
		color: $text-secondary;
		margin-top: $spacing-xs;
	}
</style>