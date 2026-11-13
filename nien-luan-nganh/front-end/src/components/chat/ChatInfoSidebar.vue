<template>
  <aside class="chat-info-sidebar">
    <div class="chat-info-sidebar-inner">
      <!-- 1. HEADER SIDEBAR (CHUYỂN ĐỔI THEO VIEW) -->
      <!-- A. Header màn hình thông tin chính -->
      <div v-if="currentView === 'INFO'" class="sidebar-header">
        <div class="header-title">
          <h3>Thông tin hội thoại</h3>
        </div>
        <button class="btn-close" title="Đóng (Esc)" @click="$emit('close')">
          <font-awesome-icon icon="x" />
        </button>
      </div>

      <!-- B. Header Sub-view Quản lý Lịch hẹn & Nhắc nhở -->
      <div v-else-if="currentView === 'REMINDERS'" class="sidebar-header subview-header">
        <div class="subview-header-left" @click="backToInfoView">
          <button class="btn-back" title="Quay lại thông tin">
            <font-awesome-icon icon="chevron-left" />
          </button>
          <div class="header-title subview-title">
            <h3>Lịch hẹn nhắc nhở</h3>
          </div>
        </div>
        <div class="rem-subview-header-actions">
          <button
            type="button"
            class="btn-header-create-rem"
            title="Tạo nhắc hẹn mới"
            @click="$emit('create-reminder')"
          >
            <font-awesome-icon icon="plus" />
            <span>Tạo hẹn</span>
          </button>
          <button class="btn-close" title="Đóng (Esc)" @click="$emit('close')">
            <font-awesome-icon icon="x" />
          </button>
        </div>
      </div>

      <!-- C. Header Sub-view xem toàn bộ File phương tiện (Chuẩn Zalo PC / Telegram Desktop) -->
      <div v-else class="sidebar-header subview-header">
        <div class="subview-header-left" @click="backToInfoView">
          <button class="btn-back" title="Quay lại thông tin">
            <font-awesome-icon icon="chevron-left" />
          </button>
          <div class="header-title subview-title">
            <h3>File phương tiện</h3>
          </div>
        </div>
        <button class="btn-close" title="Đóng (Esc)" @click="$emit('close')">
          <font-awesome-icon icon="x" />
        </button>
      </div>

      <!-- 2. NỘI DUNG CHÍNH -->
      <!-- VIEW 1: THÔNG TIN HỘI THOẠI -->
      <div v-if="currentView === 'INFO'" class="sidebar-scroll-content">
        <!-- 2.1 AVATAR & TÊN PHÒNG -->
        <div class="room-profile-section">
          <!-- Wrapper avatar: Nếu là phòng nhóm (GROUP) thì cho phép click chọn file ảnh đổi avatar -->
          <div
            class="room-avatar-wrapper"
            :class="{ 'room-avatar-clickable': isGroupRoom }"
            :title="isGroupRoom ? 'Nhấp để đổi ảnh đại diện nhóm' : ''"
            @click="handleAvatarClick"
          >
            <UserAvatar
              :name="displayRoomName"
              :src="resolveFileUrl(displayRoomAvatar)"
              :size="72"
            />
            <!-- Overlay icon máy ảnh khi hover vào avatar nhóm -->
            <div v-if="isGroupRoom" class="room-avatar-overlay">
              <font-awesome-icon icon="camera" class="overlay-cam-icon" />
            </div>
          </div>

          <!-- Hidden File Input chọn ảnh đổi avatar nhóm -->
          <input
            v-if="isGroupRoom"
            ref="groupAvatarInputRef"
            type="file"
            accept="image/*"
            style="display: none"
            @change="onGroupAvatarFileSelected"
          />

          <!-- Tên phòng & Trạng thái chỉnh sửa tên nhóm -->
          <div v-if="!isEditingRoomName" class="room-title-row">
            <h4
              class="room-title"
              :class="{ 'room-title-interactive': isGroupRoom }"
              :title="isGroupRoom ? 'Nhấp đúp hoặc nhấp icon để đổi tên nhóm' : ''"
              @dblclick="startEditRoomName"
            >
              {{ displayRoomName }}
            </h4>
            <button
              v-if="isGroupRoom"
              type="button"
              class="btn-edit-room-name"
              title="Chỉnh sửa tên nhóm"
              @click="startEditRoomName"
            >
              <font-awesome-icon icon="pen" />
            </button>
          </div>

          <!-- Trạng thái Đang chỉnh sửa inline (như chỉnh sửa tin nhắn) -->
          <div v-else class="room-title-edit-box">
            <input
              ref="editRoomNameInputRef"
              v-model="editRoomNameValue"
              type="text"
              class="room-name-inline-input"
              maxlength="100"
              placeholder="Nhập tên nhóm mới..."
              @keydown.enter.prevent="saveEditRoomName"
              @keydown.esc="cancelEditRoomName"
            />
            <div class="edit-btn-group">
              <button
                type="button"
                class="edit-mini-btn btn-save"
                title="Lưu (Enter)"
                :disabled="isSavingRoomName"
                @click="saveEditRoomName"
              >
                <font-awesome-icon icon="check" />
              </button>
              <button
                type="button"
                class="edit-mini-btn btn-cancel"
                title="Hủy (Esc)"
                :disabled="isSavingRoomName"
                @click="cancelEditRoomName"
              >
                <font-awesome-icon icon="x" />
              </button>
            </div>
          </div>
        </div>

        <!-- 2.2 BẬT / TẮT THÔNG BÁO -->
        <div class="setting-item notification-box">
          <div class="setting-info">
            <div class="setting-icon-wrapper" :class="{ muted: isNotificationMuted }">
              <font-awesome-icon :icon="isNotificationMuted ? 'bell-slash' : 'bell'" />
            </div>
            <div class="setting-text">
              <span class="setting-title">Thông báo</span>
              <span class="setting-subtext">{{ isNotificationMuted ? 'Đang tắt thông báo' : 'Đang bật thông báo' }}</span>
            </div>
          </div>
          <!-- Toggle Switch UI -->
          <label class="switch">
            <input
              type="checkbox"
              :checked="!isNotificationMuted"
              @change="handleToggleNotification"
            />
            <span class="slider round"></span>
          </label>
        </div>

        <hr class="divider" />

        <!-- 2.3 DANH SÁCH THÀNH VIÊN (COLLAPSIBLE ACCORDION) - CHỈ CHO NHÓM CHAT -->
        <template v-if="isGroupRoom">
          <div class="accordion-section">
            <div class="accordion-header" @click="isMembersExpanded = !isMembersExpanded">
              <div class="header-left">
                <font-awesome-icon icon="user-group" class="section-icon" />
                <span class="section-title">Thành viên phòng ({{ displayMembers.length }})</span>
              </div>

              <font-awesome-icon
                icon="chevron-right"
                class="arrow-icon"
                :class="{ 'arrow--down': isMembersExpanded }"
              />
            </div>

            <!-- CSS Grid Accordion Collapse -->
            <div class="accordion-collapse" :class="{ 'is-expanded': isMembersExpanded }">
              <div class="accordion-collapse-inner">
                <div class="accordion-body">
                  <!-- Thanh thêm thành viên mới vào phòng -->
                  <div class="add-member-action-bar" @click="$emit('add-member')">
                    <div class="add-member-icon-circle">
                      <font-awesome-icon icon="user-plus" />
                    </div>
                    <span class="add-member-text">Thêm thành viên</span>
                  </div>

                  <div v-if="displayMembers.length === 0" class="empty-members-hint">
                    Chưa có danh sách thành viên
                  </div>
                  <div
                    v-for="member in displayMembers"
                    :key="member.userId || member.username"
                    class="member-item member-item--clickable"
                    title="Bấm để xem thông tin thành viên"
                    @click="openMemberProfile(member)"
                  >
                    <UserAvatar
                      :name="member.username || 'Thành viên'"
                      :src="resolveFileUrl(member.avatarUrl)"
                      :size="34"
                    />
                    <div class="member-info">
                      <span class="member-name">{{ member.username || 'Thành viên' }}</span>
                      <span
                        v-if="member.role === 'ADMIN' || member.roomMemberRole === 'ADMIN'"
                        class="role-badge"
                      >
                        Trưởng nhóm
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <hr class="divider" />
        </template>

        <!-- 2.3.5 LỊCH HẸN & NHẮC NHỞ (ACCORDION + PREVIEW + XEM TẤT CẢ) -->
        <div class="accordion-section">
          <div class="accordion-header" @click="isRemindersExpanded = !isRemindersExpanded">
            <div class="header-left">
              <font-awesome-icon icon="calendar-days" class="section-icon" />
              <span class="section-title">
                Lịch hẹn nhắc nhở ({{ upcomingRemindersList.length }})
              </span>
            </div>
            <font-awesome-icon
              icon="chevron-right"
              class="arrow-icon"
              :class="{ 'arrow--down': isRemindersExpanded }"
            />
          </div>

          <div class="accordion-collapse" :class="{ 'is-expanded': isRemindersExpanded }">
            <div class="accordion-collapse-inner">
              <div class="accordion-body">
                <!-- Thanh tạo lịch hẹn mới -->
                <div class="add-member-action-bar" @click="$emit('create-reminder')">
                  <div class="add-member-icon-circle">
                    <font-awesome-icon icon="calendar-plus" />
                  </div>
                  <span class="add-member-text">Tạo nhắc hẹn mới</span>
                </div>

                <!-- Danh sách preview tối đa 3 lịch hẹn sắp tới -->
                <div v-if="upcomingRemindersList.length === 0" class="empty-members-hint">
                  Chưa có lịch hẹn nào sắp diễn ra
                </div>

                <div v-else class="rem-preview-list">
                  <div
                    v-for="rem in upcomingRemindersList.slice(0, 3)"
                    :key="rem.reminderId"
                    class="rem-preview-card"
                    @click="rem.messageId ? $emit('jump-to-message', rem.messageId) : openRemindersView('UPCOMING')"
                  >
                    <div class="rem-mini-leaf" :class="{ 'is-personal': rem.reminderType === 'PERSONAL' }">
                      <div class="rem-mini-month">{{ getCalendarLeaf(rem.startAt).month }}</div>
                      <div class="rem-mini-day">{{ getCalendarLeaf(rem.startAt).day }}</div>
                    </div>

                    <div class="rem-preview-info">
                      <div class="rem-preview-top">
                        <p class="rem-preview-title">{{ rem.title }}</p>
                        <span v-if="rem.reminderType === 'PERSONAL'" class="rem-personal-tag">
                          Cá nhân
                        </span>
                      </div>
                      <p class="rem-preview-time">
                        {{ formatReminderShortTime(rem.startAt, rem.endAt) }}
                      </p>
                      <div v-if="rem.reminderType === 'GROUP'" class="rem-preview-meta">
                        <span>{{ rem.acceptedCount || 0 }} tham gia</span>
                        <span v-if="rem.myRsvp === 'ACCEPTED'" class="rsvp-tag-ok">✓ Đã nhận</span>
                        <span v-else-if="rem.myRsvp === 'DECLINED'" class="rsvp-tag-no">✕ Từ chối</span>
                        <span v-else class="rsvp-tag-wait">Chờ phản hồi</span>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Nút Xem tất cả lịch hẹn -->
                <button
                  v-if="allRemindersList.length > 0"
                  type="button"
                  class="btn-open-gallery"
                  style="margin-top: 8px"
                  @click="openRemindersView('UPCOMING')"
                >
                  <span>Xem tất cả lịch hẹn ({{ allRemindersList.length }})</span>
                  <font-awesome-icon icon="chevron-right" class="btn-arrow" />
                </button>
              </div>
            </div>
          </div>
        </div>

        <hr class="divider" />

        <!-- 2.4 FILE PHƯƠNG TIỆN (PREVIEW GỌN GÀNG + NÚT XEM TẤT CẢ) -->
        <div class="accordion-section media-section">
          <div class="accordion-header" @click="isMediaExpanded = !isMediaExpanded">
            <div class="header-left">
              <font-awesome-icon icon="folder-open" class="section-icon" />
              <span class="section-title">File phương tiện & Tài liệu</span>
            </div>
            <font-awesome-icon
              icon="chevron-right"
              class="arrow-icon"
              :class="{ 'arrow--down': isMediaExpanded }"
            />
          </div>

          <div class="accordion-collapse" :class="{ 'is-expanded': isMediaExpanded }">
            <div class="accordion-collapse-inner">
              <div class="accordion-body">
                <!-- THANH NÚT TAB NGANG HÀNG -->
                <div class="media-tabs-bar">
                  <button
                    type="button"
                    class="tab-btn"
                    :class="{ active: currentMediaTab === 'IMAGE' }"
                    @click="switchMediaTab('IMAGE')"
                  >
                    <font-awesome-icon icon="image" class="tab-icon" />
                    <span>Ảnh</span>
                  </button>
                  <button
                    type="button"
                    class="tab-btn"
                    :class="{ active: currentMediaTab === 'VIDEO' }"
                    @click="switchMediaTab('VIDEO')"
                  >
                    <font-awesome-icon icon="video" class="tab-icon" />
                    <span>Video</span>
                  </button>
                  <button
                    type="button"
                    class="tab-btn"
                    :class="{ active: currentMediaTab === 'FILE' }"
                    @click="switchMediaTab('FILE')"
                  >
                    <font-awesome-icon icon="file-lines" class="tab-icon" />
                    <span>Tài liệu</span>
                  </button>
                </div>

                <!-- PREVIEW NỘI DUNG (CHỈ 6 MỤC, KHÔNG CÓ THANH CUỘN CỤC BỘ) -->
                <div class="media-preview-container">
                  <!-- A. TAB ẢNH & VIDEO (GRID 3 CỘT X 2 HÀNG) -->
                  <div
                    v-if="currentMediaTab === 'IMAGE' || currentMediaTab === 'VIDEO'"
                    class="media-grid"
                  >
                    <div
                      v-for="item in currentTabMediaList.slice(0, 6)"
                      :key="item.id || item.url"
                      class="media-thumbnail-card"
                      @click="openMediaPreview(item)"
                    >
                      <!-- Fallback khi ảnh/video lỗi không load được -->
                      <div v-if="isMediaFailed(item)" class="media-fallback-box">
                        <font-awesome-icon :icon="item.type === 'VIDEO' || currentMediaTab === 'VIDEO' ? 'video' : 'image'" class="media-fallback-icon" />
                      </div>
                      <img
                        v-else-if="item.type === 'IMAGE' || currentMediaTab === 'IMAGE'"
                        :src="resolveFileUrl(item.url)"
                        alt=""
                        @error="handleMediaError(item)"
                      />
                      <div v-else class="video-preview-wrapper">
                        <video :src="resolveFileUrl(item.url)" muted @error="handleMediaError(item)"></video>
                        <span class="play-overlay-icon">
                          <font-awesome-icon icon="play" />
                        </span>
                      </div>
                    </div>

                    <div v-if="currentTabMediaList.length === 0" class="empty-media-hint">
                      Chưa có {{ currentMediaTab === 'IMAGE' ? 'ảnh' : 'video' }} nào
                    </div>
                  </div>

                  <!-- B. TAB FILE TÀI LIỆU (PREVIEW TỐI ĐA 3 FILE) -->
                  <div
                    v-else
                    class="files-list"
                  >
                    <a
                      v-for="file in currentTabMediaList.slice(0, 3)"
                      :key="file.id || file.url"
                      :href="resolveFileUrl(file.url)"
                      target="_blank"
                      rel="noopener noreferrer"
                      class="file-item-card"
                    >
                      <div class="file-icon-box">
                        <font-awesome-icon icon="file-lines" />
                      </div>
                      <div class="file-meta">
                        <span class="file-name" :title="file.name">{{ file.name }}</span>
                        <span class="file-size">{{ file.size || 'Đính kèm' }}</span>
                      </div>
                      <font-awesome-icon icon="arrow-down" class="file-download-icon" />
                    </a>

                    <div v-if="currentTabMediaList.length === 0" class="empty-media-hint">
                      Chưa có tài liệu nào
                    </div>
                  </div>
                </div>

                <!-- NÚT XEM TẤT CẢ (CHUYỂN SANG SUB-VIEW GALLERY CHUYÊN DỤNG) -->
                <button
                  v-if="currentTabMediaList.length > 0"
                  type="button"
                  class="btn-open-gallery"
                  @click="openMediaView(currentMediaTab)"
                >
                  <span>Xem tất cả ({{ currentTabMediaList.length }})</span>
                  <font-awesome-icon icon="chevron-right" class="btn-arrow" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- VIEW 2: SUB-VIEW QUẢN LÝ LỊCH HẸN & NHẮC NHỞ PHÒNG CHAT -->
      <div v-else-if="currentView === 'REMINDERS'" class="subview-media-content">
        <div class="subview-tabs-bar">
          <button
            type="button"
            class="subview-tab-btn"
            :class="{ active: currentReminderTab === 'UPCOMING' }"
            @click="currentReminderTab = 'UPCOMING'"
          >
            <font-awesome-icon icon="calendar-days" />
            <span>Sắp tới ({{ upcomingRemindersList.length }})</span>
          </button>
          <button
            type="button"
            class="subview-tab-btn"
            :class="{ active: currentReminderTab === 'PAST' }"
            @click="currentReminderTab = 'PAST'"
          >
            <font-awesome-icon icon="clock-rotate-left" />
            <span>Đã qua / Hủy ({{ pastRemindersList.length }})</span>
          </button>
        </div>

        <div class="subview-gallery-scroll rem-subview-list">
          <div v-if="currentTabRemindersList.length === 0" class="empty-media-hint">
            {{
              currentReminderTab === 'UPCOMING'
                ? 'Chưa có lịch hẹn nào sắp diễn ra trong phòng này'
                : 'Chưa có lịch hẹn nào đã kết thúc hoặc bị hủy'
            }}
          </div>

          <div
            v-for="rem in currentTabRemindersList"
            :key="rem.reminderId"
            class="rem-subview-card"
            :class="{ 'is-cancelled': rem.reminderStatus === 'CANCELLED' }"
          >
            <!-- Đầu mục: Tờ lịch + Tiêu đề -->
            <div class="rem-subview-top">
              <div
                class="rem-mini-leaf rem-leaf-lg"
                :class="{
                  'is-cancelled': rem.reminderStatus === 'CANCELLED',
                  'is-personal': rem.reminderType === 'PERSONAL',
                }"
              >
                <div class="rem-mini-month">{{ getCalendarLeaf(rem.startAt).month }}</div>
                <div class="rem-mini-day">{{ getCalendarLeaf(rem.startAt).day }}</div>
              </div>

              <div class="rem-subview-main">
                <div class="rem-subview-badges">
                  <span
                    class="rem-badge-status"
                    :class="
                      rem.reminderStatus === 'CANCELLED'
                        ? 'st-cancelled'
                        : isReminderPast(rem) || rem.reminderStatus === 'DONE'
                          ? 'st-done'
                          : rem.reminderType === 'PERSONAL'
                            ? 'st-personal'
                            : 'st-pending'
                    "
                  >
                    {{
                      rem.reminderStatus === 'CANCELLED'
                        ? 'Đã hủy'
                        : isReminderPast(rem) || rem.reminderStatus === 'DONE'
                          ? 'Đã kết thúc'
                          : rem.reminderType === 'PERSONAL'
                            ? 'Cá nhân'
                            : 'Lịch nhóm'
                    }}
                  </span>

                  <button
                    v-if="rem.messageId"
                    type="button"
                    class="rem-jump-link"
                    @click="$emit('jump-to-message', rem.messageId)"
                  >
                    Xem tin nhắn
                  </button>
                </div>

                <h4 class="rem-subview-title">{{ rem.title }}</h4>

                <p class="rem-subview-time">
                  {{ formatReminderShortTime(rem.startAt, rem.endAt) }}
                </p>

                <p v-if="rem.description" class="rem-subview-desc">
                  {{ rem.description }}
                </p>

                <p class="rem-subview-author">
                  Tạo bởi <strong>{{ rem.createdByName || rem.createdByUsername || 'Thành viên' }}</strong>
                  <template v-if="rem.reminderType === 'GROUP'">
                    · <strong class="rsvp-tag-ok">{{ rem.acceptedCount || 0 }}</strong> tham gia
                  </template>
                </p>
              </div>
            </div>

            <!-- Thanh nút RSVP & Hủy lịch ngay trên Sidebar (chỉ khi còn hạn) -->
            <div v-if="rem.reminderStatus === 'PENDING' && !isReminderPast(rem)" class="rem-subview-actions">
              <template v-if="rem.reminderType === 'GROUP'">
                <button
                  type="button"
                  class="rem-sb-btn btn-sb-accept"
                  :class="{ active: rem.myRsvp === 'ACCEPTED' }"
                  @click="handleSidebarRsvp(rem, 'ACCEPTED')"
                >
                  {{ rem.myRsvp === 'ACCEPTED' ? '✓ Đã tham gia' : 'Tham gia' }}
                </button>
                <button
                  type="button"
                  class="rem-sb-btn btn-sb-decline"
                  :class="{ active: rem.myRsvp === 'DECLINED' }"
                  @click="handleSidebarRsvp(rem, 'DECLINED')"
                >
                  {{ rem.myRsvp === 'DECLINED' ? '✕ Đã từ chối' : 'Từ chối' }}
                </button>
              </template>

              <button
                v-if="canCancelReminder(rem)"
                type="button"
                class="rem-sb-cancel-btn"
                title="Hủy lịch hẹn"
                @click="handleSidebarCancelReminder(rem)"
              >
                <font-awesome-icon icon="trash" />
              </button>
            </div>

            <!-- Thông báo khi đã kết thúc hoặc hủy -->
            <div v-else-if="rem.reminderStatus !== 'CANCELLED'" class="rem-subview-finished-hint">
              <font-awesome-icon icon="clock-rotate-left" />
              <span>Lịch hẹn đã kết thúc</span>
            </div>
          </div>
        </div>
      </div>

      <!-- VIEW 3: SUB-VIEW GALLERY TOÀN BỘ FILE PHƯƠNG TIỆN (CHUẨN TELEGRAM / ZALO PC) -->
      <div v-else class="subview-media-content">
        <!-- THANH TAB SEGMENTED CONTROL TO RÕ TRÊN ĐẦU SUBVIEW -->
        <div class="subview-tabs-bar">
          <button
            type="button"
            class="subview-tab-btn"
            :class="{ active: currentMediaTab === 'IMAGE' }"
            @click="currentMediaTab = 'IMAGE'"
          >
            <font-awesome-icon icon="image" />
            <span>Ảnh ({{ props.mediaData?.images?.length || 0 }})</span>
          </button>
          <button
            type="button"
            class="subview-tab-btn"
            :class="{ active: currentMediaTab === 'VIDEO' }"
            @click="currentMediaTab = 'VIDEO'"
          >
            <font-awesome-icon icon="video" />
            <span>Video ({{ props.mediaData?.videos?.length || 0 }})</span>
          </button>
          <button
            type="button"
            class="subview-tab-btn"
            :class="{ active: currentMediaTab === 'FILE' }"
            @click="currentMediaTab = 'FILE'"
          >
            <font-awesome-icon icon="file-lines" />
            <span>File ({{ props.mediaData?.files?.length || 0 }})</span>
          </button>
        </div>

        <!-- DANH SÁCH TOÀN BỘ PHƯƠNG TIỆN - CUỘN DUY NHẤT 1 THANH SIÊU MƯỢT TỪ ĐẦU ĐẾN CUỐI -->
        <div class="subview-gallery-scroll">
          <!-- A. TAB ẢNH & VIDEO (GRID 3 CỘT) -->
          <div
            v-if="currentMediaTab === 'IMAGE' || currentMediaTab === 'VIDEO'"
            class="subview-media-grid"
          >
            <div
              v-for="item in currentTabMediaList"
              :key="item.id || item.url"
              class="media-thumbnail-card"
              @click="openMediaPreview(item)"
            >
              <!-- Fallback khi ảnh/video lỗi không load được -->
              <div v-if="isMediaFailed(item)" class="media-fallback-box">
                <font-awesome-icon :icon="item.type === 'VIDEO' || currentMediaTab === 'VIDEO' ? 'video' : 'image'" class="media-fallback-icon" />
              </div>
              <img
                v-else-if="item.type === 'IMAGE' || currentMediaTab === 'IMAGE'"
                :src="resolveFileUrl(item.url)"
                alt=""
                @error="handleMediaError(item)"
              />
              <div v-else class="video-preview-wrapper">
                <video :src="resolveFileUrl(item.url)" muted @error="handleMediaError(item)"></video>
                <span class="play-overlay-icon">
                  <font-awesome-icon icon="play" />
                </span>
              </div>
            </div>

            <div v-if="currentTabMediaList.length === 0" class="empty-media-hint">
              Chưa có {{ currentMediaTab === 'IMAGE' ? 'ảnh' : 'video' }} nào
            </div>
          </div>

          <!-- B. TAB TÀI LIỆU (DANH SÁCH FILE ĐẦY ĐỦ) -->
          <div
            v-else
            class="subview-files-list"
          >
            <a
              v-for="file in currentTabMediaList"
              :key="file.id || file.url"
              :href="resolveFileUrl(file.url)"
              target="_blank"
              rel="noopener noreferrer"
              class="file-item-card"
            >
              <div class="file-icon-box">
                <font-awesome-icon icon="file-lines" />
              </div>
              <div class="file-meta">
                <span class="file-name" :title="file.name">{{ file.name }}</span>
                <span class="file-size">{{ file.size || 'Đính kèm' }}</span>
              </div>
              <font-awesome-icon icon="arrow-down" class="file-download-icon" />
            </a>

            <div v-if="currentTabMediaList.length === 0" class="empty-media-hint">
              Chưa có tài liệu nào
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Popup xem hồ sơ thành viên (tái sử dụng ContactProfileCard) -->
    <Teleport to="body">
      <div
        v-if="selectedMemberProfile"
        class="profile-modal-backdrop"
        @click.self="closeMemberProfile"
      >
        <div class="profile-modal-card-wrap">
          <!-- Nhóm nút hành động ở góc trên bên phải Card (3 chấm & Đóng X) -->
          <div class="profile-modal-top-actions">
            <!-- Nút 3 chấm mở menu tùy chọn (chỉ hiện khi không phải chính mình) -->
            <div v-if="!selectedMemberProfile.isMe" class="profile-menu-wrapper">
              <button
                type="button"
                class="profile-action-btn profile-more-btn"
                :class="{ active: isProfileMenuOpen }"
                title="Tùy chọn khác"
                @click="isProfileMenuOpen = !isProfileMenuOpen"
              >
                <font-awesome-icon icon="ellipsis-vertical" />
              </button>

              <!-- Dropdown Menu -->
              <div v-if="isProfileMenuOpen" class="profile-menu-dropdown">
                <!-- 1. Điều hướng qua danh bạ xem chi tiết -->
                <button
                  type="button"
                  class="menu-dropdown-item"
                  @click="handleNavigateToContacts(selectedMemberProfile)"
                >
                  <font-awesome-icon icon="address-book" class="item-icon" />
                  <span>Xem trong danh bạ</span>
                </button>

                <!-- 2. Hủy kết bạn (chỉ hiện khi đã là bạn bè) -->
                <button
                  v-if="selectedMemberProfile.isFriend"
                  type="button"
                  class="menu-dropdown-item item-danger"
                  @click="handleUnfriendFromProfile(selectedMemberProfile)"
                >
                  <font-awesome-icon icon="user-minus" class="item-icon" />
                  <span>Hủy kết bạn</span>
                </button>

                <!-- 3. Chặn người dùng / Bỏ chặn -->
                <button
                  type="button"
                  class="menu-dropdown-item"
                  :class="selectedMemberProfile.isBlocked ? 'item-primary' : 'item-danger'"
                  @click="handleToggleBlockFromProfile(selectedMemberProfile)"
                >
                  <font-awesome-icon
                    :icon="selectedMemberProfile.isBlocked ? 'unlock' : 'ban'"
                    class="item-icon"
                  />
                  <span>{{ selectedMemberProfile.isBlocked ? 'Bỏ chặn người dùng' : 'Chặn người dùng' }}</span>
                </button>
              </div>
            </div>

            <!-- Nút Đóng X -->
            <button
              type="button"
              class="profile-action-btn profile-close-btn"
              title="Đóng (Esc)"
              @click="closeMemberProfile"
            >
              <font-awesome-icon icon="x" />
            </button>
          </div>

          <ContactProfileCard
            :contact="selectedMemberProfile"
            @message="handleDirectMessageWithMember"
            @add-friend="handleAddFriendFromProfile"
            @cancel-friend-request="handleCancelFriendRequestFromProfile"
            @unblock="handleToggleBlockFromProfile"
          />
        </div>
      </div>
    </Teleport>

    <!-- 4. MODAL CẮT VÀ TẢI LÊN AVATAR NHÓM -->
    <AvatarUploadModal
      v-if="showGroupAvatarCropModal"
      :file="selectedGroupAvatarFile"
      @save="handleSaveGroupAvatar"
      @cancel="handleCancelGroupAvatarCrop"
    />
  </aside>
