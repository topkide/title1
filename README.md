# 🏆 DotorimaruTitle

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-brightgreen.svg)](https://www.minecraft.net/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**도토리마을 칭호 시스템** - Minecraft Purpur 1.21.8용 고급 칭호 관리 플러그인

[한국어](#한국어) | [English](#english)

---

## 한국어

### 📋 소개

DotorimaruTitle은 Minecraft 서버를 위한 전문적인 칭호 시스템 플러그인입니다. 칭호북을 통한 획득, GUI 기반 관리, PlaceholderAPI 연동, Redis를 통한 멀티서버 동기화를 지원합니다.

### ✨ 주요 기능

- 🎫 **칭호북 시스템**: 우클릭으로 간편하게 칭호 획득
- 🎨 **RGB 색상 지원**: Legacy 색상 코드(`&`) + RGB 색상(`#RRGGBB`) 모두 지원
- 📦 **GUI 인벤토리**: 54칸 크기의 직관적인 칭호 관리 UI
- ⚡ **멀티서버 동기화**: Redis Pub/Sub를 통한 실시간 동기화
- 💾 **영구 저장**: MySQL 데이터베이스를 통한 안전한 데이터 보관
- 🔌 **PlaceholderAPI 연동**: 채팅, TAB, 스코어보드 등에서 칭호 표시
- 🗑️ **영구 삭제**: Shift + 우클릭으로 원하지 않는 칭호 삭제
- 🔄 **실시간 리로드**: 서버 재시작 없이 설정 변경 가능

### 📦 요구사항

| 항목 | 버전 |
|------|------|
| Minecraft | Purpur 1.21.8+ |
| Java | 21+ |
| 필수 플러그인 | Core (도토리마을 코어) |
| 선택 플러그인 | PlaceholderAPI |
| 데이터베이스 | MySQL 8.0+ |
| 캐시 | Redis 6.0+ |

### 🔧 설치 방법

1. **플러그인 다운로드**
   ```bash
   # releases 페이지에서 최신 버전 다운로드
   https://github.com/yourusername/DotorimaruTitle/releases
   ```

2. **파일 배치**
   ```
   plugins/
   ├── Core-1.1.5.jar          # 필수
   ├── Title-1.0.0.jar         # 본 플러그인
   └── PlaceholderAPI.jar      # 선택
   ```

3. **서버 시작**
   ```bash
   # 첫 실행 시 config.yml 자동 생성
   ```

4. **설정 편집**
   ```yaml
   # plugins/Title/config.yml 수정
   # MySQL 및 Redis 설정은 Core 플러그인에서 관리
   ```

5. **서버 재시작**

### 📖 사용법

#### 플레이어

1. **칭호북 획득**
   - 관리자로부터 칭호북을 받거나 상점에서 구매

2. **칭호북 사용**
   - 칭호북을 손에 들고 **우클릭**
   - 칭호가 자동으로 추가되고 칭호북 소모

3. **칭호 관리**
   - `/칭호` 명령어 실행
   - GUI에서 칭호 클릭:
     - **좌클릭**: 착용/해제
     - **Shift + 우클릭**: 영구 삭제

#### 관리자

1. **칭호북 생성**
   ```
   /칭호북 &c&l전설의 용사
   /칭호북 #FF5733용맹한 전사
   ```

2. **설정 리로드**
   ```
   /칭호관리 리로드
   ```

### 📝 명령어

| 명령어 | 설명 | 권한 |
|--------|------|------|
| `/칭호` | 칭호 GUI 열기 | 없음 |
| `/칭호북 <칭호이름>` | 칭호북 생성 | `title.admin` |
| `/칭호관리 리로드` | 설정 리로드 | `title.admin` |

**별칭:**
- `/칭호관리` = `/titleadmin`, `/타이틀관리`

### 🔐 권한

| 권한 | 설명 | 기본값 |
|------|------|--------|
| `title.admin` | 관리자 명령어 사용 | OP |

### 🎯 PlaceholderAPI

| 플레이스홀더 | 설명 | 예시 출력 |
|-------------|------|-----------|
| `%titlesystem_title%` | 착용 중인 칭호 (색상 적용) | `[전설의 용사] ` |
| `%titlesystem_title_raw%` | 착용 중인 칭호 (색상 코드) | `&c&l전설의 용사` |
| `%titlesystem_title_count%` | 보유 칭호 개수 | `5` |

#### 사용 예시

**RedisChat 설정:**
```yaml
formats:
  - permission: chat.default
    format: "%titlesystem_title%<white>[<gray>Lv.1<white>] <{player}> <white>{message}"
```

**채팅 결과:**
```
[전설의 용사] [Lv.1] RG_topkide > 안녕하세요
```

**TAB 플러그인:**
```yaml
tabname-prefix: "%titlesystem_title%"
```

### ⚙️ 설정 파일

<details>
<summary>config.yml (클릭하여 펼치기)</summary>

```yaml
# ===================================
# DotorimaruTitle Configuration
# Minecraft Version: 1.21.8
# ===================================

# GUI 설정
gui:
  title: "&6&l내 칭호 목록 &7({current}/{max})"
  size: 54
  
  # 선택된 칭호 아이템
  selected-item:
    material: PAPER
    display-name: "&a&l{title}"
    enchant-glow: true
    lore:
      - ""
      - "&7상태: &a&l착용 중"
      - ""
      - "&e좌클릭 &7- 해제"
      - "&e&oShift + 우클릭 &7- &c영구 삭제"
  
  # 선택되지 않은 칭호 아이템
  unselected-item:
    material: PAPER
    display-name: "&f{title}"
    enchant-glow: false
    lore:
      - ""
      - "&7상태: &c미착용"
      - ""
      - "&e좌클릭 &7- 착용"
      - "&e&oShift + 우클릭 &7- &c영구 삭제"

# 칭호북 설정
title-book:
  material: BOOK
  custom-model-data: 0
  display-name: "&6&l칭호북"
  lore:
    - ""
    - "&7획득 칭호: &f{title}"
    - ""
    - "&e우클릭하여 칭호 획득!"

# 메시지
messages:
  prefix: "&8[&6칭호&8]"
  title-obtained: "%prefix% &a칭호를 획득했습니다: {title}"
  already-owned: "%prefix% &c이미 보유한 칭호입니다."
  inventory-full: "%prefix% &c칭호 슬롯이 가득 찼습니다! (최대 54개)"
  title-equipped: "%prefix% &a칭호를 착용했습니다: {title}"
  title-unequipped: "%prefix% &7칭호를 해제했습니다."
  title-deleted: "%prefix% &c칭호를 삭제했습니다: {title}"
  no-permission: "%prefix% &c권한이 없습니다."
```

</details>

### 🗄️ 데이터베이스 구조

**player_titles** (보유 칭호)
```sql
CREATE TABLE player_titles (
    uuid VARCHAR(36),
    title_name TEXT,
    obtained_at BIGINT,
    PRIMARY KEY (uuid, title_name(255))
);
```

**selected_titles** (선택된 칭호)
```sql
CREATE TABLE selected_titles (
    uuid VARCHAR(36) PRIMARY KEY,
    title_name TEXT,
    updated_at BIGINT
);
```

### 🔄 Redis 캐시 구조

- `title:titles:{uuid}` - 보유 칭호 목록 (TTL: 600초)
- `title:selected:{uuid}` - 선택된 칭호 (TTL: 600초)

### 📡 Redis Pub/Sub 메시지

- `title-add:{uuid}:{titleName}` - 칭호 추가
- `title-remove:{uuid}:{titleName}` - 칭호 삭제
- `title-select:{uuid}:{titleName}` - 칭호 선택

### 🏗️ 프로젝트 구조

```
DotorimaruTitle/
├── src/main/java/com/dotorimaru/title/
│   ├── TitlePlugin.java              # 메인 클래스
│   ├── commands/                     # 명령어
│   │   ├── TitleCommand.java
│   │   ├── TitleBookCommand.java
│   │   └── TitleAdminCommand.java
│   ├── listeners/                    # 이벤트 리스너
│   │   ├── PlayerJoinListener.java
│   │   ├── TitleBookUseListener.java
│   │   └── TitleGUIListener.java
│   ├── managers/                     # 비즈니스 로직
│   │   ├── TitleManager.java
│   │   └── TitleBookManager.java
│   ├── database/                     # 데이터베이스
│   │   ├── TitleMySQLManager.java
│   │   ├── TitleRedisManager.java
│   │   └── TitleStorage.java
│   ├── gui/                          # GUI
│   │   └── TitleGUI.java
│   ├── placeholders/                 # PlaceholderAPI
│   │   └── TitlePlaceholder.java
│   └── models/                       # 데이터 모델
│       └── Title.java
└── src/main/resources/
    ├── plugin.yml
    └── config.yml
```

### 🛠️ 빌드 방법

```bash
# Clone repository
git clone https://github.com/yourusername/DotorimaruTitle.git
cd DotorimaruTitle

# Build with Gradle
./gradlew clean shadowJar

# Output
build/libs/Title-1.0.0.jar
```

### 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

### 👤 개발자

**명노준 (Myung Nojun)**

- GitHub: [@yourusername](https://github.com/yourusername)
- 서버: 도토리마을 (Dotorimaru)

### 📞 지원

- 이슈 트래커: [GitHub Issues](https://github.com/yourusername/DotorimaruTitle/issues)
- 디스코드: [도토리마을 Discord](https://discord.gg/yourserver)

### 🙏 감사의 말

- [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) - 플레이스홀더 시스템
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - 고성능 JDBC 연결 풀
- [Jedis](https://github.com/redis/jedis) - Redis Java 클라이언트
- [Lombok](https://projectlombok.org/) - 보일러플레이트 코드 감소

---

## English

### 📋 Introduction

DotorimaruTitle is a professional title system plugin for Minecraft servers. It supports title acquisition through title books, GUI-based management, PlaceholderAPI integration, and multi-server synchronization via Redis.

### ✨ Key Features

- 🎫 **Title Book System**: Easy title acquisition via right-click
- 🎨 **RGB Color Support**: Both Legacy color codes (`&`) and RGB colors (`#RRGGBB`)
- 📦 **GUI Inventory**: Intuitive 54-slot title management UI
- ⚡ **Multi-Server Sync**: Real-time synchronization via Redis Pub/Sub
- 💾 **Persistent Storage**: Safe data storage through MySQL database
- 🔌 **PlaceholderAPI Integration**: Display titles in chat, TAB, scoreboard, etc.
- 🗑️ **Permanent Deletion**: Remove unwanted titles with Shift + Right-click
- 🔄 **Live Reload**: Change configuration without server restart

### 📦 Requirements

| Item | Version |
|------|---------|
| Minecraft | Purpur 1.21.8+ |
| Java | 21+ |
| Required Plugin | Core (Dotorimaru Core) |
| Optional Plugin | PlaceholderAPI |
| Database | MySQL 8.0+ |
| Cache | Redis 6.0+ |

### 🔧 Installation

1. **Download Plugin**
   ```bash
   # Download latest version from releases page
   https://github.com/yourusername/DotorimaruTitle/releases
   ```

2. **Place Files**
   ```
   plugins/
   ├── Core-1.1.5.jar          # Required
   ├── Title-1.0.0.jar         # This plugin
   └── PlaceholderAPI.jar      # Optional
   ```

3. **Start Server**
   ```bash
   # config.yml will be automatically generated on first run
   ```

4. **Edit Configuration**
   ```yaml
   # Edit plugins/Title/config.yml
   # MySQL and Redis settings are managed by Core plugin
   ```

5. **Restart Server**

### 📖 Usage

#### For Players

1. **Obtain Title Book**
   - Receive from administrator or purchase from shop

2. **Use Title Book**
   - Hold title book and **right-click**
   - Title is automatically added and book is consumed

3. **Manage Titles**
   - Execute `/칭호` command
   - Click title in GUI:
     - **Left-click**: Equip/Unequip
     - **Shift + Right-click**: Permanent deletion

#### For Administrators

1. **Create Title Book**
   ```
   /칭호북 &c&lLegendary Warrior
   /칭호북 #FF5733Brave Fighter
   ```

2. **Reload Configuration**
   ```
   /칭호관리 리로드
   ```

### 📝 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/칭호` | Open title GUI | None |
| `/칭호북 <title>` | Create title book | `title.admin` |
| `/칭호관리 리로드` | Reload configuration | `title.admin` |

**Aliases:**
- `/칭호관리` = `/titleadmin`, `/타이틀관리`

### 🔐 Permissions

| Permission | Description | Default |
|-----------|-------------|---------|
| `title.admin` | Use admin commands | OP |

### 🎯 PlaceholderAPI

| Placeholder | Description | Example Output |
|------------|-------------|----------------|
| `%titlesystem_title%` | Equipped title (colored) | `[Legendary Warrior] ` |
| `%titlesystem_title_raw%` | Equipped title (color codes) | `&c&lLegendary Warrior` |
| `%titlesystem_title_count%` | Number of owned titles | `5` |

#### Usage Examples

**RedisChat Configuration:**
```yaml
formats:
  - permission: chat.default
    format: "%titlesystem_title%<white>[<gray>Lv.1<white>] <{player}> <white>{message}"
```

**Chat Result:**
```
[Legendary Warrior] [Lv.1] RG_topkide > Hello
```

**TAB Plugin:**
```yaml
tabname-prefix: "%titlesystem_title%"
```

### 🛠️ Building

```bash
# Clone repository
git clone https://github.com/yourusername/DotorimaruTitle.git
cd DotorimaruTitle

# Build with Gradle
./gradlew clean shadowJar

# Output
build/libs/Title-1.0.0.jar
```

### 🤝 Contributing

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### 📄 License

This project is distributed under the MIT License. See [LICENSE](LICENSE) file for details.

### 👤 Author

**Myung Nojun**

- GitHub: [@yourusername](https://github.com/yourusername)
- Server: Dotorimaru

### 📞 Support

- Issue Tracker: [GitHub Issues](https://github.com/yourusername/DotorimaruTitle/issues)
- Discord: [Dotorimaru Discord](https://discord.gg/yourserver)

### 🙏 Acknowledgments

- [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) - Placeholder system
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - High-performance JDBC connection pool
- [Jedis](https://github.com/redis/jedis) - Redis Java client
- [Lombok](https://projectlombok.org/) - Boilerplate code reduction

---

<div align="center">

**⭐ Star this repository if you find it useful! ⭐**

Made with ❤️ by 명노준

</div>
