/**
 * Utility phát âm thanh thông báo "ting" nhẹ nhàng bằng Web Audio API
 * Không phụ thuộc file mp3 ngoài, nhẹ, không bao giờ lỗi đường dẫn.
 */
let audioCtx = null

export function playNotificationSound() {
  try {
    const AudioContextClass = window.AudioContext || window.webkitAudioContext
    if (!AudioContextClass) return

    if (!audioCtx) {
      audioCtx = new AudioContextClass()
    }

    if (audioCtx.state === 'suspended') {
      audioCtx.resume()
    }

    const now = audioCtx.currentTime

    // Tạo bộ dao động chính (Chime tone 1: 880Hz - A5)
    const osc1 = audioCtx.createOscillator()
    const gain1 = audioCtx.createGain()

    osc1.type = 'sine'
    osc1.frequency.setValueAtTime(880, now)
    osc1.frequency.exponentialRampToValueAtTime(1320, now + 0.12)

    // Tạo độ vang nhẹ (Chime tone 2: 1760Hz - A6)
    const osc2 = audioCtx.createOscillator()
    const gain2 = audioCtx.createGain()

    osc2.type = 'triangle'
    osc2.frequency.setValueAtTime(1760, now)

    // Âm lượng êm ái, giảm dần theo đường cong exponential
    gain1.gain.setValueAtTime(0.18, now)
    gain1.gain.exponentialRampToValueAtTime(0.001, now + 0.5)

    gain2.gain.setValueAtTime(0.08, now)
    gain2.gain.exponentialRampToValueAtTime(0.001, now + 0.35)

    // Kết nối vào output
    osc1.connect(gain1)
    gain1.connect(audioCtx.destination)

    osc2.connect(gain2)
    gain2.connect(audioCtx.destination)

    osc1.start(now)
    osc1.stop(now + 0.55)

    osc2.start(now)
    osc2.stop(now + 0.4)
  } catch (err) {
    // Tránh gián đoạn app nếu thiết bị chặn audio context
    console.debug('Không thể phát âm thanh thông báo:', err)
  }
}