</template>

<script setup>
import { resolveFileUrl } from '@/api/storage.api';
import { ref, computed, watch, onMounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import UserAvatar from '@/components/common/UserAvatar.vue';
import AvatarUploadModal from '@/components/common/AvatarUploadModal.vue';
import ContactProfileCard from '@/components/contacts/ContactProfileCard.vue';
import { useChatStore } from '@/stores/chat';
import { useAuthStore } from '@/stores/auth';
import { uploadRoomAvatar, updateRoomName } from '@/api/room.api';
import {
  getFriends,
  getPendingFriendships,
  sendFriendRequest,
  cancelFriendRequest,
  getFriendshipStatus,
  blockUser,
  unblockUser
} from '@/api/contacts.api';

const router = useRouter();

const chatStore = useChatStore();
const authStore = useAuthStore();

const props = defineProps({
  room: {
    type: Object,
    default: () => ({})
  },
  members: {
    type: Array,
    default: () => []
  },
  mediaData: {
    type: Object,
    default: () => ({ images: [], videos: [], files: [] })
  }
});

const emit = defineEmits([
  'close',
  'toggle-notification',
  'preview-media',
  'add-member',
  'create-reminder',
  'jump-to-message',
]);

// Chế độ xem: 'INFO' (thông tin phòng) | 'MEDIA' (toàn bộ phương tiện) | 'REMINDERS' (quản lý lịch hẹn)
const currentView = ref('INFO');
const currentReminderTab = ref('UPCOMING'); // 'UPCOMING' | 'PAST'

const openMediaView = (tab = 'IMAGE') => {
  currentMediaTab.value = tab;
  currentView.value = 'MEDIA';
};

const openRemindersView = (tab = 'UPCOMING') => {
  currentReminderTab.value = tab;
  currentView.value = 'REMINDERS';
};

const backToInfoView = () => {
  currentView.value = 'INFO';
};

// Tự động điều hướng trở lại màn hình Thông tin hội thoại khi chuyển sang phòng chat khác
watch(
  [() => props.room?.roomId, () => props.room?.id, () => chatStore.activeRoomId],
  () => {
    currentView.value = 'INFO';
  }
);

// Phân biệt nhóm hay phòng trực tiếp 1-1
const isGroupRoom = computed(() => {
  return props.room?.roomType === 'GROUP';
});

// ==========================================
// ĐỔI AVATAR PHÒNG NHÓM (GROUP AVATAR UPLOAD)
// ==========================================
const groupAvatarInputRef = ref(null);
const selectedGroupAvatarFile = ref(null);
const showGroupAvatarCropModal = ref(false);
const isUploadingGroupAvatar = ref(false);

const handleAvatarClick = () => {
  if (!isGroupRoom.value) return;
  // Kích hoạt dialog chọn file của hệ thống
  groupAvatarInputRef.value?.click();
};

const onGroupAvatarFileSelected = (e) => {
  const file = e.target.files?.[0];
  if (!file) return;
  if (!file.type.startsWith('image/')) {
    alert('Vui lòng chọn file hình ảnh hợp lệ.');
    e.target.value = '';
    return;
  }
  selectedGroupAvatarFile.value = file;
  showGroupAvatarCropModal.value = true;
  // Reset input để người dùng có thể chọn lại cùng 1 file nếu cần
  e.target.value = '';
};

const handleCancelGroupAvatarCrop = () => {
  showGroupAvatarCropModal.value = false;
  selectedGroupAvatarFile.value = null;
};

const handleSaveGroupAvatar = async (croppedFile) => {
  const roomId = props.room?.roomId || props.room?.id || chatStore.activeRoomId;
  if (!roomId || !croppedFile) return;

  isUploadingGroupAvatar.value = true;
  try {
    const res = await uploadRoomAvatar(roomId, croppedFile);
    // Backend trả về URL avatar mới (res.roomAvatarURL || res.avatarUrl || res)
    const newAvatarUrl = res?.roomAvatarURL || res?.avatarUrl || res;
    if (newAvatarUrl) {
      chatStore.updateRoomAvatar(roomId, newAvatarUrl);
    }
    showGroupAvatarCropModal.value = false;
    selectedGroupAvatarFile.value = null;
  } catch (error) {
    console.error('Lỗi tải lên avatar nhóm:', error);
    alert('Không thể cập nhật ảnh nhóm. Vui lòng thử lại.');
  } finally {
    isUploadingGroupAvatar.value = false;
  }
};

// ==========================================
// CHỈNH SỬA TÊN PHÒNG NHÓM (RENAME GROUP)
// ==========================================
const isEditingRoomName = ref(false);
const editRoomNameValue = ref('');
const editRoomNameInputRef = ref(null);
const isSavingRoomName = ref(false);

const startEditRoomName = async () => {
  if (!isGroupRoom.value) return;
  editRoomNameValue.value = displayRoomName.value;
  isEditingRoomName.value = true;
  await nextTick();
  editRoomNameInputRef.value?.focus();
  editRoomNameInputRef.value?.select();
};

const cancelEditRoomName = () => {
  isEditingRoomName.value = false;
  editRoomNameValue.value = '';
};

const saveEditRoomName = async () => {
  const roomId = props.room?.roomId || props.room?.id || chatStore.activeRoomId;
  const trimmed = editRoomNameValue.value.trim();

  if (!trimmed) {
    alert('Tên nhóm không được để trống.');
    return;
  }
  if (trimmed === displayRoomName.value) {
    isEditingRoomName.value = false;
    return;
  }
  if (!roomId) return;

  isSavingRoomName.value = true;
  try {
    await updateRoomName(roomId, trimmed);
    chatStore.updateRoomName(roomId, trimmed);
    isEditingRoomName.value = false;
  } catch (error) {
    console.error('Lỗi đổi tên nhóm:', error);
    alert('Không thể đổi tên nhóm. Vui lòng thử lại.');
  } finally {
    isSavingRoomName.value = false;
  }
};

// State quản lý Accordion
const isMembersExpanded = ref(true);
const isRemindersExpanded = ref(true);
const isMediaExpanded = ref(true);

// Danh sách lịch hẹn của phòng đang mở từ Pinia Store
const allRemindersList = computed(() => {
  return chatStore.roomReminders || [];
});

const upcomingRemindersList = computed(() => {
  return chatStore.upcomingRoomReminders || [];
});

//Hàm kiểm tra lịch hẹn đã qua thời hạn hay chưa
const isReminderPast = (rem) => {
  if (!rem) return false;
  if (rem.reminderStatus === 'DONE' || rem.reminderStatus === 'CANCELLED') return true;
  const deadline = new Date(rem.endAt || rem.startAt).getTime();
  return !isNaN(deadline) && deadline <= Date.now();
};

//Tab "Đã qua / Hủy" tự động gom các lịch hẹn DONE, CANCELLED hoặc đã trôi qua deadline
const pastRemindersList = computed(() => {
  return [...allRemindersList.value]
    .filter((r) => isReminderPast(r))
    .sort((a, b) => new Date(b.startAt).getTime() - new Date(a.startAt).getTime());
});


const currentTabRemindersList = computed(() => {
  return currentReminderTab.value === 'UPCOMING'
    ? upcomingRemindersList.value
    : pastRemindersList.value;
});

const getCalendarLeaf = (isoStr) => {
  const d = isoStr ? new Date(isoStr) : new Date();
  if (Number.isNaN(d.getTime())) return { month: 'TH --', day: '--' };
  return {
    month: `TH ${d.getMonth() + 1}`,
    day: String(d.getDate()).padStart(2, '0'),
  };
};

const formatReminderShortTime = (startRaw, endRaw) => {
  if (!startRaw) return '';
  const start = new Date(startRaw);
  if (Number.isNaN(start.getTime())) return '';
  const pad = (n) => String(n).padStart(2, '0');
  const timeStr = `${pad(start.getHours())}:${pad(start.getMinutes())}`;
  const dateStr = `${pad(start.getDate())}/${pad(start.getMonth() + 1)}/${start.getFullYear()}`;
  if (!endRaw) return `${timeStr} · ${dateStr}`;
  const end = new Date(endRaw);
  if (Number.isNaN(end.getTime())) return `${timeStr} · ${dateStr}`;
  const endTime = `${pad(end.getHours())}:${pad(end.getMinutes())}`;
  const sameDay =
    start.getDate() === end.getDate() &&
    start.getMonth() === end.getMonth() &&
    start.getFullYear() === end.getFullYear();
  if (sameDay) return `${timeStr} - ${endTime} · ${dateStr}`;
  const endDate = `${pad(end.getDate())}/${pad(end.getMonth() + 1)}`;
  return `${timeStr} ${pad(start.getDate())}/${pad(start.getMonth() + 1)} → ${endTime} ${endDate}`;
};

const canCancelReminder = (rem) => {
  if (!rem || rem.reminderStatus !== 'PENDING' || isReminderPast(rem)) return false;
  const myId = authStore.user?.userId;
  if (myId && String(rem.createdById) === String(myId)) return true;
  const myMember = displayMembers.value.find((m) => myId && String(m.userId) === String(myId));
  const role = myMember?.role || myMember?.roomMemberRole;
  return role === 'ADMIN' || role === 'OWNER';
};

const handleSidebarRsvp = async (rem, status) => {
  if (!rem || rem.myRsvp === status) return;
  try {
    await chatStore.respondReminderRsvp(rem.roomId || chatStore.activeRoomId, rem.reminderId, status);
  } catch (err) {
    alert(err?.response?.data?.message || 'Không thể cập nhật phản hồi lịch hẹn.');
  }
};

const handleSidebarCancelReminder = async (rem) => {
  if (!rem) return;
  if (!confirm(`Bạn có chắc chắn muốn hủy lịch hẹn "${rem.title}" không?`)) return;
  try {
    await chatStore.cancelRoomReminder(rem.roomId || chatStore.activeRoomId, rem.reminderId);
  } catch (err) {
    alert(err?.response?.data?.message || 'Không thể hủy lịch hẹn.');
  }
};

// State bật/tắt thông báo
const isNotificationMuted = ref(false);

// State tab Phương tiện
const currentMediaTab = ref('IMAGE'); // 'IMAGE' | 'VIDEO' | 'FILE'

const displayRoomName = computed(() => {
  return props.room?.roomName || props.room?.name || 'Phòng trò chuyện';
});

const displayRoomAvatar = computed(() => {
  return props.room?.roomAvatarURL || props.room?.avatarUrl || '';
});

const displayMembers = computed(() => {
  return Array.isArray(props.members) ? props.members : [];
});

const switchMediaTab = (tab) => {
  currentMediaTab.value = tab;
};

// Lọc dữ liệu theo tab hiện tại, luôn sắp xếp DESC (mới nhất lên đầu)
const currentTabMediaList = computed(() => {
  let list = [];
  if (currentMediaTab.value === 'IMAGE') list = props.mediaData?.images || [];
  else if (currentMediaTab.value === 'VIDEO') list = props.mediaData?.videos || [];
  else list = props.mediaData?.files || [];

  return [...list].sort((a, b) => {
    const tA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
    const tB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
    return tB - tA;
  });
});

const handleToggleNotification = (e) => {
  isNotificationMuted.value = !e.target.checked;
  emit('toggle-notification', isNotificationMuted.value);
};

// Quản lý các ảnh/video bị lỗi không load được
const failedMediaUrls = ref(new Set());
const handleMediaError = (item) => {
  const key = item.id || item.url;
  if (key) {
    failedMediaUrls.value.add(key);
  }
};
const isMediaFailed = (item) => {
  const key = item.id || item.url;
  return key ? failedMediaUrls.value.has(key) : false;
};

const openMediaPreview = (item) => {
  emit('preview-media', item);
};

// Quản lý quan hệ bạn bè để hiển thị trạng thái và nút kết bạn / thu hồi / hủy kết bạn / chặn
const friendIds = ref(new Set());
const pendingIds = ref(new Set());
const blockedIds = ref(new Set());
const pendingMap = ref(new Map()); // userId -> friendshipId (thu hồi kết bạn)
const friendShipMap = ref(new Map()); // userId -> friendshipId (hủy kết bạn)

const fetchFriendshipStatus = async () => {
  try {
    const [friendsRes, pendingRes] = await Promise.allSettled([
      getFriends({ page: 0, size: 500 }),
      getPendingFriendships({ page: 0, size: 500 })
    ]);
    const friends = (friendsRes.status === 'fulfilled' && (Array.isArray(friendsRes.value) ? friendsRes.value : friendsRes.value?.content)) || [];
    const pending = (pendingRes.status === 'fulfilled' && (Array.isArray(pendingRes.value) ? pendingRes.value : pendingRes.value?.content)) || [];

    friendIds.value = new Set(friends.map((f) => f.friend?.userId).filter(Boolean));

    pendingMap.value.clear();
    pending.forEach((p) => {
      const uId = p.friend?.userId;
      if (uId) {
        pendingMap.value.set(uId, p.friendshipId);
      }
    });
    pendingIds.value = new Set(pendingMap.value.keys());

    friendShipMap.value.clear();
    friends.forEach((f) => {
      const uId = f.friend?.userId;
      if (uId && f.friendshipId) {
        friendShipMap.value.set(uId, f.friendshipId);
      }
    });
  } catch (err) {
    console.error('Lỗi tải trạng thái bạn bè:', err);
  }
};

onMounted(() => {
  fetchFriendshipStatus();
});

// Quản lý popup xem hồ sơ thành viên (ContactProfileCard)
const selectedMemberProfile = ref(null);
const isProfileMenuOpen = ref(false);

const closeMemberProfile = () => {
  selectedMemberProfile.value = null;
  isProfileMenuOpen.value = false;
};

const openMemberProfile = (member) => {
  if (!member) return;
  isProfileMenuOpen.value = false;
  const targetId = member.userId;
  const isMe = !!(authStore.user?.userId && targetId === authStore.user.userId);
  const isFriend = friendIds.value.has(targetId);
  const isPending = pendingIds.value.has(targetId);
  const isBlocked = blockedIds.value.has(targetId);
  const friendshipId = pendingMap.value.get(targetId) || friendShipMap.value.get(targetId) || null;

  selectedMemberProfile.value = {
    id: targetId,
    name: member.username || 'Thành viên',
    role: (member.role === 'ADMIN' || member.roomMemberRole === 'ADMIN') ? 'Trưởng nhóm' : (isMe ? 'Bạn (Thành viên)' : 'Thành viên'),
    avatar: resolveFileUrl(member.avatarUrl || member.avatar || ''),
    online: member.status === 'ACTIVE' || member.isOnline || false,
    statusText: member.status === 'ACTIVE' ? 'Đang hoạt động' : 'Offline',
    bio: member.bio || member.description || 'Thành viên trong phòng trò chuyện.',
    isMe,
    isFriend,
    isPending,
    isBlocked,
    friendshipId
  };

  // Đồng bộ trạng thái mới nhất từ server (tránh trường hợp cache cũ)
  if (!isMe && targetId) {
    getFriendshipStatus(targetId)
      .then((rel) => {
        if (!selectedMemberProfile.value || selectedMemberProfile.value.id !== targetId) return;
        if (!rel) {
          selectedMemberProfile.value.isFriend = false;
          selectedMemberProfile.value.isPending = false;
          selectedMemberProfile.value.isBlocked = false;
          selectedMemberProfile.value.friendshipId = null;
          pendingIds.value.delete(targetId);
          pendingMap.value.delete(targetId);
          friendIds.value.delete(targetId);
          friendShipMap.value.delete(targetId);
          blockedIds.value.delete(targetId);
        } else if (rel.status === 'ACCEPTED') {
          selectedMemberProfile.value.isFriend = true;
          selectedMemberProfile.value.isPending = false;
          selectedMemberProfile.value.isBlocked = false;
          selectedMemberProfile.value.friendshipId = rel.friendshipId;
          friendIds.value.add(targetId);
          friendShipMap.value.set(targetId, rel.friendshipId);
          pendingIds.value.delete(targetId);
          pendingMap.value.delete(targetId);
          blockedIds.value.delete(targetId);
        } else if (rel.status === 'PENDING') {
          selectedMemberProfile.value.isFriend = false;
          selectedMemberProfile.value.isPending = true;
          selectedMemberProfile.value.isBlocked = false;
          selectedMemberProfile.value.friendshipId = rel.friendshipId;
          pendingIds.value.add(targetId);
          pendingMap.value.set(targetId, rel.friendshipId);
          blockedIds.value.delete(targetId);
        } else if (rel.status === 'BLOCKED') {
          const amIBlocker = rel.direction === 'OUTGOING';
          selectedMemberProfile.value.isBlocked = amIBlocker;
          selectedMemberProfile.value.isFriend = false;
          selectedMemberProfile.value.isPending = false;
          selectedMemberProfile.value.friendshipId = rel.friendshipId;
          if (amIBlocker) blockedIds.value.add(targetId);
        }
      })
      .catch(() => {});
  }
};

const handleAddFriendFromProfile = async (contact) => {
  if (!contact?.id) return;
  try {
    const res = await sendFriendRequest(contact.id);
    const fId = res?.friendshipId;
    pendingIds.value.add(contact.id);
    if (fId) {
      pendingMap.value.set(contact.id, fId);
    }
    if (selectedMemberProfile.value && selectedMemberProfile.value.id === contact.id) {
      selectedMemberProfile.value.isPending = true;
      if (fId) selectedMemberProfile.value.friendshipId = fId;
    }
  } catch (err) {
    console.error('Lỗi gửi lời mời kết bạn từ hồ sơ:', err);
  }
};

const handleCancelFriendRequestFromProfile = async (contact) => {
  if (!contact?.id) return;
  try {
    let fId = contact.friendshipId || pendingMap.value.get(contact.id);
    if (!fId) {
      const rel = await getFriendshipStatus(contact.id);
      fId = rel?.friendshipId;
    }
    if (fId) {
      await cancelFriendRequest(fId);
    }
    pendingIds.value.delete(contact.id);
    pendingMap.value.delete(contact.id);
    if (selectedMemberProfile.value && selectedMemberProfile.value.id === contact.id) {
      selectedMemberProfile.value.isPending = false;
      selectedMemberProfile.value.friendshipId = null;
    }
  } catch (err) {
    console.error('Lỗi thu hồi lời mời kết bạn:', err);
  }
};

//Xy ly huy ket ban o profile
const handleUnfriendFromProfile = async (contact) => {
  if (!contact?.id) return;
  isProfileMenuOpen.value = false;
  try {
    let fId = contact.friendshipId || friendShipMap.value.get(contact.id);
    if (!fId) {
      const rel = await getFriendshipStatus(contact.id);
      fId = rel?.friendshipId;
    }
    if (fId) {
      await cancelFriendRequest(fId);
    }
    friendIds.value.delete(contact.id);
    friendShipMap.value.delete(contact.id);
    if (selectedMemberProfile.value && selectedMemberProfile.value.id === contact.id) {
      selectedMemberProfile.value.isFriend = false;
      selectedMemberProfile.value.friendshipId = null;
    }
  } catch (err) {
    console.error('Lỗi hủy kết bạn:', err);
  }
};

const handleToggleBlockFromProfile = async (contact) => {
  if (!contact?.id) return;
  isProfileMenuOpen.value = false;
  try {
    if (contact.isBlocked) {
      // Đang chặn -> Bỏ chặn
      await unblockUser(contact.id);
      blockedIds.value.delete(contact.id);
      if (selectedMemberProfile.value && selectedMemberProfile.value.id === contact.id) {
        selectedMemberProfile.value.isBlocked = false;
        selectedMemberProfile.value.isFriend = false;
        selectedMemberProfile.value.isPending = false;
        selectedMemberProfile.value.friendshipId = null;
      }
    } else {
      // Chưa chặn -> Chặn người dùng
      await blockUser(contact.id);
      blockedIds.value.add(contact.id);
      friendIds.value.delete(contact.id);
      friendShipMap.value.delete(contact.id);
      pendingIds.value.delete(contact.id);
      pendingMap.value.delete(contact.id);
      if (selectedMemberProfile.value && selectedMemberProfile.value.id === contact.id) {
        selectedMemberProfile.value.isBlocked = true;
        selectedMemberProfile.value.isFriend = false;
        selectedMemberProfile.value.isPending = false;
      }
    }
  } catch (err) {
    console.error('Lỗi thao tác chặn / bỏ chặn:', err);
  }
};

const handleNavigateToContacts = (contact) => {
  closeMemberProfile();
  emit('close');
  router.push({
    path: '/contacts',
    query: { userId: contact.id }
  });
};

const handleDirectMessageWithMember = async (contact) => {
  selectedMemberProfile.value = null;
  if (!contact?.id) return;
  // Không tự mở tin nhắn 1-1 với chính mình
  if (authStore.user?.userId && contact.id === authStore.user.userId) {
    return;
  }
  emit('close');
  await chatStore.startDirectChat(contact.id);
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Quicksand:wght@400;500;600;700&display=swap');

.chat-info-sidebar {
  width: 340px;
  min-width: 340px;
  max-width: 340px;
  height: 100%;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  user-select: text;
  box-sizing: border-box;
  overflow: hidden;
  flex-shrink: 0;
}

.chat-info-sidebar-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  overflow: hidden;
}

/* 1. Sidebar Header */
.sidebar-header {
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #f1f5f9;
  flex-shrink: 0;
  background-color: #ffffff;
}

.header-title h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.btn-close {
  background: none;
  border: none;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  color: #64748b;
  font-size: 15px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.btn-close:hover {
  background: #f1f5f9;
  color: #1e293b;
}

/* Sub-view Header (Quay lại) */
.subview-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.btn-back {
  background: none;
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  color: #475569;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}

.subview-header-left:hover .btn-back {
  background: #eff6ff;
  color: #2563eb;
}

.subview-header-left:hover .subview-title h3 {
  color: #2563eb;
}

.subview-title h3 {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  transition: color 0.15s ease;
}

/* Content cuộn của màn hình chính */
.sidebar-scroll-content {
  flex: 1;
  overflow-y: auto;
  scrollbar-gutter: stable;
  padding: 10px 0 16px;
}

.sidebar-scroll-content::-webkit-scrollbar,
.subview-gallery-scroll::-webkit-scrollbar {
  width: 5px;
}

.sidebar-scroll-content::-webkit-scrollbar-thumb,
.subview-gallery-scroll::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

.sidebar-scroll-content::-webkit-scrollbar-thumb:hover,
.subview-gallery-scroll::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* 2. Room Profile */
.room-profile-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 18px 8px;
  text-align: center;
}

.room-avatar-wrapper {
  position: relative;
  display: inline-flex;
  border-radius: 50%;
  overflow: hidden;
}

.room-avatar-clickable {
  cursor: pointer;
}

.room-avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.room-avatar-clickable:hover .room-avatar-overlay {
  opacity: 1;
}

.overlay-cam-icon {
  color: #ffffff;
  font-size: 22px;
}

.room-title-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  max-width: 100%;
  margin-top: 8px;
  padding: 0 4px;
}

.room-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: calc(100% - 36px);
}

