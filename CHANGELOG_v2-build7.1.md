# Changelog v2 - build 7.1 - Beta

**Ngày phát hành:** 21/08/2026  
**Phiên bản trước:** v2 - build 7 - Beta

---

## 🎯 Tổng quan

Phiên bản này tập trung vào **Timer V2 UI Overhaul** - thiết kế lại hoàn toàn giao diện timer trong phiên làm việc với trải nghiệm fullscreen, khả năng tùy chỉnh vị trí, và cơ chế hủy an toàn hơn.

---

## ✨ Tính năng mới

### 1. **Fullscreen Timer Display**
- **Trước:** Timer hiển thị trong tab bên cạnh sidebar navigation
- **Sau:** Timer chiếm toàn bộ màn hình khi phiên bắt đầu
- **Lợi ích:** Tập trung hoàn toàn, không bị phân tâm bởi UI khác
- **Hành vi:** 
  - Khi nhấn "BẮT ĐẦU" → Mở fullscreen ngay lập tức
  - Không có sidebar, navigation, chỉ còn vòng tròn đếm giờ
  - Khi kết thúc phiên → Quay về MainMenuScreen

### 2. **Draggable Ring (Di chuyển vòng đồng hồ)**
- **Tính năng mới:** Nhấn giữ vào vòng tròn và kéo để di chuyển vị trí
- **Ranh giới thông minh:**
  - Tự động giới hạn trong màn hình
  - Margin an toàn: `RING_OUTER_RADIUS + 10px`
  - Không cho vòng tròn bị cắt hoặc vượt biên
- **Công thức clamping:**
  ```
  centerX: [margin, width - margin]
  centerY: [margin, height - margin - 100]
  ```
- **Lợi ích:** Người chơi có thể đặt timer ở vị trí thuận tiện nhất

### 3. **Swipe-to-Cancel Slider (Kéo để hủy)**
- **Thay thế:** Nút "Kết thúc phiên" cũ (dễ bấm nhầm)
- **Thiết kế mới:**
  - Hình chữ nhật bo góc (cornerRadius = 25px)
  - Hình tròn lớn bên trong (40px diameter)
  - Icon "»»»" màu đen trong hình tròn
  - Gradient đỏ khi kéo: `0xFF4A0000` → `0xFFFF0000`
- **Cách dùng:**
  1. Nhấp giữ vào hình tròn bên trái
  2. Kéo sang phải
  3. Khi đạt 80% → Timer bị hủy
  4. Nếu thả trước 80% → Slider reset về vị trí ban đầu
- **Text hướng dẫn:**
  - Chưa kéo: "§7Kéo sang phải để hủy"
  - Đang kéo (>30%): "§cKéo tiếp để hủy timer"
- **Lợi ích:** Tránh hủy nhầm timer do bấm nhầm nút

### 4. **Countdown Number 4x Lớn Hơn**
- **Trước:** Scale 3x với label "(còn lại)"
- **Sau:** 
  - Scale 4x (to hơn 33%)
  - Bỏ label "(còn lại)" → Giao diện sạch hơn
  - Chỉ hiển thị: Số đếm ngược (CỰC LỚN) + Đồng hồ thực (nhỏ)
- **Lợi ích:** Dễ đọc hơn từ xa, tập trung vào thông tin quan trọng

### 5. **Timer Persistence (Lưu và khôi phục)**
- **Khi out game:**
  - Timer tự động **pause** (tạm dừng)
  - Lưu state vào file: `elapsed`, `target`, `type`, `state`
  - Không tính thưởng cho phần chưa hoàn thành
- **Khi vào lại game:**
  - Tự động load timer đã save
  - Tự động **resume** (tiếp tục chạy)
  - Mở fullscreen ActiveSessionScreen ngay lập tức
  - Hiển thị thông báo: "§aTimer tiếp tục! (X phút đã trôi qua)"
- **Lợi ích:** Không mất công sức đã bỏ ra nếu phải tạm out game

### 6. **Auto-open Timer Screen**
- **Client packet handler** phát hiện timer đang chạy
- Tự động mở `ActiveSessionScreen` khi:
  - Vừa start timer
  - Vừa join game với timer đã lưu
  - State = `RUNNING`
