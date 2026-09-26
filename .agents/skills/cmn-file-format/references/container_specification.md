# `.cmn` Container Binary Specification

This document defines the container layout, magic byte signature, and MIME type handling for the `.cmn` (Custom Multi-layer Note) package format.

## 1. Magic Bytes and File Signature

Generic ZIP archive managers (e.g. Windows Explorer, macOS Archive Utility) should not open or extract `.cmn` files by default upon double-click, protecting proprietary compound document integrity.

### Header Magic Bytes
- **Offset 0..3 (4 bytes)**: `0x43 0x4D 0x4E 0x01` (`CMN\x01`)
- **Offset 4..7 (4 bytes)**: Big-endian integer representing container format version (e.g. `0x00 0x00 0x00 0x01` for Version 1).
- **Offset 8..N**: Standard ZIP archive payload starting with local file header `0x50 0x4B 0x03 0x04` (`PK\x03\x04`).

When opening a `.cmn` file:
1. Verify the first 4 bytes match `0x43 0x4D 0x4E 0x01`. If missing, reject as an invalid or corrupted file.
2. Read the 4-byte version integer.
3. Slice the input stream from offset 8 onwards and pass to standard ZIP stream readers (`java.util.zip.ZipInputStream` or `okio.zipfilesystem`).

When saving a `.cmn` file:
1. Write 8 header bytes: `CMN\x01` followed by 32-bit version `1`.
2. Stream the generated ZIP payload directly into the output stream.

---

## 2. MIME Type Specification

- **MIME Type String**: `application/x-notes-cmn`
- **File Extension**: `.cmn`
- **Uniform Type Identifier (iOS / macOS)**: `com.notesalltogether.cmn`

To conform with container best practices (similar to ODF and EPUB), the first file entry in the internal ZIP archive is named `mimetype`:
- Compression method: **Stored** (`STORED` / 0% compression, raw uncompressed ASCII bytes).
- Extra fields: None.
- Content: `application/x-notes-cmn`.

---

## 3. Directory Layout

```text
/
├── mimetype                   # Plaintext uncompressed MIME type
├── manifest.json              # Document metadata, page setup, and layer index
├── layers/
│   ├── layer_0_bg.json        # Layer-specific stroke & shape definitions
│   ├── layer_1_ink.json
│   └── layer_2_ink.json
├── assets/
│   ├── img_001.png            # High-resolution raster images embedded in canvas
│   └── img_002.jpeg
└── previews/
    ├── thumbnail.png          # Fast UI gallery preview (300x400)
    └── full_page_1.png        # Optional cached first-page render
```