.room-title-interactive {
  cursor: pointer;
  user-select: none;
}

.btn-edit-room-name {
  background: none;
  border: none;
  width: 28px;
  height: 28px;
  min-width: 28px;
  border-radius: 6px;
  color: #64748b;
  font-size: 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.btn-edit-room-name:hover {
  background: #f1f5f9;
  color: #2563eb;
}

/* Inline Edit Box */
.room-title-edit-box {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  max-width: 240px;
  margin-top: 8px;
}

.room-name-inline-input {
  flex: 1;
  padding: 5px 10px;
  font-size: 14px;
  font-family: inherit;
  font-weight: 600;
  color: #0f172a;
  background: #ffffff;
  border: 1.5px solid #3b82f6;
  border-radius: 6px;
  outline: none;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);
}

.room-name-inline-input:focus {
  border-color: #2563eb;
}

.edit-btn-group {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.edit-mini-btn {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  line-height: 1;
  padding: 0;
  cursor: pointer;
  transition: all 0.15s ease;
}

.edit-mini-btn.btn-save {
  background-color: #2563eb;
  color: #ffffff;
}

.edit-mini-btn.btn-save:hover {
  background-color: #1d4ed8;
  transform: scale(1.1);
}

.edit-mini-btn.btn-cancel {
  background-color: rgba(0, 0, 0, 0.08);
  color: #64748b;
}

.edit-mini-btn.btn-cancel:hover {
  background-color: rgba(0, 0, 0, 0.15);
  color: #1e293b;
  transform: scale(1.1);
}

.edit-mini-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* 3. Notification Setting */
.notification-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 18px;
  margin: 0;
}

