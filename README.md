
## Simple Expense Tracker App
 Aplikasi Pencatatan pengeluaran harian sederhana

## Fitur
- Tambah kategori pengeluaran (Makan, Transport, Belanja, dll.)
- Tambah pengeluaran: catatan, nominal (Rupiah), kategori, tanggal otomatis
- Edit pengeluaran: ketuk salah satu baris daftar
- Hapus pengeluaran: ikon tempat sampah di kanan baris (atau tekan lama)
- Total pengeluaran dihitung otomatis
- Data tersimpan permanen di SQLite melalui Room

## Skema Database
Database: `pengeluaran.db` 

| Tabel | Kolom |
|---|---|
| `categories` | `id` (PK, autoGenerate), `name` |
| `expenses` | `id` (PK, autoGenerate), `note`, `amount`, `date`, `categoryId` (FK → `categories.id`, onDelete CASCADE) |

Relasi satu kategori ke banyak pengeluaran (`@Relation` pada `CategoryWithExpenses`).

## Teknologi
- Kotlin
- Room 2.8.5 (`room-runtime`, `room-ktx`, `room-compiler`) dengan KSP 2.3.13
- Material 3
- minSdk 31, targetSdk 37

## Struktur Kode

```
app/src/main/java/com/example/simpleexpensetracker/
├── MainActivity.kt
└── data/
    ├── Category.kt
    ├── Expense.kt
    ├── CategoryWithExpenses.kt
    ├── ExpenseDao.kt
    └── AppDatabase.kt
```