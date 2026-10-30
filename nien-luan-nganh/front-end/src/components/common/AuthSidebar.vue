<template>
  <aside class="auth-sidebar">
    <div class="sidebar-logo">
      <div class="logo-icon">
        <IconLogoMark :size="22" class="logo-svg" />
      </div>
      <span class="logo-text">bluespace.</span>
    </div>

    <div class="sidebar-content">
      <div class="sidebar-tag">
        <IconSun :size="14" class="tag-icon" />
        <span>KHÔNG GIAN GẮN KẾT</span>
      </div>

      <h1 class="sidebar-headline">
        Bắt đầu trò chuyện<br>để gần nhau hơn.
      </h1>

      <p class="sidebar-desc">
        Kết nối với bạn bè, chia sẻ khoảnh khắc và lưu giữ những điều ý nghĩa.
      </p>

      <ul class="sidebar-features">
        <li>
          <IconMessageSquare :size="16" class="feature-icon" />
          <span>Trò chuyện tức thì, liền mạch</span>
        </li>
        <li>
          <IconShield :size="16" class="feature-icon" />
          <span>An toàn và riêng tư</span>
        </li>
        <li>
          <IconBarChart :size="16" class="feature-icon" />
          <span>Hỗ trợ nhiều tính năng</span>
        </li>
      </ul>
    </div>
  </aside>
</template>

<script setup>
import IconLogoMark     from '@/components/icons/IconLogoMark.vue'
import IconSun          from '@/components/icons/IconSun.vue'
import IconMessageSquare from '@/components/icons/IconMessageSquare.vue'
import IconShield       from '@/components/icons/IconShield.vue'
import IconBarChart     from '@/components/icons/IconBarChart.vue'
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');

.auth-sidebar {
  font-family: "Quicksand", sans-serif;
  background: linear-gradient(145deg, #3b5bdb 0%, #1c3fa8 60%, #1a3299 100%);
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 36px 40px;
  position: relative;
  overflow: hidden;
}

.auth-sidebar::before {
  content: '';
  position: absolute;
  top: -80px;
  right: -80px;
  width: 300px;
  height: 300px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 50%;
}

.auth-sidebar::after {
  content: '';
  position: absolute;
  bottom: -100px;
  left: -60px;
  width: 350px;
  height: 350px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 50%;
}

/* Logo */
.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.logo-icon {
  width: 38px;
  height: 38px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-svg {
  color: white;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: white;
  letter-spacing: -0.3px;
}

.sidebar-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.sidebar-tag {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.tag-icon {
  color: rgba(255, 255, 255, 0.7);
}

.sidebar-tag span {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1.5px;
  color: rgba(255, 255, 255, 0.65);
  text-transform: uppercase;
}

.sidebar-headline {
  font-size: 38px;
  font-weight: 800;
  color: white;
  line-height: 1.2;
  margin: 0 0 20px 0;
}

.sidebar-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.7;
  margin: 0 0 36px 0;
  max-width: 340px;
}

.sidebar-features {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sidebar-features li {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
}

.feature-icon {
  color: rgba(255, 255, 255, 0.8);
  flex-shrink: 0;
}

</style>