.setting-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.setting-icon-wrapper {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #eff6ff;
  color: #3b82f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: all 0.2s ease;
}

.setting-icon-wrapper.muted {
  background: #f1f5f9;
  color: #94a3b8;
}

.setting-text {
  display: flex;
  flex-direction: column;
}

.setting-title {
  font-size: 13.5px;
  font-weight: 600;
  color: #334155;
}

.setting-subtext {
  font-size: 11.5px;
  color: #94a3b8;
}

/* Toggle Switch */
.switch {
  position: relative;
  display: inline-block;
  width: 38px;
  height: 22px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  inset: 0;
  background-color: #cbd5e1;
  transition: 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  border-radius: 34px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  border-radius: 50%;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
}

input:checked + .slider {
  background-color: #3b82f6;
}

input:checked + .slider:before {
  transform: translateX(16px);
}

.divider {
  border: none;
  height: 1px;
  background-color: #f1f5f9;
  margin: 6px 0;
}

/* 4. Accordion Section */
.accordion-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 18px;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.15s ease;
}

.accordion-header:hover {
  background-color: #f8fafc;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-icon {
  font-size: 14px;
  color: #64748b;
}

.section-title {
  font-size: 13.5px;
  font-weight: 700;
  color: #334155;
}

.arrow-icon {
  font-size: 12px;
  color: #94a3b8;
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  transform: rotate(0deg);
}