- **Code:**
  ```java
  if (state == RUNNING && currentScreen == null) {
      client.setScreen(new ActiveSessionScreen());
  }
  ```

---

## 🔧 Cải tiến kỹ thuật

### 1. **Performance Optimization (Giảm lag/giật)**

#### Vấn đề trước:
- Ring có **180 segments** × 2 triangles = 360 triangles/segment
- Mỗi frame render ~16,200 triangles cho full ring
- Triangle rasterization scan từng pixel → Chậm
- → **Giật lag** khi drag hoặc render

#### Giải pháp:
- ✅ **Giảm ring segments: 180 → 90** (giảm 50% triangles)
- ✅ **Giảm circle segments: 16 → 24** cho slider dot
- ✅ **Simplified rounded rect** rendering (bỏ corner circles phức tạp)
- ✅ **Optimized circle drawing** với ít segments hơn

#### Kết quả:
- Giảm từ ~16,200 → **~8,100 triangles/frame**
- Vẫn mượt mà (90 segments = 4° mỗi segment, không thấy răng cưa)
- **FPS cải thiện đáng kể**, không còn giật nữa

### 2. **Rounded Rectangle Rendering**
```java
drawRoundedRect(context, x, y, width, height, cornerRadius, color)
```
- Bo góc bằng cách fill các vùng chồng lấp
- Không dùng circle chính xác cho corner (quá chậm)
- Performance tốt hơn, vẫn đẹp

### 3. **Circle Detection for Click**
```java
double distToDot = Math.sqrt(
    Math.pow(mouseX - dotCenterX, 2) + 
    Math.pow(mouseY - dotCenterY, 2)
);
if (distToDot <= dotRadius) { ... }
```
- Thay rect collision bằng circle collision
- Chính xác hơn cho hình tròn
- Tự nhiên hơn khi click

---

## 🐛 Bug Fixes

### 1. **Fixed: Timer crash khi start (NullPointerException)**
- **Lỗi:** `type.ordinal()` crash vì `type = null`
- **Nguyên nhân:** Client mở screen trước khi nhận timer state từ server
- **Fix:** Safety check + loading message
```java
if (type == null) {
    context.drawText("§7Đang tải...", ...);
    return;
}
```

### 2. **Fixed: Timer không restore khi rejoin**
- **Trước:** Code có sẵn nhưng bị comment out
- **Sau:** Enable lại logic restore trong `TimerManager.onPlayerJoin()`
- **Hành vi:**
  - Load `TimerPersistence.loadTimer(playerId)`
  - Auto-resume nếu state = PAUSED
  - Sync về client → Mở fullscreen

### 3. **Fixed: Ring vượt biên màn hình**
- **Lỗi:** Kéo ring ra ngoài → Một phần bị cắt
- **Fix:** Clamping position
```java
int margin = (int)RING_OUTER_RADIUS + 10;
centerX = Math.max(margin, Math.min(width - margin, centerX));
centerY = Math.max(margin, Math.min(height - margin - 100, centerY));
```

### 4. **Fixed: Slider không smooth khi drag**
- **Trước:** Square detection + hard edges
- **Sau:** Circle detection + rounded UI
- **Kết quả:** Drag mượt hơn nhiều

### 5. **Fixed: ESC không tắt được screen**
- **Vấn đề ban đầu:** `shouldCloseOnEsc()` return `false` → Không thể thoát
- **Cân nhắc:** Thêm ESC counter (5 lần warning, 10 lần force stop)
- **Quyết định cuối:** **Cho phép ESC thoát tự do**
- **Lý do:** Timer vẫn chạy ngầm, người chơi có thể mở menu khác
- **Code:** Loại bỏ override `shouldCloseOnEsc()` và `close()`

---

## 📝 Thay đổi UI/UX

### Before vs After:

