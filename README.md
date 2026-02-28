# NaturalInteraction v1.3.0 (Cinematic ActionBar Update)

The "Premium Story Core" for **NaturalSMP**. Merupakan plugin yang bertanggung jawab untuk mengelola sistem interaksi cerita, quest dinamis, dan NPC interaktif dengan pengalaman cinematic yang imersif.

## ✨ Fitur Baru v1.3.0 (Cinematic ActionBar Update)
*   **ActionBar Dialogue System**: Dialog NPC kini muncul di ActionBar dengan efek typewriter (kata per kata) — tidak menghalangi pandangan pemain.
*   **ItemsAdder Unicode Background**: Dukungan karakter unicode dari ItemsAdder sebagai latar visual dialog (Terraria-style dialogue box). Konfigurasi `dialogueUnicode` per-interaction.
*   **Cinematic Focus Lock**: Pemain otomatis terkunci (tidak bisa bergerak) selama dialog berlangsung, namun tetap bisa memutar kepala.
*   **Sneak to Skip**: Tekan Shift untuk skip dialog. Skip pertama = langsung tampilkan semua teks. Skip kedua = lanjut ke node berikutnya.
*   **Enhanced TextDisplay**: Opsi jawaban di atas NPC kini memiliki background semi-transparan gelap agar mudah dibaca.
*   **Cinematic BossBar**: BossBar timer kini menampilkan nama NPC dengan style yang lebih menarik.

## ✨ Fitur v1.2.1 (Interactive & Rewards Update)
*   **Interactive Tagging**: Pemain dapat menandai (tag) pemain lain dengan Sneak + Right Click untuk interaksi sosial cepat.
*   **Interaction Rewards**: Sistem hadiah otomatis saat pemain mencapai milestone interaksi tertentu.

## ✨ Fitur Utama

### 🎬 Cinematic Dialogue System
*   **Smart Title Splitting**: Dialog panjang otomatis dipecah (5 kata pertama di Title, sisanya di Subtitle) agar nyaman dibaca dan tidak memenuhi layar.
*   **Conversational Distance**: Player otomatis di-teleport mundur (~2.5 blok) saat interaksi dimulai agar mendapatkan sudut pandang kamera yang sinematik.
*   **Typewriter Effect**: Pesan muncul dengan efek suara dan visual bertahap di chat dan HUD.
*   **Dynamic Zoom**: Efek zoom otomatis (FOVAS) saat berbicara dengan NPC penting.

### 🎭 NPC & Story Management
*   **Sub-Command Framework**: Sistem perintah `/ni` yang modular dengan dukungan Tab Completion penuh untuk mempercepat development.
*   **Interactive NPC Trait**: Integrasi Citizen2 yang memungkinkan NPC apa pun menjadi pemicu cerita (Interaction Trait).
*   **Visual Choice Display**: Opsi dialog muncul secara visual di atas kepala NPC menggunakan TextDisplay (v1.19.4+).

### 🎣 Dynamic NPC Spawning
*   **Content-Aware Spawning**: Command `/ni spawnfish` secara cerdas mendeteksi pinggiran air (shoreline) terdekat untuk menempatkan NPC pancingan dengan dekorasi barrel dan lantern otomatis.
*   **Shoreline Detection**: Algoritma canggih untuk memastikan NPC tidak pernah spawn di dalam air atau di area berbahaya.

## 📑 Commands & Permissions

| Command | Aliases | Description | Permission |
| :--- | :--- | :--- | :--- |
| `/ni` | - | Menu Utama Admin / Interaction Editor | `naturalinteraction.admin` |
| `/ni start <id>` | - | Mulai interaksi cerdas (Auto-ID detection) | `naturalinteraction.admin` |
| `/ni reload` | - | **Deep Reload** all dialogs & managers | `naturalinteraction.admin` |
| `/ni spawnfish` | - | Spawn NPC Kakek Tua secara dinamis | `naturalinteraction.admin` |
| `/ni wand` | - | Ambil Story Creator Wand | `naturalinteraction.admin` |
| `/ni edit <id>` | - | Edit interaksi yang sudah ada | `naturalinteraction.admin` |
| `/ni delete <id>` | - | Hapus interaksi cerdas | `naturalinteraction.admin` |

## ⚙️ Development & Actions
Setiap dialog mendukung serangkaian **Actions** otomatis:
*   `SOUND`: Putar suara dengan pitch & volume kustom.
*   `ITEM`: Memberi item dengan metadata (termasuk `FILLED_MAP` yang otomatis ter-initialize).
*   `ZOOM`: Efek cinematic zoom.
*   `COMMAND`: Eksekusi perintah konsol.
*   `TELEPORT`: Pindahkan pemain ke koordinat tertentu.

---
**© 2026 NaturalSMP Development Team**