.arrow--down {
  transform: rotate(90deg);
}

.accordion-body {
  padding: 4px 18px 8px;
}

/* Nút thêm thành viên vào nhóm */
.add-member-action-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
  padding: 6px 8px;
}

.add-member-action-bar:hover {
  background-color: #f1f5f9;
}

.add-member-action-bar:hover .add-member-text {
  color: #2563eb;
}

.add-member-icon-circle {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background-color: #eff6ff;
  color: #3b82f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.add-member-action-bar:hover .add-member-icon-circle {
  background-color: #3b82f6;
  color: #ffffff;
  transform: scale(1.05);
}

.add-member-text {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  transition: color 0.15s ease;
}

/* Members list */
.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 8px;
  border-radius: 8px;
  transition: all 0.25s ease;
  user-select: none;
}

.member-item--clickable {
  cursor: pointer;
}

.member-item--clickable:hover {
  background-color: #f1f5f9;
}

.member-item--clickable:hover .member-name {
  color: #2563eb;
}

.member-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.member-name {
  font-size: 13px;
  color: #1e293b;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.role-badge {
  font-size: 10.5px;
  font-weight: 600;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #dbeafe;
  padding: 1px 6px;
  border-radius: 4px;
  width: fit-content;
  margin-top: 2px;
}