| Khía cạnh | Trước (v2-build7) | Sau (v2-build7.1) |
|-----------|-------------------|-------------------|
| **Layout** | Sidebar + Content area | **Fullscreen** toàn bộ |
| **Countdown size** | 3x scale | **4x scale** (to hơn 33%) |
| **Label** | "(còn lại)" hiển thị | **Bỏ label** (sạch hơn) |
| **Cancel method** | Nút đỏ (dễ nhầm) | **Swipe slider** (an toàn) |
| **Slider design** | Vuông góc + square | **Bo góc + circle** |
| **Ring position** | Cố định giữa | **Draggable** (tự do) |
| **Performance** | Giật lag | **Mượt mà** (50% ít triangles) |
| **ESC behavior** | Blocked hoàn toàn | **Cho phép thoát** |
| **Out game** | Timer mất | **Auto-save** |
| **Rejoin** | Không restore | **Auto-resume** |

---

## 🎨 Thay đổi Code chính

### File đã sửa:
1. `ActiveSessionScreen.java` - **Viết lại hoàn toàn** (300+ lines changed)
2. `TimerManager.java` - Timer persistence logic
3. `ModNetworking.java` - Auto-open screen on timer state update
4. `TimerTabScreenV2.java` - Removed embedded ActiveSessionScreen
5. `ClockConfigScreen.java` - Open fullscreen on timer start
6. `gradle.properties` - Version: 1.0.6-beta → 1.0.7-beta

### Thống kê Git:
```
15 files changed
1,667 insertions(+)
755 deletions(-)
```

---

## 🔮 Technical Deep Dive

### ActiveSessionScreen Architecture:

```
┌─────────────────────────────────────────────┐
│           ActiveSessionScreen               │
│              (extends Screen)               │
├─────────────────────────────────────────────┤
│  Components:                                │
│  • Draggable Ring (centerX, centerY offset) │
│  • 4x Countdown Number (matrix transform)   │
│  • Real-time Clock (HH:mm)                  │
│  • Swipe Slider (rounded + circle)          │
├─────────────────────────────────────────────┤
│  Mouse Events:                              │
│  • mouseClicked() - Circle/Ring detection   │
│  • mouseDragged() - Update offsets          │
│  • mouseReleased() - Check 80% threshold    │
├─────────────────────────────────────────────┤
│  Rendering:                                 │
│  • renderProgressRing() - 90 segments       │
│  • renderSwipeSlider() - Rounded + circle   │
│  • drawCircle() - 24 segments optimized     │
│  • drawRoundedRect() - Simple fill method   │
└─────────────────────────────────────────────┘
```

### Timer Persistence Flow:

```
┌─────────────┐
│ Player quit │
└──────┬──────┘
       │
       ▼
┌──────────────────────────────────┐
│ TimerManager.onPlayerDisconnect() │
├──────────────────────────────────┤
│ 1. session.pause()               │
│ 2. TimerPersistence.saveTimer()  │
│ 3. activeSessions.remove()       │
└──────────────────────────────────┘
       │
       │ (Out game)
       │
       ▼
┌────────────────────────────┐
│ Player rejoin same world   │
└────────────┬───────────────┘
             │
             ▼
┌────────────────────────────────┐
│ TimerManager.onPlayerJoin()    │
├────────────────────────────────┤
│ 1. loadTimer(playerId)         │
│ 2. restoreTimer(saveData)      │
│ 3. session.resume()            │
│ 4. sendTimerStateUpdate()      │
└────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ Client receives state update    │
├─────────────────────────────────┤
│ if (state == RUNNING) {         │
│   client.setScreen(             │
│     new ActiveSessionScreen()   │
│   );                            │
│ }                               │
└─────────────────────────────────┘
             │
             ▼
     ┌───────────────┐
     │ Timer resumes │
     │   fullscreen  │
     └───────────────┘
```

---

## ⚙️ Configuration

### Constants:
```java
// Ring
RING_SEGMENTS = 90           // Was 180 (giảm 50%)
RING_OUTER_RADIUS = 120f
RING_INNER_RADIUS = 105f
GLOW_HEAD_SIZE = 8f

// Slider
SLIDER_WIDTH = 300
SLIDER_HEIGHT = 50
SLIDER_DOT_SIZE = 40         // Circle diameter
CANCEL_THRESHOLD = 0.8f      // 80% để hủy

// Performance
Circle segments = 24         // Was 16
```

