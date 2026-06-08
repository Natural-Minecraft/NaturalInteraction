# Changelog - NaturalInteraction 🤝

Dokumentasi riwayat pembaruan, perbaikan bug, dan rilis fitur untuk plugin **NaturalInteraction** (NPC & Quest Interaction Plugin).

---

## [v1.3.0] - Quest & Cinematic Update
### ✨ Fitur Baru
- **Dynamic Per-Player Tutorial**: Logika tutorial interaktif yang bersifat personal per-pemain menggunakan integrasi `FancyHolograms` dan `ActionExecutor`.
- **Genshin Impact-style Quest**: Modul quest petualangan sinematik baru lengkap dengan BossBar petunjuk arah dan visualisasi jalur (smart pathfinding).
- **Cinematic NPC Walking**: Pergerakan NPC yang berjalan mulus (cinematic walking) serta logika klaim wilayah (claim land) selama masa tutorial.
- **Catmull-Rom Splines**: Penggunaan matematika spline interpolasi Catmull-Rom untuk membuat rute pergerakan NPC and kamera sinematik menjadi sangat halus melengkung.
- **Offline Tab Completion**: Dukungan pelengkapan command otomatis (tab completion) bagi nama-nama pemain secara offline.

### 🐛 Perbaikan Bug
- **ProtocolLib Exception Fix**: Menyelesaikan error pengecualian ProtocolLib yang memicu pemutusan koneksi player secara tiba-tiba.
- **NPC Hologram Removal**: Menghapus sisa hologram NPC yang menggantung setelah tutorial selesai atau NPC dihapus.
- **Camera Offset Fix**: Mengoreksi pergeseran sumbu kamera (camera offset) sebesar 1.62 Y pada Marker ArmorStands agar pas sejajar dengan tinggi mata (eye height) player asli.