.empty-members-hint {
  font-size: 12px;
  color: #94a3b8;
  padding: 8px 0;
  text-align: center;
}

/* 5. Media Tabs ngang hàng */
.media-tabs-bar {
  display: flex;
  gap: 4px;
  background: #f1f5f9;
  padding: 3px;
  border-radius: 9px;
  margin-bottom: 8px;
}

.tab-btn {
  flex: 1;
  border: none;
  background: transparent;
  padding: 6px 0;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  border-radius: 7px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  font-family: 'Quicksand', sans-serif;
}

.tab-btn.active {
  background: #ffffff;
  color: #2563eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.tab-icon {
  font-size: 11px;
}

/* Grid ảnh & video */
.media-grid,
.subview-media-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}

.media-thumbnail-card {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  background-color: #f1f5f9;
  cursor: pointer;
  border: 1px solid #e2e8f0;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.media-thumbnail-card:hover {
  transform: scale(1.02);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.media-thumbnail-card img,
.media-thumbnail-card video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* Fallback khi ảnh/video trong ChatInfoSidebar không tải được */
.media-fallback-box {
  width: 100%;
  height: 100%;
  background-color: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
}

.media-fallback-icon {
  font-size: 20px;
  color: #94a3b8;
}

.video-preview-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.play-overlay-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #ffffff;
  font-size: 11px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(2px);
}