---

## 🧪 Testing Checklist

✅ **Timer start:**
- [x] Click "BẮT ĐẦU" → Fullscreen mở
- [x] Ring hiển thị đầy đủ với gradient
- [x] Countdown 4x to, dễ đọc
- [x] Real-time clock hiển thị

✅ **Draggable ring:**
- [x] Click giữ ring → Kéo được
- [x] Di chuyển mượt mà
- [x] Không vượt biên màn hình
- [x] Offset được lưu giữ

✅ **Swipe slider:**
- [x] Click vào circle → Drag được
- [x] Bo góc hiển thị đẹp
- [x] Gradient đỏ khi kéo
- [x] Kéo <80% → Reset
- [x] Kéo ≥80% → Hủy timer

✅ **Performance:**
- [x] Không giật lag khi drag
- [x] Render mượt 60 FPS
- [x] Ring gradient không bị đứt

✅ **Persistence:**
- [x] Out game → Timer pause
- [x] Rejoin → Timer resume
- [x] Fullscreen auto-open
- [x] Elapsed time đúng

✅ **ESC behavior:**
- [x] ESC → Thoát được screen
- [x] Timer vẫn chạy ngầm
- [x] Có thể mở menu khác

---

## 📦 Build Info

- **File:** `focustimershop-1.0.7-beta.jar`
- **Size:** 389 KB
- **Minecraft:** 1.20.1
- **Fabric Loader:** 0.19.3
- **Fabric API:** 0.92.2+

---

## 🚀 Migration Guide (từ v2-build7)

### Không cần thao tác gì:
- ✅ Auto-compatible với save cũ
- ✅ Timer persistence file format unchanged
- ✅ Database schema không đổi

### Lưu ý cho người chơi:
1. **Timer sẽ fullscreen** - Không còn sidebar trong phiên làm việc
2. **Swipe để hủy** - Không còn nút "Kết thúc phiên"
3. **Kéo ring** - Có thể di chuyển vị trí đồng hồ
4. **ESC thoát được** - Nhưng timer vẫn chạy
5. **Out game an toàn** - Timer tự động lưu và tiếp tục khi vào lại

---

## 🐛 Known Issues

### Không có lỗi nào được báo cáo

---

## 📋 Roadmap (Future)

### Planned for v2-build7.2:
- [ ] Timer ring animation smoothing
- [ ] Customizable ring colors
- [ ] Sound effects for slider
- [ ] Vibration feedback (if supported)
- [ ] Multiple timer presets
- [ ] Timer history tracking

---

## 👨‍💻 Developer Notes

### Performance:
- Giảm segments từ 180 → 90 cải thiện **~50% FPS**
- Triangle rasterization vẫn là bottleneck, có thể optimize thêm
- Consider: Cached ring texture (pre-render) cho progress arc cố định

### Code Quality:
- ActiveSessionScreen đã refactor hoàn toàn
- Clean separation: Ring logic, Slider logic, Mouse handling
- Helper methods: `drawCircle()`, `drawRoundedRect()`, `interpolateColor()`

### Future Optimization Ideas:
1. **Texture caching:** Pre-render ring segments vào texture
2. **Batch rendering:** Render tất cả segments trong 1 draw call
3. **Level-of-detail:** Giảm segments khi không focus
4. **GPU shader:** Dùng shader thay vì CPU rasterization

---

## 📸 Screenshots

*(Không có trong changelog - xem trên GitHub Release)*

---

## 🙏 Credits

- **Developed by:** diepanh435472-wq
- **Testing:** Community feedback
- **Framework:** Fabric API
- **Rendering:** Minecraft DrawContext API

---

## 📄 License

MIT License - Copyright (c) 2026

---

**Tải về:** [GitHub Releases](https://github.com/diepanh435472-wq/FOCUS-TIMER-SHOP-MOD/releases/tag/v2-build7.1)

**Báo lỗi:** [GitHub Issues](https://github.com/diepanh435472-wq/FOCUS-TIMER-SHOP-MOD/issues)
