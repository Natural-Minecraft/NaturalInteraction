# 📖 Panduan Lengkap: Membuat Story/Dialogue

Tutorial ini akan membimbing kamu membuat 1 interactive story lengkap dari awal.

---

## 📋 Persiapan

**Yang dibutuhkan:**
- Citizens2 (untuk NPC)
- NaturalInteraction (plugin ini)

---

## Step 1: Generate Template ⚡

```
/ni quickstart fishing_quest QUEST
```

Ini akan membuat file:
`plugins/NaturalInteraction/interactions/fishing_quest.json`

**Template yang tersedia:**
| Template | Fungsi |
|:---|:---|
| QUEST | NPC quest dengan pilihan accept/decline |
| SHOP | Shopkeeper dengan browse/sell |
| LORE | Story linear multi-bagian |
| TUTORIAL | Panduan pemain baru |

---

## Step 2: Edit JSON File ✏️

Buka `fishing_quest.json`:

```json
{
  "id": "fishing_quest",
  "rootNodeId": "start",
  "nodes": [
    {
      "id": "start",
      "text": "&6[Nelayan] &fHalo! Bisa bantuin aku?",
      "durationSeconds": 5,
      "options": [
        { "text": "&eTentu!", "targetNodeId": "details" },
        { "text": "&cNggak deh.", "targetNodeId": "exit" }
      ]
    },
    {
      "id": "details",
      "text": "&6[Nelayan] &fCarikan 5 &bIkan Salmon &funtukku.",
      "options": [
        { "text": "&aSiap!", "targetNodeId": "accept" }
      ]
    },
    {
      "id": "accept",
      "text": "&6[Nelayan] &fBagus! Kembali kalau sudah dapat.",
      "actions": [
        { "type": "COMMAND", "value": "quest add fishing_task %player%" }
      ]
    }
  ]
}
```

---

## Step 3: Buat NPC 👤

```
/npc create Nelayan
/npc select
/npc skin <username>
/trait interaction fishing_quest
```

Sekarang NPC akan menampilkan dialogue saat diklik!

---

## Step 4: Test! 🎮

```
/ni reload
```

Klik NPC → Dialogue muncul!

---

## 📝 Struktur Node

| Property | Fungsi |
|:---|:---|
| `id` | ID unik node |
| `text` | Teks yang ditampilkan |
| `durationSeconds` | Durasi tampil (opsional) |
| `options` | Pilihan player (array) |
| `actions` | Aksi otomatis (array) |
| `nextNodeId` | Node berikutnya (untuk linear) |

---

## ⚡ Action Types

| Type | Contoh | Fungsi |
|:---|:---|:---|
| `COMMAND` | `give %player% diamond 1` | Jalankan command |
| `MESSAGE` | `Terima kasih!` | Kirim pesan |
| `SOUND` | `ENTITY_VILLAGER_YES` | Mainkan suara |
| `TELEPORT` | `world,100,64,100` | Teleport player |

---

## ✅ Checklist

- [ ] Template di-generate
- [ ] JSON diedit sesuai kebutuhan
- [ ] NPC dibuat dengan Citizens
- [ ] Trait interaction ditambahkan
- [ ] Test klik NPC

---

**Selamat!** Story kamu sudah bisa dimainkan! 🎉