/* Danh sách file */
.files-list,
.subview-files-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.file-item-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  text-decoration: none;
  transition: all 0.15s ease;
}

.file-item-card:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.file-icon-box {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: #e2e8f0;
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}

.file-meta {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex: 1;
}

.file-name {
  font-size: 12px;
  color: #1e293b;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-size {
  font-size: 10.5px;
  color: #94a3b8;
}

.file-download-icon {
  font-size: 11px;
  color: #94a3b8;
  flex-shrink: 0;
}

/* Nút Xem tất cả (Mở Sub-view Gallery) */
.btn-open-gallery {
  width: 100%;
  padding: 8px 12px;
  margin-top: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #2563eb;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-family: 'Quicksand', sans-serif;
  transition: all 0.15s ease;
}

.btn-open-gallery:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1d4ed8;
}

.btn-arrow {
  font-size: 11px;
}

.empty-media-hint {
  grid-column: span 3;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
  padding: 16px 0;
}

/* ============================================================ */
/* SUB-VIEW CHUYÊN DỤNG XEM TOÀN BỘ FILE PHƯƠNG TIỆN (ZALO / TELEGRAM) */
/* ============================================================ */
.subview-media-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: calc(100% - 70px);
  overflow: hidden;
}

.subview-tabs-bar {
  display: flex;
  background: #f1f5f9;
  padding: 3px;
  border-radius: 9px;
  margin: 12px 16px 8px;
  gap: 4px;
  flex-shrink: 0;
}

.subview-tab-btn {
  flex: 1;
  border: none;
  background: transparent;
  padding: 7px 0;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  border-radius: 7px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: 'Quicksand', sans-serif;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.subview-tab-btn.active {
  background: #ffffff;
  color: #2563eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.subview-gallery-scroll {
  flex: 1;
  overflow-y: auto;
  scrollbar-gutter: stable;
  padding: 6px 16px 24px;
}

/* Accordion Transition - Chuẩn CSS Grid 0fr -> 1fr  */
.accordion-collapse {
  display: grid;
  grid-template-rows: 0fr;
  transition: grid-template-rows 0.22s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.18s ease;
  opacity: 0;
}

.accordion-collapse.is-expanded {
  grid-template-rows: 1fr;
  opacity: 1;
}

.accordion-collapse-inner {
  overflow: hidden;
}

/* Popup Xem hồ sơ thành viên (tái sử dụng ContactProfileCard) */
.profile-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 20px;
  animation: modalFadeIn 0.2s ease-out;
}

.profile-modal-card-wrap {
  position: relative;
  max-width: 500px;
  width: 100%;
  display: flex;
  justify-content: center;
  animation: modalScaleUp 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.profile-modal-card-wrap :deep(.profile-card) {
  width: 100% !important;
  max-width: 500px;
  box-shadow: 0 20px 40px -15px rgba(0, 0, 0, 0.25), 0 0 0 1px rgba(0, 0, 0, 0.05);
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes modalScaleUp {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(8px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.profile-modal-top-actions {
  position: absolute;
  top: 14px;
  right: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 50;
}

.profile-menu-wrapper {
  position: relative;
}

.profile-action-btn {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.92);
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  transition: all 0.18s cubic-bezier(0.16, 1, 0.3, 1);
  font-size: 14px;
}

.profile-action-btn:hover {
  background: #ffffff;
  color: #0f172a;
  transform: scale(1.08);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.16);
}

.profile-action-btn.active {
  background: #ffffff;
  color: #2563eb;
  transform: scale(1.08);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
}

.profile-menu-dropdown {
  position: absolute;
  top: 42px;
  right: 0;
  min-width: 205px;
  background: #ffffff;
  border-radius: 14px;
  padding: 6px;
  box-shadow: 0 12px 30px -4px rgba(0, 0, 0, 0.18), 0 0 0 1px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  gap: 3px;
  z-index: 60;
  animation: modalScaleUp 0.18s cubic-bezier(0.16, 1, 0.3, 1);
}

.menu-dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: none;
  background: transparent;
  border-radius: 9px;
  font-family: 'Quicksand', sans-serif;
  font-size: 13.5px;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  text-align: left;
  width: 100%;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.menu-dropdown-item:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.menu-dropdown-item.item-danger {
  color: #ef4444;
}

.menu-dropdown-item.item-danger:hover {
  background: #fef2f2;
  color: #dc2626;
}

.menu-dropdown-item.item-primary {
  color: #2563eb;
}

.menu-dropdown-item.item-primary:hover {
  background: #eff6ff;
  color: #1d4ed8;
}

.menu-dropdown-item .item-icon {
  width: 16px;
  font-size: 14px;
  text-align: center;
  flex-shrink: 0;
}

/* =========================================================================
   STYLES CHO MỤC LỊCH HẸN & NHẮC NHỞ TRÊN SIDEBAR
   ========================================================================= */
.rem-subview-header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-header-create-rem {
  padding: 5px 10px;
  border-radius: 8px;
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #2563eb;
  font-family: 'Quicksand', sans-serif;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  transition: all 0.15s;
}

.btn-header-create-rem:hover {
  background: #dbeafe;
}

.rem-preview-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 4px;
}

.rem-preview-card {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.15s;
}

.rem-preview-card:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.rem-mini-leaf {
  width: 42px;
  flex-shrink: 0;
  border-radius: 9px;
  border: 1px solid #dbeafe;
  background: #ffffff;
  overflow: hidden;
  text-align: center;
  box-shadow: 0 1px 3px rgba(37, 99, 235, 0.06);
}

.rem-mini-leaf.rem-leaf-lg {
  width: 50px;
  border-radius: 11px;
}

.rem-mini-leaf.is-personal {
  border-color: #e0e7ff;
}

.rem-mini-leaf.is-cancelled {
  border-color: #e2e8f0;
  opacity: 0.6;
}

.rem-mini-month {
  background: #2563eb;
  color: #ffffff;
  font-size: 8px;
  font-weight: 800;
  padding: 2px;
  text-transform: uppercase;
}

.rem-mini-leaf.is-personal .rem-mini-month {
  background: #4f46e5;
}

.rem-mini-leaf.is-cancelled .rem-mini-month {
  background: #94a3b8;
}

.rem-mini-day {
  padding: 4px 2px;
  font-size: 15px;
  font-weight: 800;
  color: #2563eb;
  line-height: 1;
}

.rem-mini-leaf.rem-leaf-lg .rem-mini-day {
  padding: 6px 2px;
  font-size: 18px;
}

.rem-mini-leaf.is-personal .rem-mini-day {
  color: #4f46e5;
}

.rem-mini-leaf.is-cancelled .rem-mini-day {
  color: #64748b;
  text-decoration: line-through;
}

.rem-preview-info {
  flex: 1;
  min-width: 0;
}

.rem-preview-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.rem-preview-title {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rem-personal-tag {
  font-size: 9px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 4px;
  background: #eef2ff;
  color: #4f46e5;
  flex-shrink: 0;
}

.rem-preview-time {
  margin: 2px 0 0;
  font-size: 11px;
  font-weight: 700;
  color: #2563eb;
}

.rem-preview-meta {
  margin-top: 4px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  color: #64748b;
}

.rsvp-tag-ok {
  color: #059669;
  font-weight: 700;
}

.rsvp-tag-no {
  color: #e11d48;
  font-weight: 700;
}

.rsvp-tag-wait {
  color: #d97706;
  font-weight: 700;
}

/* Sub-view Danh sách lịch hẹn */
.rem-subview-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
}

.rem-subview-card {
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  padding: 12px;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.04);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rem-subview-top {
  display: flex;
  align-items: flex-start;
  gap: 11px;
}

.rem-subview-main {
  flex: 1;
  min-width: 0;
}

.rem-subview-badges {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.rem-badge-status {
  font-size: 10px;
  font-weight: 800;
  text-transform: uppercase;
  padding: 2px 8px;
  border-radius: 999px;
}

.rem-badge-status.st-pending {
  background: #dbeafe;
  color: #1d4ed8;
}

.rem-badge-status.st-personal {
  background: #e0e7ff;
  color: #4338ca;
}

.rem-badge-status.st-done {
  background: #d1fae5;
  color: #047857;
}

.rem-badge-status.st-cancelled {
  background: #fee2e2;
  color: #dc2626;
}

.rem-jump-link {
  border: none;
  background: transparent;
  color: #2563eb;
  font-family: 'Quicksand', sans-serif;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  padding: 0;
}

.rem-jump-link:hover {
  text-decoration: underline;
}

.rem-subview-title {
  margin: 5px 0 0;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  word-break: break-word;
}

.rem-subview-card.is-cancelled .rem-subview-title {
  color: #94a3b8;
  text-decoration: line-through;
}

.rem-subview-time {
  margin: 3px 0 0;
  font-size: 12px;
  font-weight: 700;
  color: #2563eb;
}

.rem-subview-desc {
  margin: 5px 0 0;
  font-size: 12px;
  color: #475569;
  word-break: break-word;
}

.rem-subview-author {
  margin: 5px 0 0;
  font-size: 11px;
  color: #64748b;
}

.rem-subview-actions {
  padding-top: 8px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  gap: 6px;
}

.rem-sb-btn {
  flex: 1;
  padding: 6px 8px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  color: #334155;
  font-family: 'Quicksand', sans-serif;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
}

.rem-sb-btn.btn-sb-accept:hover {
  background: #ecfdf5;
  border-color: #10b981;
  color: #059669;
}

.rem-sb-btn.btn-sb-accept.active {
  background: #059669;
  border-color: #059669;
  color: #ffffff;
}

.rem-sb-btn.btn-sb-decline:hover {
  background: #fff1f2;
  border-color: #f43f5e;
  color: #e11d48;
}

.rem-sb-btn.btn-sb-decline.active {
  background: #e11d48;
  border-color: #e11d48;
  color: #ffffff;
}

.rem-sb-cancel-btn {
  padding: 6px 10px;
  border-radius: 8px;
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #dc2626;
  cursor: pointer;
}

.rem-sb-cancel-btn:hover {
  background: #fee2e2;
}

.rem-subview-finished-hint {
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
}
</style>
